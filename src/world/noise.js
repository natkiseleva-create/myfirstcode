/** Deterministic 2D hash — same coords always give the same value in [0, 1). */
export function randomAt(x, z, seed = 12345) {
  let h = (seed + x * 374761393 + z * 668265263) | 0;
  h = (h ^ (h >> 13)) * 1274126177;
  h = (h ^ (h >> 16)) >>> 0;
  return h / 4294967296;
}
