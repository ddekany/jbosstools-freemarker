package org.jboss.ide.eclipse.freemarker.util;

public final class TypeUtils {

	private TypeUtils() {
		throw new AssertionError();
	}

	public static Class<?> toNonPrimitiveType(Class<?> cl) {
		if (cl.isPrimitive()) {
			if (cl == int.class) {
				return Integer.class;
			}
			if (cl == long.class) {
				return Long.class;
			}
			if (cl == byte.class) {
				return Byte.class;
			}
			if (cl == short.class) {
				return Short.class;
			}
			if (cl == double.class) {
				return Double.class;
			}
			if (cl == float.class) {
				return Float.class;
			}
			if (cl == boolean.class) {
				return Boolean.class;
			}
			if (cl == char.class) {
				return Character.class;
			}
		}
		return cl;
	}

}
