import * as childProcess from 'node:child_process';

import { MCP_SERVER_PACKAGE, SERVER_ENV } from './constants';

import { envWithoutSecrets } from '../../shared/env';

export type ConnectFailure = 'fetch-failed' | 'runtime-unsupported' | 'exited' | 'timeout';

export type ConnectResult =
    | { ok: true; toolCount: number }
    | { ok: false; cause: ConnectFailure; detail: string };

const DEFAULT_TIMEOUT_MS = 60_000;

/** Separating these matters: FR-024c requires naming the causes we can distinguish, and with no
 *  verbose mode the message is all the developer gets. */
function classify(stderr: string): ConnectFailure {
    if (/404|E404|not found|ETARGET|ENOTFOUND|registry/i.test(stderr)) return 'fetch-failed';
    if (/Unsupported engine|requires Node|SyntaxError|Unexpected token/i.test(stderr)) {
        return 'runtime-unsupported';
    }
    return 'exited';
}

/**
 * Launch the configured server and confirm it reports its tools (FR-024a).
 *
 * Verifying the token (FR-008) proves the instance accepts it; it does not prove the developer's
 * machine can run the server. A stale npx cache or an unsupported runtime otherwise produces a
 * green summary and a broken agent.
 *
 * The token reaches the child through its ENVIRONMENT, never argv (FR-022) — this is the one
 * place the design spawns a process holding a secret, so that is load-bearing rather than
 * merely convenient. It is also exactly how the editor will launch it.
 */
export async function confirmConnection(args: {
    url: string;
    token: string;
    timeoutMs?: number;
}): Promise<ConnectResult> {
    const timeoutMs = args.timeoutMs ?? DEFAULT_TIMEOUT_MS;

    const child = childProcess.spawn('npx', ['-y', MCP_SERVER_PACKAGE], {
        stdio: ['pipe', 'pipe', 'pipe'],
        // `npx` is `npx.cmd` on Windows and Node refuses `.cmd` without a shell since the
        // CVE-2024-27980 fix — so FR-024a failed on every Windows run, reporting a broken
        // server for a configuration that was written correctly.
        shell: process.platform === 'win32',
        // This child needs the URL and the token; it has no use for the developer's
        // DOTCMS_PASSWORD, and it is an unpinned `@latest` package by design — so the smaller
        // the environment it sees, the better.
        env: envWithoutSecrets({
            [SERVER_ENV.url]: args.url,
            [SERVER_ENV.token]: args.token
        })
    });

    let stderr = '';
    child.stderr?.on('data', (chunk: Buffer) => {
        stderr += String(chunk);
    });

    return new Promise<ConnectResult>((resolve) => {
        let settled = false;
        const finish = (result: ConnectResult) => {
            if (settled) return;
            settled = true;
            clearTimeout(timer);
            // Close the pipe first: a well-behaved stdio server exits on EOF, which is the
            // clean shutdown. It was never ended at all.
            try {
                child.stdin?.end();
            } catch {
                /* already gone */
            }
            try {
                // `shell: true` on Windows makes the child cmd.exe, with npx -> node beneath it.
                // `child.kill()` reaps the shell only, leaving the MCP server running,
                // unsupervised, holding AUTH_TOKEN. Kill the tree instead.
                if (process.platform === 'win32' && child.pid) {
                    childProcess.spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F']);
                } else {
                    child.kill();
                }
            } catch {
                /* already gone */
            }
            resolve(result);
        };

        const timer = setTimeout(
            () =>
                finish({
                    ok: false,
                    cause: 'timeout',
                    detail: `No response within ${timeoutMs}ms.`
                }),
            timeoutMs
        );

        let buffer = '';
        child.stdout?.on('data', (chunk: Buffer) => {
            buffer += String(chunk);
            // Only completed frames. Re-splitting the whole buffer each chunk re-parsed every
            // line already seen, and re-threw on the trailing partial one every time.
            const frames = buffer.split('\n');
            buffer = frames.pop() ?? '';
            for (const line of frames) {
                if (!line.trim()) continue;
                try {
                    const message = JSON.parse(line) as {
                        id?: number;
                        result?: { tools?: unknown[] };
                    };
                    // MCP requires initialize -> `notifications/initialized` -> anything else.
                    // We used to fire `tools/list` immediately alongside `initialize`, which a
                    // strict server is entitled to reject — so the connection check could fail
                    // against a perfectly good server. Wait for the initialize RESULT first.
                    if (message.id === 1) {
                        child.stdin?.write(
                            `${JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' })}\n`
                        );
                        child.stdin?.write(
                            `${JSON.stringify({ jsonrpc: '2.0', id: 2, method: 'tools/list' })}\n`
                        );
                        continue;
                    }
                    const tools = message.result?.tools;
                    if (Array.isArray(tools)) finish({ ok: true, toolCount: tools.length });
                } catch {
                    /* partial frame; wait for more */
                }
            }
        });

        child.on('error', (error: Error) =>
            finish({ ok: false, cause: 'exited', detail: error.message })
        );
        child.on('exit', (code: number | null) =>
            finish({
                ok: false,
                cause: classify(stderr),
                detail: stderr.trim().split('\n').slice(-1)[0] || `Server exited with code ${code}.`
            })
        );

        // Speak MCP: initialize now; the initialized notification and `tools/list` follow once
        // the server has answered, in the handler above.
        child.stdin?.write(
            `${JSON.stringify({
                jsonrpc: '2.0',
                id: 1,
                method: 'initialize',
                params: {
                    protocolVersion: '2024-11-05',
                    capabilities: {},
                    clientInfo: { name: 'dotcms', version: '0' }
                }
            })}\n`
        );
    });
}
