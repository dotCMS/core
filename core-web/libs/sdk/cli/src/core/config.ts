/**
 * Configuration loading and writing for @dotcms/cli.
 * Reads/writes `.dotcli/config.yml` using the `yaml` library and validates with zod.
 */
import { parse, stringify } from 'yaml';
import { z } from 'zod';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { CONFIG_FILE, DOTCLI_DIR, type DotCliConfig, type InstanceConfig } from './types';

// ─── Zod Schemas ────────────────────────────────────────────────────────────

const InstanceConfigSchema = z.object({
    url: z.string().url()
});

const PullConfigEntrySchema = z.object({
    type: z.string(),
    site: z.string().optional(),
    query: z.string().optional(),
    language: z.string().optional(),
    limit: z.number().int().positive().optional(),
    withBinaries: z.boolean().optional()
});

export const DotCliConfigSchema = z.object({
    default: z.string().min(1),
    instances: z.record(z.string(), InstanceConfigSchema),
    pull: z.array(PullConfigEntrySchema).optional(),
    concurrency: z.number().int().positive().optional(),
    requestDelay: z.number().int().nonnegative().optional()
});

// ─── Functions ──────────────────────────────────────────────────────────────

/**
 * Resolves the path to the config file for a project directory.
 */
function configPath(projectDir: string): string {
    return path.join(projectDir, DOTCLI_DIR, CONFIG_FILE);
}

/**
 * Load and validate the DotCLI config from `.dotcli/config.yml`.
 * Throws if the file doesn't exist or is invalid.
 */
export function loadConfig(projectDir: string): DotCliConfig {
    const filePath = configPath(projectDir);

    if (!fs.existsSync(filePath)) {
        throw new Error(`Config file not found: ${filePath}`);
    }

    const raw = fs.readFileSync(filePath, 'utf-8');
    const parsed = parse(raw);
    return DotCliConfigSchema.parse(parsed);
}

/**
 * Save a DotCLI config to `.dotcli/config.yml`.
 * Creates the `.dotcli` directory if it doesn't exist.
 */
export function saveConfig(projectDir: string, config: DotCliConfig): void {
    const filePath = configPath(projectDir);
    const dir = path.dirname(filePath);

    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    const yml = stringify(config);
    fs.writeFileSync(filePath, yml, 'utf-8');
}

/**
 * Resolve an instance from the config by name.
 * If no name is given, uses `config.default`.
 * Throws if the instance is not found.
 */
export function resolveInstance(
    config: DotCliConfig,
    name?: string
): InstanceConfig & { name: string } {
    const instanceName = name ?? config.default;
    const instance = config.instances[instanceName];

    if (!instance) {
        throw new Error(
            `Instance "${instanceName}" not found in config. Available: ${Object.keys(config.instances).join(', ')}`
        );
    }

    return { ...instance, name: instanceName };
}
