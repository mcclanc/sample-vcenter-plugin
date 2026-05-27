/**
 * Client-side read of .ovf or .ova (TAR) packages to extract OVF descriptor:
 * Product properties, network names, and VM id hint for the deploy modal.
 */

const textDecoder = new TextDecoder("utf-8");

/** @param {Uint8Array} bytes @param {number} max */
function readCString(bytes, max) {
  let end = 0;
  while (end < max && bytes[end] !== 0) end += 1;
  return textDecoder.decode(bytes.subarray(0, end));
}

/** @param {Uint8Array} h */
function parseTarHeaderBlock(h) {
  let sum = 0;
  for (let i = 0; i < 512; i += 1) sum += h[i];
  if (sum === 0) return null;

  const name = readCString(h.subarray(0, 100), 100);
  const prefix = readCString(h.subarray(345, 500), 155);
  const fullName = prefix ? `${prefix}/${name}`.replace(/\/+/g, "/") : name;
  const sizeRaw = readCString(h.subarray(124, 136), 12).trim();
  const size = Number.parseInt(sizeRaw, 8) || 0;
  const typeflag = h[156];
  return { name, prefix, fullName, size, typeflag };
}

/**
 * Walk a POSIX / GNU ustar TAR stream and return the first .ovf member body as UTF-8 text.
 * @param {File} file
 * @returns {Promise<string>}
 */
export async function extractOvfXmlFromOva(file) {
  let offset = 0;
  const fileSize = file.size;
  /** @type {string | null} */
  let gnuLongName = null;

  while (offset + 512 <= fileSize) {
    const headerBuf = await file.slice(offset, offset + 512).arrayBuffer();
    const h = new Uint8Array(headerBuf);
    const parsed = parseTarHeaderBlock(h);
    if (!parsed) break;

    const { fullName, size, typeflag } = parsed;
    const contentStart = offset + 512;
    const contentEnd = contentStart + size;
    const nextBlock = contentStart + Math.ceil(size / 512) * 512;

    if (typeflag === 76) {
      /* GNU longlink: next file name is in this block's payload */
      const payload = await file.slice(contentStart, contentEnd).arrayBuffer();
      gnuLongName = readCString(new Uint8Array(payload), size);
      offset = nextBlock;
      continue;
    }

    const entryName = (gnuLongName || fullName).replace(/\\/g, "/");
    gnuLongName = null;

    const isRegular = typeflag === 0 || typeflag === 48; /* '0' */
    if (isRegular && entryName.toLowerCase().endsWith(".ovf")) {
      const payload = await file.slice(contentStart, contentEnd).arrayBuffer();
      return textDecoder.decode(new Uint8Array(payload));
    }

    offset = nextBlock;
  }

  throw new Error(
    "No .ovf descriptor found inside this OVA (expected a TAR member ending in .ovf). The archive layout may be non-standard.",
  );
}

/**
 * @param {File} file
 * @returns {Promise<string>}
 */
export async function readOvfXmlFromFile(file) {
  const n = file.name.toLowerCase();
  if (n.endsWith(".ovf")) {
    return file.text();
  }
  if (n.endsWith(".ova")) {
    return extractOvfXmlFromOva(file);
  }
  throw new Error("Choose an .ova or .ovf file.");
}

/**
 * @typedef {{ key: string, type: string, defaultValue: string, userConfigurable: boolean, label: string, description: string }} OvfPropertyField
 * @typedef {{ name: string }} OvfNetworkInfo
 * @typedef {{ properties: OvfPropertyField[], networks: OvfNetworkInfo[], vmNameSuggestion: string }} OvfDescriptorInfo
 */

/**
 * @param {Element} el
 * @param {string} localName
 */
function attrByLocalName(el, localName) {
  for (const a of el.attributes) {
    if (a.localName === localName) return a.value;
  }
  return "";
}

/**
 * @param {Document} doc
 * @param {string} localName
 * @returns {Element[]}
 */
function elementsByLocalName(doc, localName) {
  const out = [];
  const all = doc.getElementsByTagName("*");
  for (let i = 0; i < all.length; i += 1) {
    const el = all[i];
    if (el.localName === localName) out.push(el);
  }
  return out;
}

/**
 * @param {string} xmlString
 * @returns {OvfDescriptorInfo}
 */
export function parseOvfDescriptor(xmlString) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(xmlString, "text/xml");
  const pe = doc.querySelector("parsererror");
  if (pe) {
    throw new Error("Invalid OVF XML (parser error).");
  }

  /** @type {OvfPropertyField[]} */
  const properties = [];
  for (const prop of elementsByLocalName(doc, "Property")) {
    const key = attrByLocalName(prop, "key");
    if (!key) continue;
    const userConfigurable = attrByLocalName(prop, "userConfigurable") !== "false";
    let label = key;
    let description = "";
    for (const child of prop.children) {
      if (child.localName === "Label") label = (child.textContent || "").trim() || key;
      if (child.localName === "Description")
        description = (child.textContent || "").trim() || "";
    }
    properties.push({
      key,
      type: attrByLocalName(prop, "type") || "string",
      defaultValue: attrByLocalName(prop, "value"),
      userConfigurable,
      label,
      description,
    });
  }

  /** @type {OvfNetworkInfo[]} */
  const networks = [];
  for (const net of elementsByLocalName(doc, "Network")) {
    const name = attrByLocalName(net, "name");
    if (name) networks.push({ name });
  }

  let vmNameSuggestion = "";
  const vsList = elementsByLocalName(doc, "VirtualSystem");
  const vs = vsList[0];
  if (vs) {
    vmNameSuggestion = attrByLocalName(vs, "id") || "";
  }

  return { properties, networks, vmNameSuggestion };
}

/**
 * @param {File} file
 * @returns {Promise<OvfDescriptorInfo>}
 */
export async function inspectOvaOrOvfFile(file) {
  const xml = await readOvfXmlFromFile(file);
  return parseOvfDescriptor(xml);
}
