import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

import {
    addInstance,
    authenticateWithCredentials,
    loadAuth,
    removeInstance,
    resolveToken,
    saveAuth
} from '../auth';
import { saveConfig } from '../config';

import type { AuthStore, DotCliConfig } from '../types';

jest.mock('ofetch', () => ({
    ofetch: jest.fn()
}));

describe('auth', () => {
    let tmpDir: string;

    const seedConfig: DotCliConfig = {
        default: 'demo',
        instances: {
            demo: { url: 'https://demo.dotcms.com' },
            local: { url: 'http://localhost:8080' }
        }
    };

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-auth-'));
    });

    afterEach(() => {
        fs.rmSync(tmpDir, { recursive: true, force: true });
        // Clean up env vars
        delete process.env['DOTCONTENT_TOKEN_DEMO'];
        delete process.env['DOTCONTENT_TOKEN_LOCAL'];
    });

    describe('loadAuth', () => {
        it('should return empty store when no auth file exists', () => {
            const store = loadAuth(tmpDir);
            expect(store).toEqual({});
        });

        it('should load existing auth store', () => {
            const dotcliDir = path.join(tmpDir, '.dotcli');
            fs.mkdirSync(dotcliDir, { recursive: true });
            const authStore: AuthStore = {
                demo: { type: 'token', token: 'abc123' }
            };
            fs.writeFileSync(
                path.join(dotcliDir, '.auth.json'),
                JSON.stringify(authStore),
                'utf-8'
            );

            const loaded = loadAuth(tmpDir);
            expect(loaded['demo']).toEqual({ type: 'token', token: 'abc123' });
        });
    });

    describe('saveAuth', () => {
        it('should create .dotcli dir and write auth store', () => {
            const store: AuthStore = {
                demo: { type: 'token', token: 'my-token' }
            };

            saveAuth(tmpDir, store);

            const filePath = path.join(tmpDir, '.dotcli', '.auth.json');
            expect(fs.existsSync(filePath)).toBe(true);

            const loaded = loadAuth(tmpDir);
            expect(loaded['demo'].token).toBe('my-token');
        });
    });

    describe('resolveToken', () => {
        beforeEach(() => {
            // Seed auth file
            const store: AuthStore = {
                demo: { type: 'token', token: 'stored-token' }
            };
            saveAuth(tmpDir, store);
        });

        it('should prefer CLI token over all others', () => {
            process.env['DOTCONTENT_TOKEN_DEMO'] = 'env-token';
            const token = resolveToken(tmpDir, 'demo', 'cli-token');
            expect(token).toBe('cli-token');
        });

        it('should use env var when no CLI token', () => {
            process.env['DOTCONTENT_TOKEN_DEMO'] = 'env-token';
            const token = resolveToken(tmpDir, 'demo');
            expect(token).toBe('env-token');
        });

        it('should use auth store when no CLI token or env var', () => {
            const token = resolveToken(tmpDir, 'demo');
            expect(token).toBe('stored-token');
        });

        it('should return null when no token source available', () => {
            const token = resolveToken(tmpDir, 'unknown');
            expect(token).toBeNull();
        });
    });

    describe('addInstance', () => {
        it('should add instance to config and auth', () => {
            addInstance(tmpDir, 'staging', 'https://staging.dotcms.com', 'stg-token');

            // Verify config was created
            const configContent = fs.readFileSync(
                path.join(tmpDir, '.dotcli', 'config.yml'),
                'utf-8'
            );
            expect(configContent).toContain('staging');

            // Verify auth store
            const auth = loadAuth(tmpDir);
            expect(auth['staging']).toEqual({ type: 'token', token: 'stg-token' });
        });

        it('should add to existing config without replacing other instances', () => {
            saveConfig(tmpDir, seedConfig);

            addInstance(tmpDir, 'staging', 'https://staging.dotcms.com', 'tok');

            const configContent = fs.readFileSync(
                path.join(tmpDir, '.dotcli', 'config.yml'),
                'utf-8'
            );
            expect(configContent).toContain('demo');
            expect(configContent).toContain('staging');
        });

        it('should add instance without token', () => {
            addInstance(tmpDir, 'noauth', 'https://noauth.dotcms.com');

            const auth = loadAuth(tmpDir);
            expect(auth['noauth']).toBeUndefined();
        });

        it('should set as default when it is the first instance', () => {
            addInstance(tmpDir, 'first', 'https://first.dotcms.com');

            const configRaw = fs.readFileSync(path.join(tmpDir, '.dotcli', 'config.yml'), 'utf-8');
            expect(configRaw).toContain('default: first');
        });
    });

    describe('removeInstance', () => {
        beforeEach(() => {
            saveConfig(tmpDir, seedConfig);
            saveAuth(tmpDir, {
                demo: { type: 'token', token: 'demo-tok' },
                local: { type: 'token', token: 'local-tok' }
            });
        });

        it('should remove instance from config and auth', () => {
            removeInstance(tmpDir, 'local');

            const configContent = fs.readFileSync(
                path.join(tmpDir, '.dotcli', 'config.yml'),
                'utf-8'
            );
            expect(configContent).not.toContain('local');

            const auth = loadAuth(tmpDir);
            expect(auth['local']).toBeUndefined();
        });

        it('should update default when removing the default instance', () => {
            removeInstance(tmpDir, 'demo');

            const configContent = fs.readFileSync(
                path.join(tmpDir, '.dotcli', 'config.yml'),
                'utf-8'
            );
            expect(configContent).toContain('default: local');
        });

        it('should throw when removing nonexistent instance', () => {
            expect(() => removeInstance(tmpDir, 'nonexistent')).toThrow(
                'Instance "nonexistent" not found'
            );
        });
    });

    describe('authenticateWithCredentials', () => {
        it('should POST credentials and return token', async () => {
            const { ofetch: mockOfetch } = require('ofetch');
            mockOfetch.mockResolvedValueOnce({
                entity: { token: 'jwt-token-from-server' }
            });

            const token = await authenticateWithCredentials(
                'https://demo.dotcms.com',
                'admin@dotcms.com',
                'admin'
            );

            expect(token).toBe('jwt-token-from-server');
            expect(mockOfetch).toHaveBeenCalledWith('/api/v1/authentication/api-token', {
                baseURL: 'https://demo.dotcms.com',
                method: 'POST',
                body: {
                    user: 'admin@dotcms.com',
                    password: 'admin',
                    expirationDays: 30
                }
            });
        });
    });
});
