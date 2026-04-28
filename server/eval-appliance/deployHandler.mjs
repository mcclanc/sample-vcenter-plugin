import fs from "node:fs/promises";
import path from "node:path";
import { deployOvaWithOvftool, resolveOvftoolPath } from "./ovftool.mjs";
import { buildViDestinationUrl } from "./viUrl.mjs";

const ALLOWED_EXT = new Set([".ova", ".ovf"]);

function extOfUpload(originalname) {
  const ext = path.extname(String(originalname || "")).toLowerCase();
  return ext;
}

/**
 * POST /…/api/eval-appliance/deploy — multipart form: `ovaFile` + vCenter / OVF fields; deploy with ovftool.
 * Expects multer middleware: `ovaUpload.single('ovaFile')` before this handler.
 */
export async function evalApplianceDeployHandler(req, res) {
  /* Only multipart upload + vCenter fields; no Broadcom Support Portal / registry token. */
  const file = req.file;
  if (!file?.path) {
    return res.status(400).json({
      error: "OVA/OVF package upload is required (form field name: ovaFile).",
    });
  }

  const ext = extOfUpload(file.originalname);
  if (!ALLOWED_EXT.has(ext)) {
    await fs.unlink(file.path).catch(() => {});
    return res.status(400).json({
      error: `Uploaded file must be .ova or .ovf (got ${ext || "(no extension)"}).`,
    });
  }

  const body = req.body && typeof req.body === "object" ? req.body : {};
  const str = (k) => (typeof body[k] === "string" ? body[k].trim() : "");

  const vcHost = str("vcHost");
  const vcUsername = str("vcUsername");
  const vcPassword = typeof body.vcPassword === "string" ? body.vcPassword : "";
  const vmName = str("vmName");
  const datastore = str("datastore");
  const network = str("network");
  const datacenter = str("datacenter");
  const computePath = str("computePath");
  const ovfNetworkLabelRaw = str("ovfNetworkLabel");
  const ovfNetworkLabel = ovfNetworkLabelRaw || "VM Network";

  if (!vcHost || !vcUsername || !vcPassword) {
    await fs.unlink(file.path).catch(() => {});
    return res.status(400).json({
      error: "vCenter hostname, username, and password are required for deployment.",
    });
  }
  if (!vmName || !datastore || !network) {
    await fs.unlink(file.path).catch(() => {});
    return res.status(400).json({
      error: "VM name, datastore, and network (port group) are required.",
    });
  }
  if (!datacenter || !computePath) {
    await fs.unlink(file.path).catch(() => {});
    return res.status(400).json({
      error: "Datacenter name and compute path are required (e.g. compute path: host/ClusterName).",
    });
  }

  let ovfProperties = null;
  const opRaw = typeof body.ovfProperties === "string" ? body.ovfProperties.trim() : "";
  if (opRaw) {
    try {
      ovfProperties = JSON.parse(opRaw);
      if (typeof ovfProperties !== "object" || ovfProperties === null || Array.isArray(ovfProperties)) {
        await fs.unlink(file.path).catch(() => {});
        return res.status(400).json({ error: "ovfProperties must be a JSON object when provided." });
      }
    } catch (e) {
      await fs.unlink(file.path).catch(() => {});
      return res.status(400).json({
        error: `Invalid ovfProperties JSON: ${e instanceof Error ? e.message : String(e)}`,
      });
    }
  }

  const ovaPath = file.path;
  console.info("[eval-appliance] deploy upload", {
    originalname: file.originalname,
    size: file.size,
    vmName,
    vcHost,
  });

  try {
    const ovftoolPath = await resolveOvftoolPath();
    if (!ovftoolPath) {
      await fs.unlink(ovaPath).catch(() => {});
      return res.status(501).json({
        error:
          "VMware OVF Tool (ovftool) was not found on this server. Install it or place `ovftool` under tools/ovftool, or set OVFTOOL_PATH.",
      });
    }

    const viUrl = buildViDestinationUrl({
      vcHost,
      vcUsername,
      vcPassword,
      datacenter,
      computePath,
    });

    console.info("[eval-appliance] deploying with ovftool", {
      ovftoolPath,
      ovaPath,
      vmName,
      datastore,
      ovfNetworkLabel,
      portGroup: network,
    });

    const ovftoolOut = await deployOvaWithOvftool({
      ovftoolPath,
      ovaPath,
      viUrl,
      vmName,
      datastore,
      ovfNetworkLabel,
      portGroupName: network,
      ovfProperties,
    });

    await fs.unlink(ovaPath).catch(() => {});

    return res.json({
      ok: true,
      message:
        "Deploy finished: the uploaded package was deployed with ovftool (no errors thrown). Confirm the new VM in vCenter inventory.",
      upload: { originalname: file.originalname, bytes: file.size },
      ovftool: ovftoolOut,
    });
  } catch (err) {
    await fs.unlink(ovaPath).catch(() => {});
    const msg = err instanceof Error ? err.message : String(err);
    console.error("[eval-appliance] failed", msg);
    return res.status(500).json({
      error: msg,
      hint:
        "Check vCenter credentials, datacenter/compute path, datastore, OVF network label, and OVF properties. ovftool network labels must match the OVF (see ovftool --help).",
    });
  }
}
