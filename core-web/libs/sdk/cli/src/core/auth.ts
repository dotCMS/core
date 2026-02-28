/**
 * Authentication management for @dotcms/cli.
 * Reads/writes `.dotcli/.auth.json` and resolves tokens from multiple sources.
 */
import { ofetch } from 'ofetch';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { loadConfig, saveConfig } from './config';
import { AUTH_FILE, DOTCLI_DIR, type AuthEntry, type AuthStore, type DotCliConfig } from './types';

// ─── Auth Store I/O ─────────────────────────────────────────────────────────

function authPath(projectDir: string): string {
    return path.join(projectDir, DOTCLI_DIR, AUTH_FILE);
}

/**
 * Load the auth store from `.dotcli/.auth.json`.
 * Returns an empty store if the file doesn't exist.
 */
export function loadAuth(projectDir: string): AuthStore {
    const filePath = authPath(projectDir);

    if (!fs.existsSync(filePath)) {
        return {};
    }

    const raw = fs.readFileSync(filePath, 'utf-8');
    return JSON.parse(raw) as AuthStore;
}

/**
 * Save the auth store to `.dotcli/.auth.json`.
 * Creates the `.dotcli` directory if needed.
 */
export function saveAuth(projectDir: string, store: AuthStore): void {
    const filePath = authPath(projectDir);
    const dir = path.dirname(filePath);

    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    fs.writeFileSync(filePath, JSON.stringify(store, null, 2), 'utf-8');
}

// ─── Token Resolution ───────────────────────────────────────────────────────

/**
 * Resolve an auth token for the given instance.
 * Resolution order:
 *   1. CLI flag (passed as `cliToken` param)
 *   2. Environment variable `DOTCONTENT_TOKEN_{UPPERCASED_NAME}`
 *   3. `.auth.json` store
 *   4. null (not authenticated)
 */
export function resolveToken(
    projectDir: string,
    instanceName: string,
    cliToken?: string
): string | null {
    // 1. CLI flag
    if (cliToken) {
        return cliToken;
    }

    // 2. Environment variable
    const envKey = `DOTCONTENT_TOKEN_${instanceName.toUpperCase()}`;
    const envToken = process.env[envKey];
    if (envToken) {
        return envToken;
    }

    // 3. Auth store
    const store = loadAuth(projectDir);
    const entry = store[instanceName];
    if (entry?.token) {
        return entry.token;
    }

    // 4. Not found
    return null;
}

// ─── Instance Management ────────────────────────────────────────────────────

/**
 * Add an instance to both config and auth store.
 */
export function addInstance(projectDir: string, name: string, url: string, token?: string): void {
    let config: DotCliConfig;

    try {
        config = loadConfig(projectDir);
    } catch {
        // No config yet — create a new one
        config = { default: name, instances: {} };
    }

    config.instances[name] = { url };

    // If this is the first instance, make it the default
    if (Object.keys(config.instances).length === 1) {
        config.default = name;
    }

    saveConfig(projectDir, config);

    if (token) {
        const store = loadAuth(projectDir);
        const entry: AuthEntry = { type: 'token', token };
        store[name] = entry;
        saveAuth(projectDir, store);
    }
}

/**
 * Remove an instance from both config and auth store.
 * Throws if the instance is not found.
 */
export function removeInstance(projectDir: string, name: string): void {
    const config = loadConfig(projectDir);

    if (!config.instances[name]) {
        throw new Error(`Instance "${name}" not found in config`);
    }

    delete config.instances[name];

    // If we removed the default, pick the first remaining or empty string
    if (config.default === name) {
        const remaining = Object.keys(config.instances);
        config.default = remaining.length > 0 ? remaining[0] : '';
    }

    saveConfig(projectDir, config);

    const store = loadAuth(projectDir);
    delete store[name];
    saveAuth(projectDir, store);
}

// ─── Credential Authentication ──────────────────────────────────────────────

interface AuthApiResponse {
    entity: {
        token: string;
    };
}

/**
 * Authenticate with a dotCMS instance using username/password.
 * Returns a persistent API token (not a session JWT).
 */
export async function authenticateWithCredentials(
    baseURL: string,
    username: string,
    password: string
): Promise<string> {
    const response = await ofetch<AuthApiResponse>('/api/v1/authentication/api-token', {
        baseURL,
        method: 'POST',
        body: {
            user: username,
            password,
            expirationDays: 30
        }
    });

    return response.entity.token;
}
