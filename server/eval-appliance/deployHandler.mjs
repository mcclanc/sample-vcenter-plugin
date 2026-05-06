import fs from "node:fs/promises";
import path from "node:path";
import { deployOvaWithOvftool, resolveOvftoolPath } from "./ovftool.mjs";
import { buildViDestinationUrl } from "./viUrl.mjs";
import { getOvfNetworkLabels } from "./ovfInspect.mjs";

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
  let ovfNetworkMappings = null;
  const onmRaw = typeof body.ovfNetworkMappings === "string" ? body.ovfNetworkMappings.trim() : "";
  if (onmRaw) {
    try {
      const parsed = JSON.parse(onmRaw);
      if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
        await fs.unlink(file.path).catch(() => {});
        return res.status(400).json({ error: "ovfNetworkMappings must be a JSON object when provided." });
      }
      ovfNetworkMappings = parsed;
    } catch (e) {
      await fs.unlink(file.path).catch(() => {});
      return res.status(400).json({
        error: `Invalid ovfNetworkMappings JSON: ${e instanceof Error ? e.message : String(e)}`,
      });
    }
  }

  /*
   * Resolve the OVF network label to use for --net:<label>=<portGroup>.
   * Priority: (1) explicit client value, (2) auto-detected from OVF descriptor,
   * (3) "VM Network" as last-resort fallback.
   * Skip detection entirely when ovfNetworkMappings is provided (multi-net path).
   */
  let ovfNetworkLabel = ovfNetworkLabelRaw;
  if (!ovfNetworkLabel && !onmRaw) {
    const detected = await getOvfNetworkLabels(file.path, ext);
    if (detected.length === 1) {
      ovfNetworkLabel = detected[0];
      console.info(`[eval-appliance] auto-detected OVF network label: "${ovfNetworkLabel}"`);
    } else if (detected.length > 1) {
      /* Multiple networks and no explicit mappings: build mappings from detected labels → same port group.
         The port group will be resolved from the `network` field later. */
      const portGroup = str("network");
      if (portGroup) {
        ovfNetworkMappings = Object.fromEntries(detected.map((l) => [l, portGroup]));
        console.info(
          `[eval-appliance] auto-detected ${detected.length} OVF networks; mapping all → "${portGroup}":`,
          detected,
        );
      } else {
        ovfNetworkLabel = detected[0];
        console.info(
          `[eval-appliance] auto-detected ${detected.length} OVF networks but no port group; using first label: "${ovfNetworkLabel}"`,
        );
      }
    }
  }
  if (!ovfNetworkLabel) ovfNetworkLabel = "VM Network";

  if (!vcHost || !vcUsername || !vcPassword) {
    await fs.unlink(file.path).catch(() => {});
    return res.status(400).json({
      error: "vCenter hostname, username, and password are required for deployment.",
    });
  }
  const mappingEntries =
    ovfNetworkMappings && typeof ovfNetworkMappings === "object" && !Array.isArray(ovfNetworkMappings)
      ? Object.entries(ovfNetworkMappings).filter(([k, v]) => String(k || "").trim() && String(v || "").trim())
      : [];
  const useMultiNet = mappingEntries.length > 0;

  if (!vmName || !datastore) {
    await fs.unlink(file.path).catch(() => {});
    return res.status(400).json({
      error: "VM name and datastore are required.",
    });
  }
  if (!useMultiNet && !network) {
    await fs.unlink(file.path).catch(() => {});
    return res.status(400).json({
      error: "Destination port group (network) is required when ovfNetworkMappings is not used.",
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
  console.info("[eval-appliance] deploy upload received", {
    originalname: file.originalname,
    size: file.size,
    vmName,
    vcHost,
  });

  /* ── Resolve ovftool before switching to streaming ────────────────────── */
  const ovftoolPath = await resolveOvftoolPath();
  if (!ovftoolPath) {
    await fs.unlink(ovaPath).catch(() => {});
    return res.status(501).json({
      error:
        "VMware OVF Tool (ovftool) was not found on this server. Install it or place `ovftool` under tools/ovftool, or set OVFTOOL_PATH.",
    });
  }

  /* ── Switch to SSE streaming — all further communication via events ───── */
  res.setHeader("Content-Type", "text/event-stream; charset=utf-8");
  res.setHeader("Cache-Control", "no-cache, no-transform");
  res.setHeader("X-Accel-Buffering", "no"); // disable nginx / vCenter proxy buffering
  res.flushHeaders();

  /** Write one SSE event. Safe to call after response end (no-op). */
  const send = (obj) => {
    if (!res.writableEnded) res.write("data: " + JSON.stringify(obj) + "\n\n");
  };

  try {
    const viUrl = buildViDestinationUrl({ vcHost, vcUsername, vcPassword, datacenter, computePath });

    console.info("[eval-appliance] starting ovftool", {
      ovftoolPath,
      ovaPath,
      vmName,
      datastore,
      ovfNetworkLabel,
      portGroup: network,
      multiNet: useMultiNet,
    });

    send({ type: "status", message: "ovftool is deploying the OVA to vCenter…" });

    const ovftoolOut = await deployOvaWithOvftool(
      {
        ovftoolPath,
        ovaPath,
        viUrl,
        vmName,
        datastore,
        ovfNetworkLabel,
        portGroupName: network,
        ovfNetworkMappings: useMultiNet ? ovfNetworkMappings : null,
        ovfProperties,
      },
      {
        onLine: ({ stream, text }) => {
          send({ type: "line", stream, text });
        },
      },
    );

    await fs.unlink(ovaPath).catch(() => {});

    send({
      type: "done",
      ok: true,
      message: "Deploy finished. Confirm the new VM appears in the vCenter inventory.",
      upload: { originalname: file.originalname, bytes: file.size },
      ovftool: ovftoolOut,
    });
  } catch (err) {
    await fs.unlink(ovaPath).catch(() => {});
    const msg = err instanceof Error ? err.message : String(err);
    const ovftoolStderr = err instanceof Error && err.stderr ? String(err.stderr).slice(-10_000) : "";
    const ovftoolStdout = err instanceof Error && err.stdout ? String(err.stdout).slice(-10_000) : "";
    console.error("[eval-appliance] deploy failed:", msg);
    if (ovftoolStderr) console.error("[eval-appliance] ovftool stderr:\n", ovftoolStderr);
    send({
      type: "done",
      ok: false,
      error: msg,
      ovftoolStderr: ovftoolStderr || undefined,
      ovftoolStdout: ovftoolStdout || undefined,
      hint: "Check vCenter credentials, datacenter/compute path, datastore, and OVF network label.",
    });
  } finally {
    res.end();
  }
}
