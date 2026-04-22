/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

Installer for the Remote Plug-in Sample

This directory contains supplementary files used when registering the Remote Plug-in Sample in order
to enable the plug-in to create vCenter Server tasks and to verify user permissions.
The files are located under "registration/resources"

The Remote Plug-in Sample can be registered in the ExtensionManager in a way to leverage
the task creation and user privilege verification functionalities. This can be accomplished by passing the taskList, faultList, privilegeList and resourceList json files
located under registration/resources to the vCenter plugin registration tool.

The taskList.json contains task IDs and can be passed to the vCenter plugin registration tool with the --taskList argument
The faultList.json contains fault IDs and can be passed to the vCenter plugin registration tool with the --faultList argument
The privilegeList.json contains privilege IDs and can be passed to the vCenter plugin registration tool with the --privilegeList argument
The resourceList.json contains localization information and can be passed to the vCenter plugin registration tool with the --resourceList argument

Example command using the extension-registration script and remote-plugin-path as the Remote Plug-in folder path:
./extension-registration.bat -action registerPlugin -url https://myvcenter/sdk -username administrator@vsphere.local -password <YOUR_SSO_PASSWORD> -key com.vmware.sample.remote -version 1.0.0.0 -pluginUrl https://mydevbox:8443/sample-ui/plugin.json -serverThumbprint 19:FD:2B:0E:62:5E:0E:10:FF:24:34:7A:81:F1:D5:33:19:A7:22:A0:DA:33:27:07:90:0F:8E:8D:72:F1:BD:F1 -c 'Example, Inc.' -n 'Remote Plug-in' -s 'This is a sample plug-in' --taskList remote-plugin-path\registration\resources\taskList.json --faultList remote-plugin-path\registration\resources\faultList.json --privilegeList remote-plugin-path\registration\resources\privilegeList.json --resourceList remote-plugin-path\registration\resources\resourceList.json

In order to prevent localization keys collision when multiple versions of the plug-in are deployed in a linked environment,
the plug-in version is inserted in all localization keys and in the task/fault/privilege IDs.
