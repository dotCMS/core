// Builds the Amplify Hosting deployment bundle (.amplify-hosting/) from the
// Angular SSR output. Spec: https://docs.aws.amazon.com/amplify/latest/userguide/ssr-deployment-specification.html
import {
  cpSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const dist = join(root, 'dist', 'angular-ssr-amplify');
const out = join(root, '.amplify-hosting');
const compute = join(out, 'compute', 'default');

if (!existsSync(join(dist, 'server', 'server.mjs'))) {
  console.error('dist/angular-ssr-amplify/server/server.mjs not found. Run `ng build` first.');
  process.exit(1);
}

rmSync(out, { recursive: true, force: true });
mkdirSync(compute, { recursive: true });

// Static assets: hashed JS/CSS, public/ files. In outputMode "server" Angular emits
// index.csr.html (not index.html), so the SSR shell is never served as a static file.
cpSync(join(dist, 'browser'), join(out, 'static'), { recursive: true });

// Compute: Angular SSR server bundle + entry that listens on :3000.
cpSync(join(dist, 'server'), join(compute, 'server'), { recursive: true });
cpSync(join(root, 'amplify', 'compute-entry.cjs'), join(compute, 'compute-entry.cjs'));

// Runtime env: the compute does not receive the Amplify console variables, so
// bundle the .env that amplify.yml wrote during the build (if there is one).
if (existsSync(join(root, '.env'))) {
  cpSync(join(root, '.env'), join(compute, '.env'));
}

// Deploy manifest, stamped with the Angular version that was actually built.
const manifest = JSON.parse(readFileSync(join(root, 'amplify', 'deploy-manifest.json'), 'utf8'));
const angular = JSON.parse(
  readFileSync(join(root, 'node_modules', '@angular', 'core', 'package.json'), 'utf8')
);
manifest.framework.version = angular.version;
writeFileSync(join(out, 'deploy-manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);

const size = (dir) =>
  readdirSync(dir, { withFileTypes: true }).reduce(
    (sum, e) => sum + (e.isDirectory() ? size(join(dir, e.name)) : statSync(join(dir, e.name)).size),
    0
  );
console.log(`.amplify-hosting ready. compute: ${(size(compute) / 1048576).toFixed(1)} MB (limit 220 MB)`);
