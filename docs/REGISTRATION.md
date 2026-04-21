# Registering the remote plug-in with vCenter

Follow the **vSphere Client SDK** installation and registration chapters in Broadcom’s guide: [Developing Remote Plug-ins with the vSphere Client SDK](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/9-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0.html).

## Manifest URL

With the dev server in this repo, the manifest is:

`https://<your-host>:8443/tanzu-hub-poc-ui/plugin.json`

Use a hostname and certificate that vCenter trusts (or add your CA to trust). Self-signed dev certs require registering the server thumbprint per the SDK registration tool / API.

## TLS for local development

Generate or reuse a cert (for example `mkcert`) and start the server with:

```bash
export SSL_KEY_PATH=/path/to/key.pem
export SSL_CERT_PATH=/path/to/cert.pem
npm start
```

## Related doc links

- [Register the vSphere Client Remote Plug-in Sample](https://techdocs.broadcom.com/us/en/vmware-cis/vsphere/vsphere-sdks-tools/8-0/developing-remote-plug-ins-with-the-vsphere-client-sdk-8-0/using-the-vsphere-client-remote-plug-in-sample/register-the-vsphere-client-remote-plug-in-sample.html) (same family of steps for your own manifest URL).
