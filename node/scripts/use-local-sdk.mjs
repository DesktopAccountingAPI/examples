// Points node_modules/@desktopaccountingapi/quickbooks-desktop at a local checkout of the SDK instead of the npm package.
//
//   SDK_REPOS_DIR=/path/to/sdk node scripts/use-local-sdk.mjs
//
// SDK_REPOS_DIR defaults to <examples root>/sdk. The SDK must be built (`npm ci` in its folder
// builds dist/). Also links the SDK's TypeScript and @types/node when they are not installed
// here, so type-checking needs no `npm install`. Run `npm install` to go back to the npm package.

import { existsSync, lstatSync, mkdirSync, rmSync, symlinkSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const reposDir = resolve(process.env["SDK_REPOS_DIR"] ?? join(here, "..", "sdk"));
const sdk = join(reposDir, "quickbooks-desktop-node");

if (!existsSync(join(sdk, "package.json"))) {
  console.error(`No SDK checkout at ${sdk}. Clone DesktopAccountingAPI/quickbooks-desktop-node there or set SDK_REPOS_DIR.`);
  process.exit(1);
}
if (!existsSync(join(sdk, "dist", "esm", "index.js"))) {
  console.error(`The SDK at ${sdk} is not built. Run: (cd ${sdk} && npm ci)`);
  process.exit(1);
}

function link(target, path) {
  mkdirSync(dirname(path), { recursive: true });
  if (existsSync(path) || isLink(path)) rmSync(path, { recursive: true, force: true });
  symlinkSync(target, path, process.platform === "win32" ? "junction" : "dir");
  console.log(`${path} -> ${target}`);
}

function isLink(path) {
  try {
    return lstatSync(path).isSymbolicLink();
  } catch {
    return false;
  }
}

link(sdk, join(here, "node_modules", ...("@desktopaccountingapi/quickbooks-desktop".split("/"))));
for (const dep of ["typescript", join("@types", "node")]) {
  const local = join(here, "node_modules", dep);
  const fromSdk = join(sdk, "node_modules", dep);
  if (!existsSync(local) && existsSync(fromSdk)) link(fromSdk, local);
}
