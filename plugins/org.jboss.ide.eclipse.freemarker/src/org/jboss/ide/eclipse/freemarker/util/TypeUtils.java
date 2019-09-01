/*
 * JBoss by Red Hat
 * Copyright 2006-2009, Red Hat Middleware, LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ide.eclipse.freemarker.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.jboss.ide.eclipse.freemarker.Plugin;

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
	
	/**
	 * Extracts the JavaBeans property from a reader method name, or returns {@code null} if the method name doesn't
	 * look like a reader method name. 
	 */
	public static String getBeanPropertyNameFromReaderMethodName(String name, Class<?> returnType) {
	    int start;
	    if (name.startsWith("get")) { //$NON-NLS-1$
	        start = 3;
	    } else if (returnType == boolean.class && name.startsWith("is")) { //$NON-NLS-1$
	        start = 2;
	    } else {
	        return null;
	    }
	    int ln = name.length();
	    
	    if (start == ln) {
	        return null;
	    }
	    char c1 = name.charAt(start);
	    
	    return start + 1 < ln && Character.isUpperCase(name.charAt(start + 1)) && Character.isUpperCase(c1)
	            ? name.substring(start) // getFOOBar => "FOOBar" (not lower case) according the JavaBeans spec.
	            : new StringBuilder(ln - start).append(Character.toLowerCase(c1)).append(name, start + 1, ln)
	                    .toString();
	}

	/**
	 * Returns the bean members for a class, trying to follow more or less what FreeMarker by default
	 * exposes for a generic object.  
	 */
	public static BeanMembers getBeanMembers(
			Class<?> beanClass, boolean getProperties, boolean getMethods, boolean filterOutReadMethods,
			BiPredicate<String, Method> filter) {
		final List<BeanMember> properties = new ArrayList<>();
		final List<BeanMember> methods = new ArrayList<>();
		try {
			BeanInfo bi = Introspector.getBeanInfo(beanClass);
			
			Set<Method> propertyReadMethods = new HashSet<Method>();
			for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
				String propertyName = pd.getName();
				Method readMethod = pd.getReadMethod();
				if (readMethod != null) {
					if (filterOutReadMethods) {
						propertyReadMethods.add(readMethod);
					}
					if (!readMethod.isBridge() && filter.test(propertyName, readMethod)) {
						properties.add(new BeanMember(propertyName, pd.getPropertyType(), readMethod));
					}
				}
			}
			// At least in Java 8 (and 9?) Introspector did not return properties based on default read methods, but FreeMarker can.
			for (Method readMethod : beanClass.getMethods()) {
				if (readMethod.isDefault()
						&& readMethod.getReturnType() != void.class
						&& readMethod.getParameterTypes().length == 0) {
					String propertyName = getBeanPropertyNameFromReaderMethodName(
							readMethod.getName(), readMethod.getReturnType());
					if (propertyName != null) {
						if (filterOutReadMethods) {
							propertyReadMethods.add(readMethod);
						}
						if (!readMethod.isBridge() && filter.test(propertyName, readMethod)) {
							properties.add(new BeanMember(propertyName, readMethod.getReturnType(), readMethod));
						}
					}
				}
			}
			
			for (MethodDescriptor methodDescriptor : bi.getMethodDescriptors()) {
				String name = methodDescriptor.getName();
				Method method = methodDescriptor.getMethod();
				if (!method.isBridge()
						&& filter.test(name, method)
						&& !propertyReadMethods.contains(method)) {
					methods.add(new BeanMember(name, method.getReturnType(), method));
				}
			}
			// At least in Java 8 (and 9?) Introspector did not return default methods, but FreeMarker can.
			for (Method method : beanClass.getMethods()) {
				if (method.isDefault()
						&& !method.isBridge()
						&& filter.test(method.getName(), method)
						&& !propertyReadMethods.contains(method)) {
					methods.add(new BeanMember(method.getName(), method.getReturnType(), method));
				}
			}
		} catch (IntrospectionException e) {
			Plugin.log(e);
		}
		return new BeanMembers(properties, methods);
	}
	
	/**
	 * For filtering bean members that we typically don't want to show in completion proposals.
	 * 
	 * @param The name of the Bean property, or the name of the method
	 * @param The read method for a property, or just the method otherwise
	 */
	public static boolean filterForCompletionProposal(String name, Method method) {
		return !TypeUtils.isObjectMethod(method) && !method.isSynthetic();
	}
	
	public static class BeanMembers {
		final List<BeanMember> properties;
		final List<BeanMember> methods;
		
		public BeanMembers(List<BeanMember> properties, List<BeanMember> methods) {
			this.properties = properties;
			this.methods = methods;
		}

		public List<BeanMember> getProperties() {
			return properties;
		}

		public List<BeanMember> getMethods() {
			return methods;
		}
	}

	/**
	 * Tells if a public method was originally declared in {@link Object}, even if it was overridden.
	 */
	public static boolean isObjectMethod(Method m) {
		Class<?> declaringClass = m.getDeclaringClass();
		while (declaringClass != null) {
			if (declaringClass == Object.class) {
				return true;
			}
			
			Class<?> superClass = declaringClass.getSuperclass();
			if (superClass != null) {
				try {
					Method newM = superClass.getMethod(m.getName(), m.getParameterTypes());
					declaringClass = newM.getDeclaringClass();
				} catch (NoSuchMethodException | SecurityException e) {
					// Give up
					declaringClass = null;
				}
			} else {
				declaringClass = null;
			}
		}
		return false;
	}
	
}
