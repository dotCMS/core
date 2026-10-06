/**
 * Decides, per project, whether the Angular compiler runs — and always says why.
 *
 * The spec forbids a silent fallback, and the reason is concrete: a project that quietly drops to
 * TypeScript-only has its templates unchecked while the run still reports PASS. That is
 * indistinguishable from "the templates are fine", which is the failure a gate exists to prevent.
 */
import fs from 'node:fs/promises';
import path from 'node:path';

/** Strips comments and trailing commas so a tsconfig with JSONC in it can be read. */
function parseJsonc(text) {
    const stripped = text
        .replace(/\\"|"(?:\\"|[^"])*"|(\/\/.*|\/\*[\s\S]*?\*\/)/g, (m, comment) => (comment ? '' : m))
        .replace(/,(\s*[}\]])/g, '$1');
    return JSON.parse(stripped);
}

/** Walks the `extends` chain looking for `angularCompilerOptions`. */
async function declaresAngular(configPath, seen = new Set()) {
    const resolved = path.resolve(configPath);
    if (seen.has(resolved)) return false;
    seen.add(resolved);

    let config;
    try {
        config = parseJsonc(await fs.readFile(resolved, 'utf8'));
    } catch {
        return false;
    }
    if (config.angularCompilerOptions) return true;
    if (!config.extends) return false;

    const parents = Array.isArray(config.extends) ? config.extends : [config.extends];
    for (const parent of parents) {
        const candidate = parent.startsWith('.')
            ? path.resolve(path.dirname(resolved), parent)
            : null;
        if (!candidate) continue;
        const withExt = candidate.endsWith('.json') ? candidate : `${candidate}.json`;
        if (await declaresAngular(withExt, seen)) return true;
    }
    return false;
}

/**
 * @param {{ configPath: string, templates?: boolean }} input
 * @returns {Promise<{ mode: 'typescript'|'template-aware', reason: string }>}
 */
export async function selectMode({ configPath, templates = false }) {
    if (!templates) {
        return {
            mode: 'typescript',
            reason: 'template checking not requested (--templates off)'
        };
    }
    if (await declaresAngular(configPath)) {
        return {
            mode: 'template-aware',
            reason: 'project declares angularCompilerOptions in its config chain'
        };
    }
    return {
        mode: 'typescript',
        reason: 'not an Angular project: no angularCompilerOptions found in the config chain'
    };
}
