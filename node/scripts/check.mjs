// Type-checks every example against the local SDK checkout (no API calls, no npm install).
//
//   SDK_REPOS_DIR=/path/to/sdk node scripts/check.mjs

import { spawnSync } from "node:child_process";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const run = (args) => {
  const r = spawnSync(process.execPath, args, { cwd: here, stdio: "inherit" });
  if (r.status !== 0) process.exit(r.status ?? 1);
};

run([join(here, "scripts", "use-local-sdk.mjs")]);
run([join(here, "node_modules", "typescript", "bin", "tsc"), "-p", join(here, "tsconfig.json")]);
// The webhook self-test needs no API key or network: run it as a smoke test.
run([join(here, "async-webhooks.ts"), "--self-test"]);
console.log("node examples: type-check and webhook self-test passed");
