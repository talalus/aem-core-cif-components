/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.core.components.internal.servlets;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;

@Component(
    property = {
        "sling.filter.scope=REQUEST",
        "sling.filter.methods=GET",
        "sling.filter.extensions=html"
    })
@Designate(ocd = SpecificPageFilterConfiguration.class, factory = true)
public class SpecificPageFilterFactory implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecificPageFilterFactory.class);

    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

        // Skip filter if there isn't any selector in the URL
        String selector = slingRequest.getRequestPathInfo().getSelectorString();
        if (selector == null) {
            chain.doFilter(request, response);
            return;
        }

        Resource page = slingRequest.getResource();
        LOGGER.debug("Checking sub-pages for {}", slingRequest.getRequestURI());

        Iterator<Resource> children = page.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            if (!NameConstants.NT_PAGE.equals(child.getResourceType())) {
                continue;
            }

            Resource jcrContent = child.getChild(JcrConstants.JCR_CONTENT);
            if (jcrContent == null) {
                continue;
            }

            Object filter = jcrContent.getValueMap().get(SELECTOR_FILTER_PROPERTY);
            if (filter == null) {
                continue;
            }

            // The property is saved as a String when it's a simple selection, or an array when a multi-selection is done
            String[] selectors = filter.getClass().isArray() ? ((String[]) filter) : ArrayUtils.toArray((String) filter);

            if (ArrayUtils.contains(selectors, selector)) {
                LOGGER.debug("Page has a matching sub-page for selector {} at {}", selector, child.getPath());
                RequestDispatcher dispatcher = slingRequest.getRequestDispatcher(child);
                dispatcher.forward(slingRequest, response);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}

}
