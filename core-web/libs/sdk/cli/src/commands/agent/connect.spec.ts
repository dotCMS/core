import * as childProcess from 'node:child_process';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';

import { confirmConnection } from './connect';

function fakeChild() {
    const child = new EventEmitter() as EventEmitter & Record<string, unknown>;
    child['stdin'] = new PassThrough();
    child['stdout'] = new PassThrough();
    child['stderr'] = new PassThrough();
    child['kill'] = jest.fn();
    return child;
}

describe('confirmConnection (FR-024a-e)', () => {
    afterEach(() => { jest.restoreAllMocks(); });

    it('passes the token through the child ENVIRONMENT, never argv (FR-022)', async () => {
        const child = fakeChild();
        const spawn = jest.spyOn(childProcess, 'spawn').mockReturnValue(child as never);
        void confirmConnection({ url: 'https://demo.dotcms.com', token: 'dot_secret_9999', timeoutMs: 50 });
        const [, args, opts] = spawn.mock.calls[0] as [string, string[], { env?: NodeJS.ProcessEnv }];
        expect(JSON.stringify(args)).not.toContain('dot_secret_9999');
        expect(opts?.env?.['AUTH_TOKEN']).toBe('dot_secret_9999');
    });

    it('launches the same command that was written to the config', async () => {
        const child = fakeChild();
        const spawn = jest.spyOn(childProcess, 'spawn').mockReturnValue(child as never);
        void confirmConnection({ url: 'https://demo.dotcms.com', token: 't', timeoutMs: 50 });
        const [cmd, args] = spawn.mock.calls[0] as [string, string[]];
        expect(cmd).toBe('npx');
        expect(args).toEqual(expect.arrayContaining(['@dotcms/mcp-server@latest']));
    });

    it('treats a timeout as "did not respond" rather than hanging', async () => {
        const child = fakeChild();
        jest.spyOn(childProcess, 'spawn').mockReturnValue(child as never);
        const result = await confirmConnection({ url: 'u', token: 't', timeoutMs: 20 });
        expect(result.ok).toBe(false);
        if (!result.ok) expect(result.cause).toBe('timeout');
    });

    it('kills the child process even when the exchange fails', async () => {
        const child = fakeChild();
        jest.spyOn(childProcess, 'spawn').mockReturnValue(child as never);
        await confirmConnection({ url: 'u', token: 't', timeoutMs: 20 });
        expect(child['kill']).toHaveBeenCalled();
    });

    it('distinguishes a package that could not be fetched from a server that exited (FR-024c)', async () => {
        const child = fakeChild();
        jest.spyOn(childProcess, 'spawn').mockReturnValue(child as never);
        const p = confirmConnection({ url: 'u', token: 't', timeoutMs: 500 });
        (child['stderr'] as PassThrough).write('npm ERR! 404 Not Found - GET @dotcms/mcp-server\n');
        child.emit('exit', 1);
        const result = await p;
        expect(result.ok).toBe(false);
        if (!result.ok) expect(result.cause).toBe('fetch-failed');
    });
});
