/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.controllers;

import com.vmware.sample.remote.exception.RemotePluginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for generic exception handling
 */
@ControllerAdvice
public class BaseControllerAdvice {

   private static final Log logger = LogFactory.getLog(BaseControllerAdvice.class);

   /**
    * Generic handling of unexpected internal exceptions.
    * Sends a 500 server error response along with a locale key for the UI
    * to localize the error message.
    *
    * @param ex The exception that was thrown.
    * @return a response with the exception stackTrace and locale key.
    */
   @ExceptionHandler(Exception.class)
   public ResponseEntity<Map<String, String>> handleException(
         final Exception ex) {
      logger.error("Unexpected internal exception", ex);
      return buildErrorResponse(ex, "errors.general");
   }

   /**
    * Generic handling of expected exceptions.
    * Sends a 500 server error response along with a locale key for the UI
    * to localize the error message.
    *
    * @param ex The exception that was thrown.
    * @return a response with the exception stackTrace and locale key.
    */
   @ExceptionHandler(RemotePluginException.class)
   public ResponseEntity<Map<String, String>> handleException(
         final RemotePluginException ex) {
      logger.error("Expected exception", ex);
      return buildErrorResponse(ex, ex.getLocaleKey());
   }

   private ResponseEntity<Map<String, String>> buildErrorResponse(
         final Exception ex, final String localeKey) {
      final Map<String, String> errorMap = new HashMap<>();
      errorMap.put("localeKey", localeKey);
      errorMap.put("stackTrace", printStackTrace(ex));
      return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
   }

   private String printStackTrace(Exception ex) {
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      return sw.toString();
   }
}
