import * as childProcess from 'node:child_process';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';

import { confirmConnection } from './connect';

/** See registry.spec.ts — the `node:child_process` namespace is non-configurable under
 *  ts-jest, so `jest.spyOn` on it throws. Replace the one function via a module factory. */
jest.mock('node:child_process', () => ({
    ...jest.requireActual('node:child_process'),
    spawn: jest.fn()
}));

function fakeChild() {
    const child = new EventEmitter() as EventEmitter & Record<string, unknown>;
    child['stdin'] = new PassThrough();
    child['stdout'] = new PassThrough();
    child['stderr'] = new PassThrough();
    child['kill'] = jest.fn();
    return child;
}

describe('confirmConnection (FR-024a-e)', () => {
    let child: ReturnType<typeof fakeChild>;
    const spawn = childProcess.spawn as unknown as jest.Mock;

    beforeEach(() => {
        child = fakeChild();
        spawn.mockReturnValue(child as never);
    });

    afterEach(() => { jest.clearAllMocks(); });

    it('passes the token through the child ENVIRONMENT, never argv (FR-022)', async () => {
        void confirmConnection({ url: 'https://demo.dotcms.com', token: 'dot_secret_9999', timeoutMs: 50 });
        const [, args, opts] = spawn.mock.calls[0] as [string, string[], { env?: NodeJS.ProcessEnv }];
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
