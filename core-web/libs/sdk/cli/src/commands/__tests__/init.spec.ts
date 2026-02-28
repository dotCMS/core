import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

jest.mock('@clack/prompts', () => ({
    intro: jest.fn(),
    outro: jest.fn(),
    text: jest.fn(),
    select: jest.fn(),
    confirm: jest.fn(),
    password: jest.fn(),
    spinner: jest.fn(() => ({ start: jest.fn(), stop: jest.fn() })),
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
    saveAuth: jest.fn()
}));

jest.mock('../../core/config', () => ({
    saveConfig: jest.fn()
}));

// eslint-disable-next-line import/order
import * as prompts from '@clack/prompts';

import { addInstance } from '../../core/auth';
import { saveConfig } from '../../core/config';
import { initCommand } from '../init';

describe('init command', () => {
    let tmpDir: string;
    let originalCwd: string;

    beforeEach(() => {
        tmpDir = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-init-')));
        originalCwd = process.cwd();
        process.chdir(tmpDir);
        jest.clearAllMocks();
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it('should create .dotcli directory structure', async () => {
        (prompts.text as jest.Mock)
            .mockResolvedValueOnce('https://demo.dotcms.com') // server URL
            .mockResolvedValueOnce('demo'); // instance name
        (prompts.select as jest.Mock).mockResolvedValueOnce('skip'); // auth method

        await initCommand.run!({} as never);

        expect(fs.existsSync(path.join(tmpDir, '.dotcli'))).toBe(true);
        expect(fs.existsSync(path.join(tmpDir, '.dotcli', 'cache'))).toBe(true);
    });

    it('should create .dotcliignore with defaults', async () => {
        (prompts.text as jest.Mock)
            .mockResolvedValueOnce('https://demo.dotcms.com')
            .mockResolvedValueOnce('demo');
        (prompts.select as jest.Mock).mockResolvedValueOnce('skip');

        await initCommand.run!({} as never);

        const ignorePath = path.join(tmpDir, '.dotcliignore');
        expect(fs.existsSync(ignorePath)).toBe(true);
        const content = fs.readFileSync(ignorePath, 'utf-8');
        expect(content).toContain('**/assets/');
    });

    it('should append to .gitignore', async () => {
        // Create existing .gitignore
        fs.writeFileSync(path.join(tmpDir, '.gitignore'), 'node_modules/\n', 'utf-8');

        (prompts.text as jest.Mock)
            .mockResolvedValueOnce('https://demo.dotcms.com')
            .mockResolvedValueOnce('demo');
        (prompts.select as jest.Mock).mockResolvedValueOnce('skip');

        await initCommand.run!({} as never);

        const gitignore = fs.readFileSync(path.join(tmpDir, '.gitignore'), 'utf-8');
        expect(gitignore).toContain('.dotcli/.auth.json');
        expect(gitignore).toContain('.dotcli/cache/');
        expect(gitignore).toContain('.dotcli/snapshot.json');
    });

    it('should call addInstance with token when token auth is selected', async () => {
        (prompts.text as jest.Mock)
            .mockResolvedValueOnce('https://demo.dotcms.com') // server URL
            .mockResolvedValueOnce('demo') // instance name
            .mockResolvedValueOnce('my-api-token'); // token
        (prompts.select as jest.Mock).mockResolvedValueOnce('token');

        await initCommand.run!({} as never);

        expect(addInstance).toHaveBeenCalledWith(
            tmpDir,
            'demo',
            'https://demo.dotcms.com',
            'my-api-token'
        );
    });

    it('should call addInstance without token when skip is selected', async () => {
        (prompts.text as jest.Mock)
            .mockResolvedValueOnce('https://demo.dotcms.com')
            .mockResolvedValueOnce('demo');
        (prompts.select as jest.Mock).mockResolvedValueOnce('skip');

        await initCommand.run!({} as never);

        expect(addInstance).toHaveBeenCalledWith(
            tmpDir,
            'demo',
            'https://demo.dotcms.com',
            undefined
        );
    });

    it('should save empty config when URL prompt is canceled', async () => {
        (prompts.text as jest.Mock).mockResolvedValueOnce(Symbol('cancel'));
        (prompts.isCancel as unknown as jest.Mock).mockImplementation(
            (value: unknown) => typeof value === 'symbol'
        );

        await initCommand.run!({} as never);

        expect(saveConfig).toHaveBeenCalledWith(
            tmpDir,
            expect.objectContaining({ default: '', instances: {} })
        );
    });

    it('should warn if .dotcli already exists and abort on no', async () => {
        fs.mkdirSync(path.join(tmpDir, '.dotcli'), { recursive: true });
        (prompts.confirm as jest.Mock).mockResolvedValueOnce(false);

        await initCommand.run!({} as never);

        expect(addInstance).not.toHaveBeenCalled();
    });
});
