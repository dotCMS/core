import { execSync, spawn } from 'node:child_process';
import { existsSync, readdirSync } from 'node:fs';
import { basename, join, sep } from 'node:path';

/**
 * Boots the BUILT artifact and speaks MCP to it over stdio.
 *
 * Every other spec in this app tests source modules, which is precisely what missed
 * issue #37337: `upload_assets.spec.ts` sat inside the xmcp tools path, so the bundle
 * executed it as a tool and the server died with `ReferenceError: describe is not defined`
 * before reading any configuration — for every user, on every invocation. Unit tests all
 * passed. The only thing that catches a crash-on-boot is starting the thing that ships.
 *
 * The `test` target declares `dependsOn: ["build"]`, so the bundle under test is always the
 * one this commit produces.
 */
const DIST = join(__dirname, '..', '..', '..', '..', 'dist', 'apps', 'mcp-server');
const SERVER = join(DIST, 'stdio.js');

const STDERR_TAIL_CHARS = 1_500;

const tail = (text: string) =>
    text.length > STDERR_TAIL_CHARS ? `…${text.slice(-STDERR_TAIL_CHARS)}` : text;

interface JsonRpcResponse {
    jsonrpc: string;
    id?: number;
    result?: { tools?: { name: string }[]; [key: string]: unknown };
    error?: { code: number; message: string };
}

/**
 * Writes the given JSON-RPC messages to the server's stdin and resolves once `expected`
 * responses have come back — or when the process exits, whichever happens first, so a
 * crash-on-boot surfaces as its real stderr rather than as a timeout.
 */
const speak = (
    messages: unknown[],
    expected: number
): Promise<{ responses: JsonRpcResponse[]; stderr: string; exitCode: number | null }> =>
    new Promise((resolve, reject) => {
        const child = spawn(process.execPath, [SERVER], {
            stdio: ['pipe', 'pipe', 'pipe'],
            // A boot failure must not be masked by a configured instance, and must not reach
            // out to one either: the server reads credentials lazily, inside tool handlers.
            // DEBUG is dropped so a developer's shell can't put chatter on stderr, which the
            // assertions below require to be empty.
            env: { ...process.env, DOTCMS_URL: '', AUTH_TOKEN: '', DEBUG: '' }
        });

        const responses: JsonRpcResponse[] = [];
        let stdout = '';
        let stderr = '';
        let settled = false;

        const finish = (exitCode: number | null) => {
            if (settled) return;
            settled = true;
            child.kill('SIGKILL');
            // A crash inside the bundle makes node echo the whole minified chunk it faulted on
            // — hundreds of KB that bury the trace they precede, and the assertion diff below
            // would print all of it. The trailing stack is the part that names the culprit.
            resolve({ responses, stderr: tail(stderr), exitCode });
        };

        child.stdout.on('data', (chunk: Buffer) => {
            stdout += chunk.toString();
            const lines = stdout.split('\n');
            // The trailing element is either '' or a partial line still being written.
            stdout = lines.pop() ?? '';

            for (const line of lines) {
                if (!line.trim()) continue;
                responses.push(JSON.parse(line) as JsonRpcResponse);
            }

            if (responses.length >= expected) finish(null);
        });

        child.stderr.on('data', (chunk: Buffer) => {
            stderr += chunk.toString();
        });

        child.on('error', reject);
        child.on('exit', (code) => finish(code));

        child.stdin.write(messages.map((message) => `${JSON.stringify(message)}\n`).join(''));
    });

describe('mcp-server boot smoke test', () => {
    // Spawning node and loading the bundle is slower than the 5s jest default.
    jest.setTimeout(30_000);

    it('has a built artifact to test', () => {
        expect(existsSync(SERVER)).toBe(true);
    });

    it('answers initialize and tools/list without crashing', async () => {
        const { responses, stderr, exitCode } = await speak(
            [
                {
                    jsonrpc: '2.0',
                    id: 1,
                    method: 'initialize',
                    params: {
                        protocolVersion: '2025-06-18',
                        capabilities: {},
                        clientInfo: { name: 'boot-smoke-test', version: '0' }
                    }
                },
                { jsonrpc: '2.0', method: 'notifications/initialized' },
                { jsonrpc: '2.0', id: 2, method: 'tools/list' }
            ],
            2
        );

        // Surfaced first: when the bundle is broken this is the only useful output there is,
        // and the assertions below would otherwise just report "undefined".
        expect({ exitCode, stderr }).toEqual({ exitCode: null, stderr: '' });

        const initialize = responses.find((response) => response.id === 1);
        expect(initialize?.error).toBeUndefined();
        expect(initialize?.result?.protocolVersion).toBeDefined();

        const list = responses.find((response) => response.id === 2);
        expect(list?.error).toBeUndefined();

        // Every module under src/tools/ is loaded as a tool, so this is also what catches a
        // non-tool file being bundled in — it would show up here as an extra entry.
        const tools = (list?.result?.tools ?? []).map((tool) => tool.name).sort();
        expect(tools).toEqual([
            'download_assets',
            'execute',
            'page_create',
            'page_place_content',
            'page_verify',
            'search',
            'upload_assets'
        ]);
    });

    // The test above boots dist/ in place, which never consults package.json's `files`
    // allowlist — so it would happily pass while the published tarball was missing half the
    // bundle. That is #37337 one level up: build output fine, shipped artifact broken, nothing
    // notices until a user runs it. `files` is `["*.js", "README.md"]`, a non-recursive glob
    // over names the build generates (403.js, 736.js — they change build to build, so an
    // explicit list is not possible here). If xmcp ever emits a chunk into a subdirectory, that
    // glob silently drops it.
    it('packs every file the bundle needs', () => {
        // --dry-run --json reports the tarball manifest without writing one, so this needs no
        // temp directory and no tar — it runs the same everywhere the build does.
        const [manifest] = JSON.parse(
            execSync('npm pack --dry-run --json', { cwd: DIST, encoding: 'utf-8' })
        ) as { files: { path: string }[] }[];

        const packed = new Set(manifest.files.map((file) => file.path));

        // RECURSIVE, deliberately. A non-recursive listing would share the very blind spot
        // this test exists to cover: a chunk emitted into a subdirectory is missed by the
        // `*.js` glob AND by a flat readdir, so the comparison would pass vacuously while the
        // tarball shipped without it. Walking the tree means a nested chunk shows up here as
        // built-but-not-packed, which is the failure we want.
        const built = readdirSync(DIST, { recursive: true, encoding: 'utf-8' })
            .map((file) => file.split(sep).join('/'))
            .filter((file) => file.endsWith('.js'));

        // Guards against the empty-set trap: a build that emitted nothing would otherwise
        // satisfy "every emitted chunk is packed" vacuously.
        expect(built).toContain(basename(SERVER));
        expect(built.filter((file) => !packed.has(file))).toEqual([]);

        // The entry point and the docs npm shows on the package page.
        expect(packed).toContain('package.json');
        expect(packed).toContain('README.md');
    });
});
