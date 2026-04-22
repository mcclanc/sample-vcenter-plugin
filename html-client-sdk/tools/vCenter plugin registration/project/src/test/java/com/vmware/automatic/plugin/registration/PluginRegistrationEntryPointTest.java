/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration;

import com.vmware.automatic.plugin.registration.commands.PluginCmd;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PluginRegistrationEntryPointTest {

   private PluginCmdInstanceCreator pluginCmdInstanceCreator;
   private CommandLineParser parser;
   private String[] cmdLineArguments;
   private PluginRegistrationEntryPoint entryPoint;

   @Test(expectedExceptions = ParseException.class)
   public void execute_whenParserFails_throwsException() throws Exception {
      beforeEach();
      EasyMock.expect(parser.parse(EasyMock.anyObject(Options.class),
            EasyMock.eq(cmdLineArguments), EasyMock.eq(true)))
            .andThrow(new ParseException("")).once();
      EasyMock.replay(parser);
      entryPoint.execute();
      EasyMock.verify(parser);
   }

   @Test
   public void execute_whenParserSucceeds_executesPluginCommand()
         throws Exception {
      beforeEach();
      final String action = "test-action";
      final PluginCmd pluginCmd = EasyMock.createMock(PluginCmd.class);
      final CommandLine cmdLine = EasyMock.createMock(CommandLine.class);
      EasyMock.expect(parser.parse(EasyMock.anyObject(Options.class),
            EasyMock.eq(cmdLineArguments), EasyMock.eq(true)))
            .andReturn(cmdLine).once();
      EasyMock.expect(cmdLine.getOptionValue("action")).andReturn(action)
            .once();
      EasyMock.expect(pluginCmdInstanceCreator.getInstance(action)).andReturn(pluginCmd)
            .once();
      pluginCmd.execute(cmdLineArguments);
      EasyMock.expectLastCall().once();
      // replays
      EasyMock.replay(parser, cmdLine, pluginCmdInstanceCreator, pluginCmd);
      entryPoint.execute();
      // verify
      EasyMock.verify(parser, cmdLine, pluginCmdInstanceCreator, pluginCmd);
   }

   @Test
   public void execute_whenActionIsNotFirstCmdLineArgument_successfullyParsesCmdLineArgs()
         throws Exception {
      final String[] cmdLineArgs = new String[] { "-url", "some-url", "-action",
            "registerPlugin" };
      final String[] expectedArgs = new String[] { "-action",
            "registerPlugin" };
      beforeEach(cmdLineArgs);
      assertCmdLineArgs(expectedArgs);
   }

   @Test
   public void execute_whenActionIsNotFirstCmdLineArgument_andHasNoValue_correctlyParsesCmdLineArgs()
         throws Exception {
      final String[] cmdLineArgs = new String[] { "-url", "some-url",
            "-action" };
      final String[] expectedArgs = new String[] { "-action" };
      beforeEach(cmdLineArgs);
      assertCmdLineArgs(expectedArgs);
   }

   @Test
   public void execute_whenActionIsNotFirstCmdLineArgument_andHasNoValueButAnotherArgument_correctlyParsesCmdLineArgs()
         throws Exception {
      final String[] cmdLineArgs = new String[] { "-url", "some-url", "-action",
            "-p" };
      final String[] expectedArgs = new String[] { "-action" };
      beforeEach(cmdLineArgs);
      assertCmdLineArgs(expectedArgs);
   }

   private void beforeEach() {
      beforeEach(new String[0]);
   }

   private void beforeEach(String[] cmdLineArgs) {
      pluginCmdInstanceCreator = EasyMock
            .createMock(PluginCmdInstanceCreator.class);
      parser = EasyMock.createMock(DefaultParser.class);
      cmdLineArguments = cmdLineArgs;
      entryPoint = new PluginRegistrationEntryPoint(cmdLineArguments,
            pluginCmdInstanceCreator, parser);
   }

   private void assertCmdLineArgs(final String[] expectedArgs)
         throws Exception {
      final String action = "test-action";
      final PluginCmd pluginCmd = EasyMock.createMock(PluginCmd.class);
      final CommandLine cmdLine = EasyMock.createMock(CommandLine.class);
      // Capture parsed cmdLineArgs to assert them later on
      Capture<String[]> capturedOption = Capture.newInstance();
      // Expect mock calls
      EasyMock.expect(parser.parse(EasyMock.anyObject(Options.class),
            EasyMock.capture(capturedOption), EasyMock.eq(true)))
            .andReturn(cmdLine).once();
      EasyMock.expect(cmdLine.getOptionValue("action")).andReturn(action)
            .once();
      EasyMock.expect(pluginCmdInstanceCreator.getInstance(action))
            .andReturn(pluginCmd).once();
      // Expect void method
      pluginCmd.execute(cmdLineArguments);
      EasyMock.expectLastCall().once();
      // replays
      EasyMock.replay(parser, cmdLine, pluginCmdInstanceCreator, pluginCmd);
      entryPoint.execute();
      // verify method calls
      EasyMock.verify(parser, cmdLine, pluginCmdInstanceCreator, pluginCmd);
      // verify action cmd argument is extracted correctly
      Assert.assertEquals(capturedOption.getValue(), expectedArgs);
   }
}
