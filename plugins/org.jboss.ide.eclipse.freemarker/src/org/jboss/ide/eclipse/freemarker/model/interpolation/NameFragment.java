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
package org.jboss.ide.eclipse.freemarker.model.interpolation;


import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.jboss.ide.eclipse.freemarker.Plugin;
import org.jboss.ide.eclipse.freemarker.configuration.ConfigurationManager;
import org.jboss.ide.eclipse.freemarker.configuration.ContextValue;
import org.jboss.ide.eclipse.freemarker.lang.LexicalConstants;

/**
 * @author <a href="mailto:joe@binamics.com">Joe Hudson</a>
 */
public class NameFragment extends AbstractFragment {

	public NameFragment(int offset, String content) {
		super(offset, content);
	}

	@Override
	public ICompletionProposal[] getCompletionProposals (int subOffset, int offset, Class<?> parentClass,
			List<Fragment> fragments, ISourceViewer sourceViewer, Map<String, Class<?>> context, IResource file, IProject project) {
		if (isStartFragment()) {
			// pull from context
			String prefix = getContent().substring(0, subOffset);
			List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
			for (Iterator<String> i=context.keySet().iterator(); i.hasNext(); ) {
				String key = i.next();
				if (key.startsWith(prefix)) proposals.add(getCompletionProposal(
						offset, subOffset, key, getContent()));
			}
			return completionProposals(proposals, true);
		}
		else {
			if (null == parentClass) return null;
			return getMemberCompletionProposals (subOffset, offset, parentClass, file);
		}
	}

	private Class<?> returnClass;
	@Override
	public Class<?> getReturnClass (Class<?> parentClass, List<Fragment> fragments, Map<String, Class<?>> context, IResource resource, IProject project){
		if (null == returnClass) {
			String content = getContent();
			if (isStartFragment()) {
				returnClass = context.get(content);
			}
			else {
				if (null == parentClass) {
					returnClass = Object.class;
				}
				else {
					content = Character.toUpperCase(content.charAt(1)) + content.substring(2, getContent().length());
					String getcontent = "get" + content; //$NON-NLS-1$
					for (int i=0; i<parentClass.getMethods().length; i++) {
						Method m = parentClass.getMethods()[i];
						if (m.getName().equals(content) || m.getName().equals(getcontent)) {
							returnClass = m.getReturnType();
							break;
						}
					}
				}
			}
		}
		return returnClass;
	}

	private Class<?> singulaReturnClass;
	@Override
	public Class<?> getSingularReturnClass(Class<?> parentClass, List<Fragment> fragments, Map<String, Class<?>> context, IResource resource, IProject project) {
		if (null == singulaReturnClass) {
			String content = getContent();
			if (isStartFragment()) {
				ContextValue contextValue = ConfigurationManager.getInstance(project).getContextValue(content, resource, true);
				if (null == contextValue || null == contextValue.singularClass)
					singulaReturnClass = Object.class;
				else
					singulaReturnClass = contextValue.singularClass;
			}
			else {
				if (null == parentClass) {
					singulaReturnClass = Object.class;
				}
				else {
					content = Character.toUpperCase(content.charAt(1)) + content.substring(2, getContent().length());
					String getcontent = "get" + content; //$NON-NLS-1$
					for (int i=0; i<parentClass.getMethods().length; i++) {
						Method m = parentClass.getMethods()[i];
						if (m.getName().equals(content) || m.getName().equals(getcontent)) {
							Type type = m.getGenericReturnType();
							if (type instanceof ParameterizedType) {
								ParameterizedType pType = (ParameterizedType) type;
								if (pType.getActualTypeArguments().length > 0) {
									singulaReturnClass = (Class<?>) pType.getActualTypeArguments()[0];
									break;
								}
							}
							singulaReturnClass = Object.class;
							break;
						}
					}
				}
			}
		}
		return singulaReturnClass;
	}

	public boolean isStartFragment () {
		return !getContent().startsWith("."); //$NON-NLS-1$
	}

