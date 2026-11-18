import * as childProcess from 'node:child_process';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';

import { confirmConnection } from './connect';

import type { Mock } from 'vitest';

/** See registry.spec.ts — the `node:child_process` namespace is non-configurable under
 *  an ES module, so `vi.spyOn` on it throws. Replace the one function via a module factory. */
vi.mock('node:child_process', async (importOriginal) => ({
    ...(await importOriginal<typeof childProcess>()),
    spawn: vi.fn(),
    spawnSync: vi.fn(() => ({ status: 0 }))
}));

function fakeChild() {
    const child = new EventEmitter() as EventEmitter & Record<string, unknown>;
    child['stdin'] = new PassThrough();
    child['stdout'] = new PassThrough();
    child['stderr'] = new PassThrough();
    child['kill'] = vi.fn();
    return child;
}

describe('confirmConnection (FR-024a-e)', () => {
    let child: ReturnType<typeof fakeChild>;
    const spawn = childProcess.spawn as unknown as Mock;

    beforeEach(() => {
        child = fakeChild();
        spawn.mockReturnValue(child as never);
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('passes the token through the child ENVIRONMENT, never argv (FR-022)', async () => {
        void confirmConnection({
            url: 'https://demo.dotcms.com',
            token: 'dot_secret_9999',
            timeoutMs: 50
        });
        const [, args, opts] = spawn.mock.calls[0] as [
            string,
            string[],
            { env?: NodeJS.ProcessEnv }
        ];
        expect(JSON.stringify(args)).not.toContain('dot_secret_9999');
        expect(opts?.env?.['AUTH_TOKEN']).toBe('dot_secret_9999');
    });

    it('launches the same command that was written to the config', async () => {
        void confirmConnection({ url: 'https://demo.dotcms.com', token: 't', timeoutMs: 50 });
        const [cmd, args] = spawn.mock.calls[0] as [string, string[]];
        expect(cmd).toBe('npx');
        expect(args).toEqual(expect.arrayContaining(['@dotcms/mcp-server@latest']));
    });

    it('treats a timeout as "did not respond" rather than hanging', async () => {
        const result = await confirmConnection({ url: 'u', token: 't', timeoutMs: 20 });
        expect(result.ok).toBe(false);
        if (!result.ok) expect(result.cause).toBe('timeout');
    });

    it('kills the child process even when the exchange fails', async () => {
        await confirmConnection({ url: 'u', token: 't', timeoutMs: 20 });
        expect(child['kill']).toHaveBeenCalled();
    });

    it('distinguishes a package that could not be fetched from a server that exited (FR-024c)', async () => {
        const p = confirmConnection({ url: 'u', token: 't', timeoutMs: 500 });
        (child['stderr'] as PassThrough).write('npm ERR! 404 Not Found - GET @dotcms/mcp-server\n');
        child.emit('exit', 1);
        const result = await p;
        expect(result.ok).toBe(false);
        if (!result.ok) expect(result.cause).toBe('fetch-failed');
    });
});

describe('the MCP handshake is sequenced, not fired all at once (FR-024a)', () => {
    /**
     * `tools/list` used to go out in the same tick as `initialize`. MCP requires
     * initialize -> `notifications/initialized` -> everything else, so a strict server may
     * reject that request and the connection check would report a broken server for a
     * configuration that is perfectly good.
     *
     * Nothing here caught it: no existing test drove the server SIDE of the conversation.
     */
    function sent(child: ReturnType<typeof fakeChild>): Promise<string[]> {
        const frames: string[] = [];
        (child['stdin'] as NodeJS.WritableStream).on('data', (c: Buffer) => {
            for (const line of String(c).split('\n')) if (line.trim()) frames.push(line);
        });
        return Promise.resolve(frames);
    }

    it('sends initialize alone, then initialized + tools/list only after the server answers', async () => {
        const child = fakeChild();
        (childProcess.spawn as unknown as Mock).mockReturnValue(child as never);
        const frames = await sent(child);

        const run = confirmConnection({
            url: 'https://demo.dotcms.com',
            token: 't',
            timeoutMs: 500
        });
        await new Promise((r) => setImmediate(r));

        const first = frames.map((f) => JSON.parse(f).method);
        expect(first).toEqual(['initialize']);

        // The server answers the initialize.
        (child['stdout'] as NodeJS.ReadableStream).emit(
            'data',
            Buffer.from(`${JSON.stringify({ jsonrpc: '2.0', id: 1, result: {} })}\n`)
        );
        await new Promise((r) => setImmediate(r));

        expect(frames.map((f) => JSON.parse(f).method)).toEqual([
            'initialize',
            'notifications/initialized',
            'tools/list'
        ]);

        (child['stdout'] as NodeJS.ReadableStream).emit(
            'data',
            Buffer.from(
                `${JSON.stringify({ jsonrpc: '2.0', id: 2, result: { tools: [{ name: 'x' }] } })}\n`
            )
        );
        await expect(run).resolves.toMatchObject({ ok: true });
    });

    it('the initialized notification carries no id — it is a notification', async () => {
        const child = fakeChild();
        (childProcess.spawn as unknown as Mock).mockReturnValue(child as never);
        const frames = await sent(child);
        void confirmConnection({ url: 'https://demo.dotcms.com', token: 't', timeoutMs: 200 });
        await new Promise((r) => setImmediate(r));
        (child['stdout'] as NodeJS.ReadableStream).emit(
            'data',
            Buffer.from(`${JSON.stringify({ jsonrpc: '2.0', id: 1, result: {} })}\n`)
        );
        await new Promise((r) => setImmediate(r));
        const note = frames
            .map((f) => JSON.parse(f))
            .find((m) => m.method === 'notifications/initialized');
        expect(note).toBeDefined();
        expect(note.id).toBeUndefined();
    });
});

describe('Windows needs a shell to run npx (FR-024a, FR-025)', () => {
    /**
     * `npx` on Windows is `npx.cmd`, and since the CVE-2024-27980 fix Node refuses to execute
     * `.cmd`/`.bat` without a shell. Windows is in scope (see `CAN_RESTRICT`, the `%APPDATA%`
     * path in the registry, research R5), so without this the connection check failed on EVERY
     * Windows run — after the configuration had been written correctly.
     *
     * Asserted by faking the platform, because CI here is not Windows and an untested claim
     * about another OS is worth very little.
     */
    const real = process.platform;
    const asPlatform = (value: string) =>
        Object.defineProperty(process, 'platform', { value, configurable: true });
    afterEach(() => asPlatform(real));

    it('passes shell: true on win32', () => {
        asPlatform('win32');
        void confirmConnection({ url: 'https://demo.dotcms.com', token: 't', timeoutMs: 20 });
        const [, , opts] = (childProcess.spawn as unknown as Mock).mock.calls.at(-1) as [
            string,
            string[],
            { shell?: boolean }
        ];
        expect(opts.shell).toBe(true);
    });

    it('does not on posix, where a shell would only add an interpreter', () => {
        asPlatform('darwin');
        void confirmConnection({ url: 'https://demo.dotcms.com', token: 't', timeoutMs: 20 });
        const [, , opts] = (childProcess.spawn as unknown as Mock).mock.calls.at(-1) as [
            string,
            string[],
            { shell?: boolean }
        ];
        expect(opts.shell).toBe(false);
    });
});

describe('the server child gets what it needs and nothing more', () => {
    it('passes the URL and token but not DOTCMS_PASSWORD', () => {
        const OLD = process.env;
        process.env = { ...OLD, DOTCMS_PASSWORD: 'hunter2', DOTCMS_AUTH_TOKEN: 'dot_env' };
        try {
            void confirmConnection({ url: 'https://demo.dotcms.com', token: 't', timeoutMs: 20 });
            const [, , opts] = (childProcess.spawn as unknown as Mock).mock.calls.at(-1) as [
                string,
                string[],
                { env: NodeJS.ProcessEnv }
            ];
            expect(opts.env['AUTH_TOKEN']).toBe('t');
            expect(opts.env['DOTCMS_URL']).toBe('https://demo.dotcms.com');
            expect(opts.env['DOTCMS_PASSWORD']).toBeUndefined();
        } finally {
            process.env = OLD;
        }
    });
});

describe('killing the server on Windows kills the tree, not just the shell', () => {
    /**
     * `shell: true` makes the child cmd.exe with npx -> node beneath it, so `child.kill()`
     * reaps the shell and leaves the MCP server running — unsupervised, holding AUTH_TOKEN.
     */
    const real = process.platform;
    const asPlatform = (value: string) =>
        Object.defineProperty(process, 'platform', { value, configurable: true });
    afterEach(() => asPlatform(real));

    it('uses taskkill /T on win32', async () => {
        asPlatform('win32');
        const child = fakeChild();
        (childProcess.spawn as unknown as Mock).mockReturnValue(child as never);
        child['pid'] = 4321;
        const sync = childProcess.spawnSync as unknown as Mock;
        sync.mockReturnValue({ status: 0 });

        const run = confirmConnection({ url: 'https://x', token: 't', timeoutMs: 20 });
        await run;

        const call = sync.mock.calls.find((c) => c[0] === 'taskkill');
        expect(call).toBeDefined();
        expect(call?.[1]).toEqual(expect.arrayContaining(['/T', '/F', '4321']));
    });

    it('closes stdin so a well-behaved server can exit on its own', async () => {
        asPlatform('darwin');
        const child = fakeChild();
        (childProcess.spawn as unknown as Mock).mockReturnValue(child as never);
        const ended = vi.spyOn(child['stdin'] as NodeJS.WritableStream, 'end');
        await confirmConnection({ url: 'https://x', token: 't', timeoutMs: 20 });
        expect(ended).toHaveBeenCalled();
    });
});
