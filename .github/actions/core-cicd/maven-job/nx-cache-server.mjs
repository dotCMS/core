// Local Nx remote cache for CI, backed by a directory that actions/cache carries between runs.
//
// Why a server: Nx 21+ indexes its local cache (.nx/cache) in a SQLite file named after the
// machine id (.nx/workspace-data/<machine-id>-v3.db). Every GitHub runner is a new machine, so a
// restored .nx/cache is reported as "unrecognized cache artifacts" and never used. Nx's
// self-hosted remote cache protocol has no such tie: Nx asks for an artifact by task hash and
// unpacks whatever comes back. This serves that protocol from disk.
//
// Nx's own answer for CI-provider caches was @nx/shared-fs-cache, which is commercially licensed
// and now deprecated (CVE-2025-36852, CREEP). Its deprecation notice points to this protocol:
// https://nx.dev/docs/kb/self-hosted-caching#build-your-own-caching-server
// Nx reads the server URL from NX_SELF_HOSTED_REMOTE_CACHE_SERVER.
//   GET /v1/cache/<hash>  200 with the artifact, or 404
//   PUT /v1/cache/<hash>  200 when stored, 409 when the hash is already stored. Never
//                         overwriting is what the spec requires against cache poisoning.
//
// Usage: node nx-cache-server.mjs <port> <dir> [<restored-dir>]
// Artifacts found in <restored-dir> are hard-linked into <dir> when Nx asks for them, so <dir>
// ends up holding exactly the artifacts this run used or built. Saving <dir> keeps the cache at
// one build's worth instead of growing with every run.
import { createReadStream, createWriteStream, existsSync, linkSync, mkdirSync, renameSync } from 'node:fs';
import { createServer } from 'node:http';
import { join } from 'node:path';
import { pipeline } from 'node:stream/promises';

const [port, dir, restoredDir] = process.argv.slice(2);
mkdirSync(dir, { recursive: true });

// Nx task hashes are digits; anything else is refused, which also rules out path traversal.
const CACHE_PATH = /^\/v1\/cache\/(\w+)$/;

const server = createServer(async (req, res) => {
    const hash = CACHE_PATH.exec(req.url ?? '')?.[1];
    if (!hash) {
        res.writeHead(404).end();
        return;
    }

    const file = join(dir, hash);

    if (req.method === 'GET') {
        const restored = restoredDir && join(restoredDir, hash);
        if (!existsSync(file) && restored && existsSync(restored)) {
            try {
                linkSync(restored, file);
            } catch (e) {
                // Two requests for the same hash can race here; the other one linked it.
                if (e.code !== 'EEXIST') throw e;
            }
        }
        if (!existsSync(file)) {
            res.writeHead(404).end();
            return;
        }
        res.writeHead(200, { 'Content-Type': 'application/octet-stream' });
        createReadStream(file).pipe(res);
        return;
    }

    if (req.method === 'PUT') {
        if (existsSync(file)) {
            req.resume();
            res.writeHead(409).end();
            return;
        }
        // Write then rename, so a reader never sees a half-written artifact.
        const tmp = `${file}.${process.pid}.${Date.now()}.tmp`;
        try {
            await pipeline(req, createWriteStream(tmp));
            renameSync(tmp, file);
            res.writeHead(200).end();
        } catch (e) {
            console.error(`nx-cache-server: storing ${hash} failed: ${e.message}`);
            res.writeHead(500).end();
        }
        return;
    }

    res.writeHead(405).end();
});

// A failed request must not take the server down: Nx would then miss the cache for every task
// after it, and the build would only get slower, never fail.
server.on('clientError', (e, socket) => socket.destroy());
process.on('uncaughtException', (e) => console.error(`nx-cache-server: ${e.stack}`));

server.listen(Number(port), '127.0.0.1', () =>
    console.log(`nx-cache-server: listening on 127.0.0.1:${port}, storing in ${dir}`)
);
