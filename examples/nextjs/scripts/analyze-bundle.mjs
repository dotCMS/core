#!/usr/bin/env node
/**
 * Reports how much JavaScript a route downloads before any interaction, and can tell you
 * whether a specific component is part of it.
 *
 * Why this exists: `next build` with Turbopack prints no First Load JS column, and
 * `@next/bundle-analyzer` is a webpack plugin that produces nothing on a Turbopack build. So
 * there is no built-in way to see whether your content-type components are actually being
 * loaded on demand.
 *
 * Usage:
 *   npm run build
 *   npm run analyze                              # initial-route totals
 *   npm run analyze -- "pointer-events-none abs" # is that component's code downloaded up front?
 *
 * Search for a string from the component's OUTPUT — a class name, a label — not its name.
 * The component map keys live in the initial chunk by design: `Banner: dynamic(() => …)` is
 * the loader, a few bytes that decide when to fetch the real thing. Grepping for "Banner"
 * finds that loader and tells you nothing.
 */
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import { gzipSync } from "node:zlib";

const NEXT_DIR = join(process.cwd(), ".next");

if (!existsSync(NEXT_DIR)) {
  console.error("No .next directory. Run `npm run build` first.");
  process.exit(1);
}

function walk(dir, match, found = []) {
  if (!existsSync(dir)) return found;
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) walk(full, match, found);
    else if (match(full)) found.push(full);
  }
  return found;
}

/**
 * The chunks the browser loads to render a route, before it interacts with anything:
 * the shared app shell plus every chunk the route's client modules pull in.
 */
function initialChunks() {
  const chunks = new Set();
  const manifest = JSON.parse(readFileSync(join(NEXT_DIR, "build-manifest.json"), "utf-8"));

  for (const file of manifest.rootMainFiles ?? []) chunks.add(join(NEXT_DIR, file));

  for (const file of walk(join(NEXT_DIR, "server"), (f) =>
    f.endsWith("page_client-reference-manifest.js"),
  )) {
    globalThis.__RSC_MANIFEST = {};
    new Function(readFileSync(file, "utf-8"))();
    for (const route of Object.values(globalThis.__RSC_MANIFEST)) {
      for (const entry of Object.values(route.clientModules ?? {})) {
        for (const chunk of entry.chunks ?? []) {
          chunks.add(join(NEXT_DIR, chunk.replace(/^\/_next\//, "")));
        }
      }
    }
  }
  delete globalThis.__RSC_MANIFEST;

  return [...chunks].filter((f) => existsSync(f) && statSync(f).isFile());
}

const initial = initialChunks();
const allClient = walk(join(NEXT_DIR, "static"), (f) => f.endsWith(".js"));
const lazy = allClient.filter((f) => !initial.includes(f));

let raw = 0;
let gzip = 0;
for (const file of initial) {
  const contents = readFileSync(file);
  raw += contents.byteLength;
  gzip += gzipSync(contents).byteLength;
}

const kb = (n) => `${(n / 1024).toFixed(1)} KB`;

console.log(`\nInitial route JavaScript`);
console.log(`  ${initial.length} chunks — ${kb(raw)} raw, ${kb(gzip)} gzip`);
console.log(`  ${lazy.length} more chunks load on demand\n`);

const needle = process.argv[2];

if (!needle) {
  console.log("Pass a string from a component's output to see when it downloads:");
  console.log('  npm run analyze -- "pointer-events-none absolute inset-0"');
  console.log("");
  console.log("Use a class name or label the component renders, not the component's name —");
  console.log("the map keys are in the initial chunk by design, since that is where the");
  console.log("dynamic() loaders live.\n");
  process.exit(0);
}

const inInitial = initial.filter((f) => readFileSync(f, "utf-8").includes(needle));
const inLazy = lazy.filter((f) => readFileSync(f, "utf-8").includes(needle));

if (inInitial.length) {
  console.log(`FOUND in the initial download — ${inInitial.length} chunk(s).`);
  console.log(`Every visitor downloads this, on every route.`);
  console.log(`If it is a content type, map it with next/dynamic.\n`);
} else if (inLazy.length) {
  console.log(`Not in the initial download.`);
  console.log(`It lives in ${inLazy.length} on-demand chunk(s), fetched only when a page needs it.\n`);
} else {
  console.log(`Not found anywhere in the client build.`);
  console.log(`Check the string, or whether the component is server-only.\n`);
}
