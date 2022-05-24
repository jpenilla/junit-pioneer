/*
 * Copyright 2016-2022 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junitpioneer.jupiter.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.junitpioneer.jupiter.cartesian.CartesianParameterArgumentsProvider;

/**
 * Provides arguments from JSON files specified with {@link JsonFileSource}.
 */
abstract class AbstractJsonArgumentsProvider<A extends Annotation>
		implements ArgumentsProvider, AnnotationConsumer<A>, CartesianParameterArgumentsProvider<Object> {

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		Method method = context.getRequiredTestMethod();
		return provideNodes(context).map(node -> createArguments(method, node));
	}

	@Override
	public Stream<Object> provideArguments(ExtensionContext context, Parameter parameter) throws Exception {
		return provideNodes(context).map(node -> createArgumentForCartesianProvider(parameter, node));
	}

	private Stream<Node> provideNodes(ExtensionContext context) {
		return provideNodes(context, JsonConverterProvider.getJsonConverter());
	}

	protected abstract Stream<Node> provideNodes(ExtensionContext context, JsonConverter jsonConverter);

	private static Object createArgumentForCartesianProvider(Parameter parameter, Node node) {
		Property property = parameter.getAnnotation(Property.class);
		if (property == null) {
			return node.toType(parameter.getType());
		} else {
			return node.getNode(property.value()).map(value -> value.value(parameter.getType())).orElse(null);
		}
	}

	private static Arguments createArguments(Method method, Node node) {
		boolean singleParameter = method.getParameterCount() == 1;
		if (singleParameter) {
			Parameter onlyParameter = method.getParameters()[0];
			// When there is a single parameter, the user might want to extract a single value or an entire type.
			// When the parameter has the `@Property` annotation, then a single value needs to be extracted.
			Property property = onlyParameter.getAnnotation(Property.class);
			if (property == null) {
				// no property specified -> the node should be converted in the parameter type
				return Arguments.arguments(node.toType(onlyParameter.getType()));
			}

			// otherwise, treat this as method arguments
			return createArgumentsForMethod(method, node);
		}
		return createArgumentsForMethod(method, node);
	}

	private static Arguments createArgumentsForMethod(Method method, Node node) {
		// @formatter:off
		Object[] arguments = Arrays.stream(method.getParameters())
				.map(parameter -> {
					Property property = parameter.getAnnotation(Property.class);
					String name = property == null
							? parameter.getName()
							: property.value();
					return node
							.getNode(name)
							.map(value -> value.value(parameter.getType()))
							.orElse(null);
				})
				.toArray();
		// @formatter:on
		return Arguments.of(arguments);
	}

}