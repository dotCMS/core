import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

jest.mock('@clack/prompts', () => ({
    select: jest.fn(),
    text: jest.fn(),
    password: jest.fn(),
    isCancel: jest.fn(() => false)
}));

const mockConsola = {
    info: jest.fn(),
    success: jest.fn(),
    error: jest.fn(),
    warn: jest.fn(),
    log: jest.fn()
};
jest.mock('consola', () => ({
    __esModule: true,
    default: mockConsola
}));

jest.mock('../../core/auth', () => ({
    addInstance: jest.fn(),
    authenticateWithCredentials: jest.fn().mockResolvedValue('jwt-token'),
    loadAuth: jest.fn().mockReturnValue({}),
    removeInstance: jest.fn(),
    saveAuth: jest.fn()
}));

jest.mock('../../core/config', () => ({
    loadConfig: jest.fn(),
    saveConfig: jest.fn()
}));

import { addInstance, loadAuth, removeInstance } from '../../core/auth';
import { loadConfig, saveConfig } from '../../core/config';
import { authCommand } from '../auth';

import type { DotCliConfig } from '../../core/types';

const consola = mockConsola;

const subCommands = authCommand.subCommands!;

describe('auth command', () => {
    let tmpDir: string;
    let originalCwd: string;

    const seedConfig: DotCliConfig = {
        default: 'demo',
        instances: {
            demo: { url: 'https://demo.dotcms.com' },
            local: { url: 'http://localhost:8080' }
        }
    };

    beforeEach(() => {
        tmpDir = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-auth-cmd-')));
        originalCwd = process.cwd();
        process.chdir(tmpDir);
        jest.clearAllMocks();
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    describe('add', () => {
        it('should call addInstance with token from args', async () => {
            const run = (subCommands as Record<string, { run: (ctx: unknown) => Promise<void> }>)[
                'add'
            ].run;

            await run({
                args: {
                    name: 'staging',
                    url: 'https://staging.dotcms.com',
                    token: 'my-token'
                }
            });

            expect(addInstance).toHaveBeenCalledWith(
                tmpDir,
                'staging',
                'https://staging.dotcms.com',
                'my-token'
            );
        });

        it('should reject invalid URL', async () => {
            const run = (subCommands as Record<string, { run: (ctx: unknown) => Promise<void> }>)[
                'add'
            ].run;

            await run({
                args: {
                    name: 'bad',
                    url: 'not-a-url',
                    token: 'tok'
                }
            });

            expect(addInstance).not.toHaveBeenCalled();
            expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('Invalid URL'));
        });
    });

    describe('list', () => {
        it('should display instances when config exists', async () => {
            (loadConfig as jest.Mock).mockReturnValue(seedConfig);
            (loadAuth as jest.Mock).mockReturnValue({
                demo: { type: 'token', token: 'tok' }
            });

            const run = (subCommands as Record<string, { run: () => Promise<void> }>)['list'].run;
            await run();

            expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('demo'));
            expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('default'));
            expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('authenticated'));
        });

        it('should show error when no project found', async () => {
            (loadConfig as jest.Mock).mockImplementation(() => {
                throw new Error('Config file not found');
            });

            const run = (subCommands as Record<string, { run: () => Promise<void> }>)['list'].run;
            await run();

            expect(consola.error).toHaveBeenCalledWith(
                expect.stringContaining('No dotcli project')
            );
        });

        it('should inform when no instances configured', async () => {
            (loadConfig as jest.Mock).mockReturnValue({
                default: '',
                instances: {}
            });

            const run = (subCommands as Record<string, { run: () => Promise<void> }>)['list'].run;
            await run();

            expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No instances'));
        });
    });

    describe('default', () => {
        it('should update default instance', async () => {
            (loadConfig as jest.Mock).mockReturnValue({ ...seedConfig });

            const run = (subCommands as Record<string, { run: (ctx: unknown) => Promise<void> }>)[
                'default'
            ].run;
            await run({ args: { name: 'local' } });

            expect(saveConfig).toHaveBeenCalledWith(
                tmpDir,
                expect.objectContaining({ default: 'local' })
            );
        });

        it('should error when instance not found', async () => {
            (loadConfig as jest.Mock).mockReturnValue({ ...seedConfig });

            const run = (subCommands as Record<string, { run: (ctx: unknown) => Promise<void> }>)[
                'default'
            ].run;
            await run({ args: { name: 'nonexistent' } });

            expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('not found'));
        });
    });

    describe('remove', () => {
        it('should call removeInstance', async () => {
            const run = (subCommands as Record<string, { run: (ctx: unknown) => Promise<void> }>)[
                'remove'
            ].run;
            await run({ args: { name: 'demo' } });

            expect(removeInstance).toHaveBeenCalledWith(tmpDir, 'demo');
        });

        it('should display error when removeInstance throws', async () => {
            (removeInstance as jest.Mock).mockImplementation(() => {
                throw new Error('Instance "nope" not found');
            });

            const run = (subCommands as Record<string, { run: (ctx: unknown) => Promise<void> }>)[
                'remove'
            ].run;
            await run({ args: { name: 'nope' } });

            expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('not found'));
        });
    });
});