	public ICompletionProposal[] getMemberCompletionProposals (int subOffset, int offset, Class<?> parentClass, IResource file) {
		if (instanceOf(parentClass, String.class)
				|| instanceOf(parentClass, Number.class)
				|| instanceOf(parentClass, Date.class)
				|| instanceOf(parentClass, Boolean.class)
				|| instanceOf(parentClass, Map.class)
				|| instanceOf(parentClass, Collection.class)
				|| parentClass.isArray()
				|| instanceOf(parentClass, Iterator.class)
				|| instanceOf(parentClass, Enumeration.class)
				|| instanceOf(parentClass, ResourceBundle.class))
			return null;
		String prefix = getContent().substring(1, subOffset);
		String pUpper = prefix.toUpperCase();
		try {
			BeanInfo bi = Introspector.getBeanInfo(parentClass);
			
			List<ICompletionProposal> propertyProposals = new ArrayList<>();
			Set<Method> propertyReadMethods = new HashSet<Method>();
			Map<String, Method> properties = new HashMap<>();
			for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
				String propertyName = pd.getName();
				Method readMethod = pd.getReadMethod();
				propertyReadMethods.add(readMethod);
				if (readMethod != null && !isObjectMethod(readMethod)
						&& propertyName.toUpperCase().startsWith(pUpper)) {
					properties.put(propertyName, readMethod);
				}
			}
			// At least in Java 8 (and 9?) Introspector did not return properties based on default read methods, but FreeMarker can.
			for (Method readMethod : parentClass.getMethods()) {
				if (readMethod.isDefault() && !readMethod.isBridge() && readMethod.getReturnType() != void.class && readMethod.getParameterTypes().length == 0) {
					String propertyName = getBeanPropertyNameFromReaderMethodName(readMethod.getName(), readMethod.getReturnType());
					if (propertyName != null) {
						propertyReadMethods.add(readMethod);
						properties.put(propertyName, readMethod);
					}
				}
			}
			properties.forEach((propertyName, readMethod) -> {
				propertyProposals.add(new CompletionProposal(
						propertyName,
						offset - subOffset + 1,
						getContent().length()-1,
						propertyName.length(),
						null, propertyName + " : " + readMethod.getReturnType().getName(), null, null)); //$NON-NLS-1$
			});
			propertyProposals.sort(COMPLETION_PROPOSAL_COMPARATOR);
			
			List<ICompletionProposal> methodProposals = new ArrayList<>();
			Set<Method> methods = new HashSet<>();
			for (MethodDescriptor methodDescriptor : bi.getMethodDescriptors()) {
				methods.add(methodDescriptor.getMethod());
			}
			
			// At least in Java 8 (and 9?) Introspector did not return default methods, but FreeMarker can.
			for (Method method : parentClass.getMethods()) {
				if (method.isDefault()) {
					methods.add(method);
				}
			}
			
			for (Method m : methods) {
				String mName = m.getName();
				if (!m.isBridge() && !m.isSynthetic()
						&& !propertyReadMethods.contains(m) && !isObjectMethod(m)
						&& mName.toUpperCase().startsWith(pUpper)) {
					StringBuilder mLabel = new StringBuilder();
					mLabel.append(mName);
					mLabel.append("("); //$NON-NLS-1$
					boolean first = true;
					for (Class<?> parameterType : m.getParameterTypes()) {
						if (!first) {
							mLabel.append(", "); //$NON-NLS-1$
						}
						mLabel.append(parameterType.getName());
						first = false;
					}
					mLabel.append(") : ").append(m.getReturnType().getName()); //$NON-NLS-1$
					String actual = mName + "()"; //$NON-NLS-1$
					int tLength = actual.length();
					if (m.getParameterTypes().length > 0) {
						tLength--;
					}
					methodProposals.add(new CompletionProposal(actual,
							offset - subOffset + 1, getContent().length()-1, tLength,
							null, mLabel.toString(), null, null));
				}
			}
			methodProposals.sort(COMPLETION_PROPOSAL_COMPARATOR);
			
			List<ICompletionProposal> proposals = new ArrayList<>();
			proposals.addAll(propertyProposals);
			proposals.addAll(methodProposals);
			return completionProposals(proposals, false);
		}
		catch (IntrospectionException e) {
			Plugin.log(e);
			return null;
		}
	}

	/**
	 * Tells if a public method was originally declared in {@link Object}, even if it was overridden.
	 */
	private static boolean isObjectMethod(Method m) {
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
	
    /**
     * Extracts the JavaBeans property from a reader method name, or returns {@code null} if the method name doesn't
     * look like a reader method name. 
     */
    public static String getBeanPropertyNameFromReaderMethodName(String name, Class<?> returnType) {
        int start;
        if (name.startsWith("get")) {
            start = 3;
        } else if (returnType == boolean.class && name.startsWith("is")) {
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
    
}