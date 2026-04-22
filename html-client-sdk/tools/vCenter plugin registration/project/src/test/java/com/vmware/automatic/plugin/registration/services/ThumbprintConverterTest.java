/*
 * ******************************************************************
 * Copyright (c) 2024-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.automatic.plugin.registration.services;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ThumbprintConverterTest {
   @Test
   public void returnsThumbprintUnchangedWhenConvertingColonSeparatedOne() {
      String thumbprint = "59:93:D9:CF:54:CB:32:60:93:58:CE:45:AD:4D:D7:1F:1C:08:1D:0D:B6:A5:3D:F5:3E:D7:A9:CA:E2:32:DE:41";

      Assert.assertSame(
            ThumbprintConverter.convertThumbprintToColonSeparated(thumbprint),
            thumbprint);
   }

   @Test
   public void returnsColonSeparatedThumbprintWhenInputIsSpaceSeparated() {
      String thumbprint = "59 93 D9 CF 54 CB 32 60 93 58 CE 45 AD 4D D7 1F 1C 08 1D 0D B6 A5 3D F5 3E D7 A9 CA E2 32 DE 41";

      Assert.assertEquals(
            ThumbprintConverter.convertThumbprintToColonSeparated(thumbprint),
            "59:93:D9:CF:54:CB:32:60:93:58:CE:45:AD:4D:D7:1F:1C:08:1D:0D:B6:A5:3D:F5:3E:D7:A9:CA:E2:32:DE:41");
   }

   @Test
   public void returnsColonSeparatedThumbprintWhenInputIsNotSeparated() {
      String thumbprint = "5993D9CF54CB32609358CE45AD4DD71F1C081D0DB6A53DF53ED7A9CAE232DE41";

      Assert.assertEquals(
            ThumbprintConverter.convertThumbprintToColonSeparated(thumbprint),
            "59:93:D9:CF:54:CB:32:60:93:58:CE:45:AD:4D:D7:1F:1C:08:1D:0D:B6:A5:3D:F5:3E:D7:A9:CA:E2:32:DE:41");
   }
}
