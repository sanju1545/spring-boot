/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.security.servlet;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * {@link ApplicationContext} backed {@link RequestMatcher}. Can work directly with the
 * {@link ApplicationContext}, obtain an existing bean or
 * {@link AutowireCapableBeanFactory#createBean(Class, int, boolean) create a new bean}
 * that is autowired in the usual way.
 *
 * @param <C> The type of the context that the match method actually needs to use. Can be
 * an {@link ApplicationContext}, a class of an {@link ApplicationContext#getBean(Class)
 * existing bean} or a custom type that will be
 * {@link AutowireCapableBeanFactory#createBean(Class, int, boolean) created} on demand.
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class ApplicationContextRequestMatcher<C> implements RequestMatcher {

	private final Class<? extends C> contextClass;

	private volatile C context;

	private Object contextLock = new Object();

	public ApplicationContextRequestMatcher(Class<? extends C> contextClass) {
		Assert.notNull(contextClass, "Context class must not be null");
		this.contextClass = contextClass;
	}

	@Override
	public final boolean matches(HttpServletRequest request) {
		return matches(request, getContext(request));
	}

	/**
	 * Decides whether the rule implemented by the strategy matches the supplied request.
	 * @param request the source request
	 * @param context the context instance
	 * @return if the request matches
	 */
	protected abstract boolean matches(HttpServletRequest request, C context);

	private C getContext(HttpServletRequest request) {
		if (this.context == null) {
			synchronized (this.contextLock) {
				this.context = createContext(request);
				initialized(this.context);
			}
		}
		return this.context;
	}

	/**
	 * Called once the context has been initialized.
	 * @param context the initialized context
	 */
	protected void initialized(C context) {
	}

	@SuppressWarnings("unchecked")
	private C createContext(HttpServletRequest request) {
		WebApplicationContext context = WebApplicationContextUtils
				.getRequiredWebApplicationContext(request.getServletContext());
		if (this.contextClass.isInstance(context)) {
			return (C) context;
		}
		try {
			return context.getBean(this.contextClass);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return (C) context.getAutowireCapableBeanFactory().createBean(
					this.contextClass, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR,
					false);
		}
	}

}
