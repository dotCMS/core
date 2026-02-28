import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

import { DotCliConfigSchema, loadConfig, resolveInstance, saveConfig } from '../config';

import type { DotCliConfig } from '../types';

describe('config', () => {
    let tmpDir: string;

    const validConfig: DotCliConfig = {
        default: 'demo',
        instances: {
            demo: { url: 'https://demo.dotcms.com' },
            local: { url: 'http://localhost:8080' }
        }
    };

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-config-'));
    });

    afterEach(() => {
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    describe('loadConfig', () => {
        it('should load and validate a valid config file', () => {
            const dotcliDir = path.join(tmpDir, '.dotcli');
            fs.mkdirSync(dotcliDir, { recursive: true });
            fs.writeFileSync(
                path.join(dotcliDir, 'config.yml'),
                'default: demo\ninstances:\n  demo:\n    url: https://demo.dotcms.com\n',
                'utf-8'
            );

            const config = loadConfig(tmpDir);
            expect(config.default).toBe('demo');
            expect(config.instances['demo'].url).toBe('https://demo.dotcms.com');
        });

        it('should throw if config file does not exist', () => {
            expect(() => loadConfig(tmpDir)).toThrow('Config file not found');
        });

        it('should throw on invalid config (missing default)', () => {
            const dotcliDir = path.join(tmpDir, '.dotcli');
            fs.mkdirSync(dotcliDir, { recursive: true });
            fs.writeFileSync(
                path.join(dotcliDir, 'config.yml'),
                'instances:\n  demo:\n    url: https://demo.dotcms.com\n',
                'utf-8'
            );

            expect(() => loadConfig(tmpDir)).toThrow();
        });

        it('should throw on invalid config (bad URL)', () => {
            const dotcliDir = path.join(tmpDir, '.dotcli');
            fs.mkdirSync(dotcliDir, { recursive: true });
            fs.writeFileSync(
                path.join(dotcliDir, 'config.yml'),
                'default: demo\ninstances:\n  demo:\n    url: not-a-url\n',
                'utf-8'
            );

            expect(() => loadConfig(tmpDir)).toThrow();
        });

        it('should accept optional fields (pull, concurrency)', () => {
            const dotcliDir = path.join(tmpDir, '.dotcli');
            fs.mkdirSync(dotcliDir, { recursive: true });
            const yml = [
                'default: demo',
                'instances:',
                '  demo:',
                '    url: https://demo.dotcms.com',
                'concurrency: 10',
                'pull:',
                '  - type: Blog',
                '    site: default',
                ''
            ].join('\n');
            fs.writeFileSync(path.join(dotcliDir, 'config.yml'), yml, 'utf-8');

            const config = loadConfig(tmpDir);
            expect(config.concurrency).toBe(10);
            expect(config.pull).toHaveLength(1);
            expect(config.pull![0].type).toBe('Blog');
        });
    });

    describe('saveConfig', () => {
        it('should create .dotcli dir and write config', () => {
            saveConfig(tmpDir, validConfig);

            const filePath = path.join(tmpDir, '.dotcli', 'config.yml');
            expect(fs.existsSync(filePath)).toBe(true);

            // Verify it can be loaded back
            const loaded = loadConfig(tmpDir);
            expect(loaded.default).toBe('demo');
            expect(loaded.instances['local'].url).toBe('http://localhost:8080');
        });

        it('should overwrite an existing config', () => {
            saveConfig(tmpDir, validConfig);

            const updated: DotCliConfig = {
                default: 'local',
                instances: {
                    local: { url: 'http://localhost:9090' }
                }
            };
            saveConfig(tmpDir, updated);

            const loaded = loadConfig(tmpDir);
            expect(loaded.default).toBe('local');
            expect(Object.keys(loaded.instances)).toHaveLength(1);
        });
    });

    describe('resolveInstance', () => {
        it('should resolve by explicit name', () => {
            const result = resolveInstance(validConfig, 'local');
            expect(result.name).toBe('local');
            expect(result.url).toBe('http://localhost:8080');
        });

        it('should resolve using default when no name given', () => {
            const result = resolveInstance(validConfig);
            expect(result.name).toBe('demo');
            expect(result.url).toBe('https://demo.dotcms.com');
        });

        it('should throw for unknown instance name', () => {
            expect(() => resolveInstance(validConfig, 'nonexistent')).toThrow(
                'Instance "nonexistent" not found'
            );
        });
    });

    describe('DotCliConfigSchema', () => {
        it('should reject empty default', () => {
            const result = DotCliConfigSchema.safeParse({
                default: '',
                instances: {}
            });
            expect(result.success).toBe(false);
        });

        it('should reject negative concurrency', () => {
            const result = DotCliConfigSchema.safeParse({
                default: 'test',
                instances: { test: { url: 'https://example.com' } },
                concurrency: -1
            });
            expect(result.success).toBe(false);
        });
    });
});
