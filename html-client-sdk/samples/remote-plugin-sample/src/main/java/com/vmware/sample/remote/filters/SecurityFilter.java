/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vmware.sample.remote.gateway.GatewayCredentials;
import com.vmware.sample.remote.services.SecurityService;

public class SecurityFilter implements Filter {

   private SecurityService _securityService;

   @Override
   public void init(final FilterConfig filterConfig) {
      ApplicationContext context = WebApplicationContextUtils.
            getRequiredWebApplicationContext(filterConfig.getServletContext());
      _securityService = context.getBean(SecurityService.class);
   }

   @Override
   public void doFilter(ServletRequest servletRequest,
         ServletResponse servletResponse, FilterChain filterChain)
         throws IOException, ServletException {

      final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

      final GatewayCredentials credentials =
            GatewayCredentials.fromRequestHeaders(httpServletRequest);
      if (!_securityService.validateGatewayCredentials(credentials)) {
         httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         return;
      }

      filterChain.doFilter(servletRequest, servletResponse);
   }

   @Override
   public void destroy() {
   }
}
