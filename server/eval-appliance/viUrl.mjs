/**
 * Build ovftool / OVF Tool "vi://" destination URL.
 * @see https://developer.broadcom.com/s/topic/0TO4R000000XX6HWAW/ovf-tool-documentation
 */
export function buildViDestinationUrl({ vcHost, vcUsername, vcPassword, datacenter, computePath }) {
  const rawHost = String(vcHost || "")
    .trim()
    .replace(/^https?:\/\//i, "")
    .split("/")[0];
  if (!rawHost) {
    throw new Error("vCenter hostname is empty.");
  }
  const dc = String(datacenter || "").trim();
  if (!dc) {
    throw new Error("Datacenter name is required.");
  }
  const rel = String(computePath || "")
    .trim()
    .replace(/^\/+/, "")
    .replace(/\/+$/, "");
  if (!rel) {
    throw new Error("Compute path is required (e.g. host/ClusterName).");
  }
  const u = encodeURIComponent(vcUsername);
  const p = encodeURIComponent(vcPassword);
  return `vi://${u}:${p}@${rawHost}/${dc}/${rel}`;
}
