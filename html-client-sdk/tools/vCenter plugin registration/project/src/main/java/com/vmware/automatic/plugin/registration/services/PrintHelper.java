/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import jakarta.xml.ws.soap.SOAPFaultException;

public class PrintHelper {

   public static void printHelp(Options options,  String footer) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.setWidth(100);
      formatter.printHelp("extension-registration", "\nRegister/Unregister/Update plugin to the VC\nOptions:", options, "\n"
            + footer, true);
   }

   /**
    * Writes info about the specified exception to standard output.
    */
   public static void printSoapFaultException(SOAPFaultException sfe) {
      System.out.println("SOAP Fault -");
      if (sfe.getFault().hasDetail()) {
         System.out.println(sfe.getFault().getDetail().getFirstChild().getLocalName());
      }
      if (sfe.getFault().getFaultString() != null) {
         System.out.println("\n Message: " + sfe.getFault().getFaultString());
      }
   }
}
