/*
 * ******************************************************************
 * Copyright (c) 2024-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.automatic.plugin.registration.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Provides methods for converting thumbprints to a specific format.
 */
public class ThumbprintConverter {
   private static final List<Pair<Pattern, Function<String, String>>> THUMBPRINT_CONVERTERS = Arrays.asList(
         Pair.of(Pattern.compile("^[0-9a-fA-F]{2}:"),
               (thumbprint) -> thumbprint),
         Pair.of(Pattern.compile("^[0-9a-fA-F]{2} "),
               (thumbprint) -> thumbprint.replace(" ", ":")),
         Pair.of(Pattern.compile("^[0-9a-fA-F]{3}"),
               (thumbprint) -> thumbprint.replaceAll("([0-9a-fA-F]{2})", "$1:")
                     .replaceFirst(":$", "")));

   /**
    * Converts the thumbprint to format XX:XX:...:XX. If the provided thumbprint
    * adheres to that format or an unknown one, it is returned unchanged.
    *
    * @param thumbprint the thumbprint to convert
    * @return the converted thumbprint
    */
   public static String convertThumbprintToColonSeparated(String thumbprint) {
      if (StringUtils.isEmpty(thumbprint)) {
         return thumbprint;
      }

      for (Pair<Pattern, Function<String, String>> thumbprintConverter : THUMBPRINT_CONVERTERS) {
         if (thumbprintConverter.getLeft().matcher(thumbprint).lookingAt()) {
            return thumbprintConverter.getRight().apply(thumbprint);
         }
      }

      // The thumbprint does not match any known format. Return it as is
      return thumbprint;
   }
}
