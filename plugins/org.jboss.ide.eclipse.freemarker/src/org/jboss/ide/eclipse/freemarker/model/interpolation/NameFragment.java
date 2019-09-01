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


import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.jboss.ide.eclipse.freemarker.configuration.ConfigurationManager;
import org.jboss.ide.eclipse.freemarker.configuration.ContextValue;
import org.jboss.ide.eclipse.freemarker.util.TypeUtils;
import org.jboss.ide.eclipse.freemarker.util.TypeUtils.BeanMembers;

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
			String content = getContent().trim();
			if (isStartFragment()) {
				returnClass = context.get(content);
			} else {
				if (null == parentClass) {
					returnClass = Object.class;
				} else {
					String subvarName = contentToSubvarName(content);
					BeanMembers beanMembers = TypeUtils.getBeanMembers(parentClass, true, true, false,
							(name, method) -> subvarName.equals(name));
					if (!beanMembers.getMethods().isEmpty()) {
						returnClass = Method.class; // Can't encode the return type into this
					} else if (!beanMembers.getProperties().isEmpty()) {
						returnClass = beanMembers.getProperties().get(0).getReturnType();
					} else {
						returnClass = Object.class;
					}
				}
			}
		}
		return returnClass;
	}

	private String contentToSubvarName(String content) {
		String s = content;
		if (s.startsWith(".")) { //$NON-NLS-1$
			s = s.substring(1);
		}
		String subvarName = s.trim();
		return subvarName;
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
				|| instanceOf(parentClass, ResourceBundle.class)) {
			return null;
		}
		
		String prefix = getContent().substring(1, subOffset);
		String prefixUpper = prefix.toUpperCase();
		
		BeanMembers beanMembers = TypeUtils.getBeanMembers(
				parentClass, true, true, true,
				(name, method) ->
						TypeUtils.filterForCompletionProposal(name, method)
						&& name.toUpperCase().startsWith(prefixUpper));
		
		List<ICompletionProposal> propertyProposals = beanMembers.getProperties().stream()
				.map(beanMember -> new CompletionProposal(
						beanMember.getName(),
						offset - subOffset + 1,
						getContent().length()-1,
						beanMember.getName().length(),
						null,
						beanMember.getName() + " : " + beanMember.getReturnType().getName(), //$NON-NLS-1$
						null, null))
				.sorted(COMPLETION_PROPOSAL_COMPARATOR)
				.collect(Collectors.toList());
		
		List<ICompletionProposal> methodProposals = beanMembers.getMethods().stream()
				.map(beanMember -> {
					String mName = beanMember.getName();
					
					StringBuilder mLabel = new StringBuilder();
					mLabel.append(mName);
					mLabel.append("("); //$NON-NLS-1$
					boolean first = true;
					Class<?>[] parameterTypes = beanMember.getAccessor().getParameterTypes();
					for (Class<?> parameterType : parameterTypes) {
						if (!first) {
							mLabel.append(", "); //$NON-NLS-1$
						}
						mLabel.append(parameterType.getName());
						first = false;
					}
					mLabel.append(") : ").append(beanMember.getReturnType().getName()); //$NON-NLS-1$
					String insertedText = mName + "()"; //$NON-NLS-1$
					int cursorTextLength = insertedText.length();
					if (parameterTypes.length > 0) {
						cursorTextLength--;
					}
					
					return new CompletionProposal(insertedText,
							offset - subOffset + 1, getContent().length()-1, cursorTextLength,
							null, mLabel.toString(), null, null);								
				})
				.sorted(COMPLETION_PROPOSAL_COMPARATOR)
				.collect(Collectors.toList());
		
		List<ICompletionProposal> proposals = new ArrayList<>();
		proposals.addAll(propertyProposals);
		proposals.addAll(methodProposals);
		return completionProposals(proposals, false);
	}
    
}