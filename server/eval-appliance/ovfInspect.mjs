import { open, readFile } from "node:fs/promises";

const utf8 = new TextDecoder("utf-8");

/**
 * Walk a ustar/GNU TAR stream from disk and return the first .ovf member as UTF-8 text.
 * Reads only what it needs (TAR headers + the .ovf payload); does not load the whole file.
 * @param {string} ovaPath
 * @returns {Promise<string>}
 */
async function extractOvfXmlFromOvaDisk(ovaPath) {
  const fh = await open(ovaPath, "r");
  try {
    const header = Buffer.alloc(512);
    let offset = 0;
    let gnuLongName = null;

    while (true) {
      const { bytesRead } = await fh.read(header, 0, 512, offset);
      if (bytesRead < 512) break;

      /* End-of-archive: two consecutive all-zero 512-byte blocks */
      let sum = 0;
      for (let i = 0; i < 512; i += 1) sum += header[i];
      if (sum === 0) break;

      const name = header.subarray(0, 100).toString("utf8").replace(/\0.*$/s, "");
      const prefix = header.subarray(345, 500).toString("utf8").replace(/\0.*$/s, "");
      const fullName = prefix ? `${prefix}/${name}`.replace(/\/+/g, "/") : name;
      const sizeRaw = header.subarray(124, 136).toString("utf8").trim();
      const size = parseInt(sizeRaw, 8) || 0;
      const typeflag = header[156];

      const contentStart = offset + 512;
      const nextBlock = contentStart + Math.ceil(size / 512) * 512;

      if (typeflag === 76) {
        /* GNU LongLink — real file name is in this payload */
        const buf = Buffer.alloc(size);
        await fh.read(buf, 0, size, contentStart);
        gnuLongName = buf.toString("utf8").replace(/\0.*$/s, "");
        offset = nextBlock;
        continue;
      }

      const entryName = (gnuLongName || fullName).replace(/\\/g, "/");
      gnuLongName = null;

      const isRegular = typeflag === 0 || typeflag === 48;
      if (isRegular && entryName.toLowerCase().endsWith(".ovf")) {
        const buf = Buffer.alloc(size);
        await fh.read(buf, 0, size, contentStart);
        return utf8.decode(buf);
      }

      offset = nextBlock;
    }

    throw new Error("No .ovf descriptor found inside the OVA (expected a TAR member ending in .ovf).");
  } finally {
    await fh.close();
  }
}

/**
 * Extract OVF `<Network name="...">` labels from OVF XML via regex.
 * Handles both plain XML and namespace-prefixed forms used by VMware OVF tool:
 *   <Network name="Network 1">
 *   <ovf:Network ovf:name="Network 1">
 * @param {string} xml
 * @returns {string[]}
 */
function extractNetworkNamesFromXml(xml) {
  const names = [];
  const seen = new Set();
  /*
   * Pattern breakdown:
   *   <(?:\w[\w.-]*:)?   — optional namespace prefix e.g. "ovf:"
   *   Network\b          — tag local name "Network"
   *   [^>]*?             — any other attributes (non-greedy)
   *   \b(?:\w[\w.-]*:)?  — word boundary + optional attr namespace prefix e.g. "ovf:"
   *   name\s*=\s*        — "name" attribute
   *   ["']([^"']+)["']   — quoted value captured in group 1
   */
  const re = /<(?:\w[\w.-]*:)?Network\b[^>]*?\b(?:\w[\w.-]*:)?name\s*=\s*["']([^"']+)["']/gi;
  let m;
  while ((m = re.exec(xml)) !== null) {
    const n = m[1].trim();
    if (n && !seen.has(n)) {
      seen.add(n);
      names.push(n);
    }
  }
  return names;
}

/**
 * Return OVF network labels from an .ova or .ovf file on disk.
 * Never throws — returns [] on any error so callers can fall back gracefully.
 * @param {string} filePath
 * @param {string} ext  lowercase file extension, e.g. ".ova" or ".ovf"
 * @returns {Promise<string[]>}
 */
export async function getOvfNetworkLabels(filePath, ext) {
  try {
    const xml =
      ext === ".ovf"
        ? await readFile(filePath, "utf8")
        : await extractOvfXmlFromOvaDisk(filePath);
    return extractNetworkNamesFromXml(xml);
  } catch (err) {
    console.warn("[ovfInspect] could not extract network labels:", err?.message ?? err);
    return [];
  }
}
