import { constants as fsConstants } from "node:fs";
import { access, stat } from "node:fs/promises";
import { execFile } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

/** OVF Tool binaries are multi‑MB; avoids matching tiny wrong files. */
const MIN_OVFTOOL_BYTES = 300_000;

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.join(__dirname, "..", "..");
/** Manually dropped binary, e.g. user copied `ovftool` to repo `tools/`. */
const manualToolsOvftool = path.join(repoRoot, "tools", "ovftool");
const manualToolsOvftoolWin = path.join(repoRoot, "tools", "ovftool.exe");
const bundledOvftool = path.join(repoRoot, "tools", "vendor", "ovftool", "ovftool");

/** Under repo `tools/`, `ovftool --version` can hang on first Rosetta launch; use stat-only probe. */
function useStatOnlyProbe(absPath) {
  const toolsDir = path.resolve(repoRoot, "tools") + path.sep;
  return path.resolve(absPath).startsWith(toolsDir);
}

async function probeOvftoolPath(p) {
  if (useStatOnlyProbe(p)) {
    try {
      const s = await stat(p);
      return s.isFile() && s.size >= MIN_OVFTOOL_BYTES;
    } catch {
      return false;
    }
  }
  try {
    await access(p, fsConstants.F_OK);
    await execFileAsync(p, ["--version"], { timeout: 25_000 });
    return true;
  } catch {
    try {
      await execFileAsync(p, ["--version"], { timeout: 25_000 });
      return true;
    } catch {
      return false;
    }
  }
}

function candidateOvftoolPaths() {
  const fromEnv = process.env.OVFTOOL_PATH ? [process.env.OVFTOOL_PATH] : [];
  return [
    ...fromEnv,
    manualToolsOvftool,
    manualToolsOvftoolWin,
    bundledOvftool,
    "/usr/lib/vmware-ovf-tool/ovftool",
    "/usr/bin/ovftool",
    "/Applications/VMware OVF Tool/ovftool",
    "C:\\Program Files\\VMware\\VMware OVF Tool\\ovftool.exe",
    "ovftool",
  ];
}

/** @returns {Promise<string | null>} */
export async function resolveOvftoolPath() {
  for (const p of candidateOvftoolPaths()) {
    if (await probeOvftoolPath(p)) return p;
  }
  return null;
}

/**
 * Deploy local OVA to vCenter using OVF Tool.
 * @param {object} p
 * @param {string} p.ovftoolPath
 * @param {string} p.ovaPath
 * @param {string} p.viUrl
 * @param {string} p.vmName
 * @param {string} p.datastore
 * @param {string} p.ovfNetworkLabel - e.g. "VM Network" from the OVF descriptor
 * @param {string} p.portGroupName - vSphere distributed or standard port group name
 * @param {Record<string, string | number | boolean> | null} [p.ovfProperties]
 */
export async function deployOvaWithOvftool(p) {
  const args = [
    "--acceptAllEulas",
    "--allowExtraConfig",
    "--noSSLVerify",
    `--name=${p.vmName}`,
    `--datastore=${p.datastore}`,
    `--net:${p.ovfNetworkLabel}=${p.portGroupName}`,
  ];
  const props = p.ovfProperties && typeof p.ovfProperties === "object" ? p.ovfProperties : {};
  for (const [k, v] of Object.entries(props)) {
    if (["string", "number", "boolean"].includes(typeof v)) {
      args.push(`--prop:${k}=${String(v)}`);
    }
  }
  args.push(p.ovaPath, p.viUrl);
  const { stdout, stderr } = await execFileAsync(p.ovftoolPath, args, {
    maxBuffer: 64 * 1024 * 1024,
    timeout: 4 * 60 * 60 * 1000,
    env: { ...process.env, LANG: process.env.LANG || "C.UTF-8" },
  });
  return {
    stdoutTail: String(stdout || "").slice(-12_000),
    stderrTail: String(stderr || "").slice(-12_000),
  };
}
