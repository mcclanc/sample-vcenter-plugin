import { constants as fsConstants } from "node:fs";
import { access, stat } from "node:fs/promises";
import { execFile, spawn } from "node:child_process";
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
 * Build the ovftool args array (shared by streaming and probe helpers).
 * @param {object} p  — same shape as deployOvaWithOvftool's first param
 * @returns {string[]}
 */
function buildOvftoolArgs(p) {
  const args = [
    "--acceptAllEulas",
    "--allowExtraConfig",
    "--noSSLVerify",
    "--parallelThreads=4",
    `--name=${p.vmName}`,
    `--datastore=${p.datastore}`,
  ];
  const mappings =
    p.ovfNetworkMappings && typeof p.ovfNetworkMappings === "object" && !Array.isArray(p.ovfNetworkMappings)
      ? p.ovfNetworkMappings
      : null;
  const mappingPairs = mappings
    ? Object.entries(mappings).filter(([k, v]) => String(k || "").trim() && String(v || "").trim())
    : [];
  if (mappingPairs.length > 0) {
    for (const [ovfLabel, portGroup] of mappingPairs) {
      args.push(`--net:${String(ovfLabel).trim()}=${String(portGroup).trim()}`);
    }
  } else {
    args.push(`--net:${p.ovfNetworkLabel}=${p.portGroupName}`);
  }
  const props = p.ovfProperties && typeof p.ovfProperties === "object" ? p.ovfProperties : {};
  for (const [k, v] of Object.entries(props)) {
    if (["string", "number", "boolean"].includes(typeof v)) {
      args.push(`--prop:${k}=${String(v)}`);
    }
  }
  args.push(p.ovaPath, p.viUrl);
  return args;
}

const OVFTOOL_TIMEOUT_MS = 4 * 60 * 60 * 1000; // 4 hours

/**
 * Deploy local OVA to vCenter using OVF Tool, streaming stdout/stderr lines
 * to the optional `onLine` callback as they arrive.
 *
 * @param {object} p
 * @param {string} p.ovftoolPath
 * @param {string} p.ovaPath
 * @param {string} p.viUrl
 * @param {string} p.vmName
 * @param {string} p.datastore
 * @param {string} p.ovfNetworkLabel
 * @param {string} p.portGroupName
 * @param {Record<string, string> | null} [p.ovfNetworkMappings]
 * @param {Record<string, string | number | boolean> | null} [p.ovfProperties]
 * @param {{ onLine?: (e: {stream: 'stdout'|'stderr', text: string}) => void }} [opts]
 * @returns {Promise<{stdoutTail: string, stderrTail: string}>}
 */
export function deployOvaWithOvftool(p, { onLine } = {}) {
  const args = buildOvftoolArgs(p);

  return new Promise((resolve, reject) => {
    const proc = spawn(p.ovftoolPath, args, {
      env: { ...process.env, LANG: process.env.LANG || "C.UTF-8" },
    });

    let stdoutBuf = "";
    let stderrBuf = "";

    const killTimer = setTimeout(() => {
      proc.kill();
      reject(new Error(`ovftool timed out after ${OVFTOOL_TIMEOUT_MS / 60_000} minutes`));
    }, OVFTOOL_TIMEOUT_MS);

    function emitLines(text, stream) {
      if (!onLine) return;
      for (const raw of text.split(/\r?\n/)) {
        const line = raw.trim();
        if (line) onLine({ stream, text: line });
      }
    }

    proc.stdout.on("data", (chunk) => {
      const text = String(chunk);
      stdoutBuf += text;
      emitLines(text, "stdout");
    });

    proc.stderr.on("data", (chunk) => {
      const text = String(chunk);
      stderrBuf += text;
      emitLines(text, "stderr");
    });

    proc.on("error", (err) => {
      clearTimeout(killTimer);
      reject(err);
    });

    proc.on("close", (code) => {
      clearTimeout(killTimer);
      const result = {
        stdoutTail: stdoutBuf.slice(-12_000),
        stderrTail: stderrBuf.slice(-12_000),
      };
      if (code === 0) {
        resolve(result);
      } else {
        const cmdStr = [p.ovftoolPath, ...args].join(" ");
        const err = new Error(`Command failed: ${cmdStr}`);
        err.stdout = stdoutBuf;
        err.stderr = stderrBuf;
        reject(err);
      }
    });
  });
}
