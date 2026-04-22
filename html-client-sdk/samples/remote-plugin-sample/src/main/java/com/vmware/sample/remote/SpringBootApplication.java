/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.vmware.sample.remote.filters.SecurityFilter;

@Configuration
@ImportResource("classpath:spring-context.xml")
@org.springframework.boot.autoconfigure.SpringBootApplication
public class SpringBootApplication {

   public static void main(String[] args) {
      HttpsURLConnection.setDefaultHostnameVerifier(NoopHostnameVerifier.INSTANCE);
      SpringApplication.run(SpringBootApplication.class, args);
   }

   @Bean
   protected FilterRegistrationBean<SecurityFilter> securityFilterRegistration() {
      FilterRegistrationBean<SecurityFilter> registration =
            new FilterRegistrationBean<>();
      registration.setFilter(new SecurityFilter());
      registration.addUrlPatterns("/rest/*");
      registration.setName("securityFilter");
      registration.setOrder(0);
      return registration;
   }
}
