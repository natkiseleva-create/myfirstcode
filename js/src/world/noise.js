/** Deterministic 2D hash — same coords always give the same value in [0, 1). */
export function randomAt(x, z, seed = 12345) {
  let h = (seed + x * 374761393 + z * 668265263) | 0;
  h = (h ^ (h >> 13)) * 1274126177;
  h = (h ^ (h >> 16)) >>> 0;
  return h / 4294967296;
}

function lerp(a, b, t) {
  return a + (b - a) * t;
}

function smoothstep(t) {
  return t * t * (3 - 2 * t);
}

/** Smooth value noise in world space. */
export function smoothNoise(x, z, seed = 12345) {
  const x0 = Math.floor(x);
  const z0 = Math.floor(z);
  const fx = smoothstep(x - x0);
  const fz = smoothstep(z - z0);

  const v00 = randomAt(x0, z0, seed);
  const v10 = randomAt(x0 + 1, z0, seed);
  const v01 = randomAt(x0, z0 + 1, seed);
  const v11 = randomAt(x0 + 1, z0 + 1, seed);

  const ix0 = lerp(v00, v10, fx);
  const ix1 = lerp(v01, v11, fx);
  return lerp(ix0, ix1, fz);
}

/** Fractal Brownian motion — layered hills. */
export function fbm(x, z, octaves = 5, seed = 12345) {
  let value = 0;
  let amplitude = 1;
  let frequency = 1;
  let maxValue = 0;

  for (let i = 0; i < octaves; i++) {
    value += smoothNoise(x * frequency, z * frequency, seed + i * 97) * amplitude;
    maxValue += amplitude;
    amplitude *= 0.5;
    frequency *= 2.05;
  }

  return value / maxValue;
}
