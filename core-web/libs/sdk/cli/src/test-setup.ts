import { existsSync } from 'node:fs';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * A unit test must never touch the developer's machine or this repository.
 *
 * It happened: `installSkills` really shelled out to `npx skills add` during a test run and
 * installed two skill trees into `libs/sdk/cli/.agents/skills/`, which a `git add -A` then
 * committed. Nothing failed — the suite was green while writing files into the repo.
 *
 * This is the backstop. Every spec file gets it, so whichever one causes it is the one that
 * fails, rather than the damage being noticed days later in a diff.
 */
// `__dirname` is a CommonJS global; Vite serves this file as ESM, where it does not exist.
const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

/**
 * Everything the CLI writes at folder scope, plus what the skills installer leaves behind.
 * None of it belongs in the source tree.
 *
 * The first version of this list missed `.claude`, `skills-lock.json` and `.mcp.json`, and the
 * run that added it promptly found all three sitting in the repo — so the list is the whole
 * surface, not the parts I happened to remember.
 */
const FORBIDDEN = [
    '.agents',
    '.claude',
    '.codex',
    '.cursor',
    '.devin',
    '.vscode',
    '.mcp.json',
    'opencode.json',
    'skills-lock.json'
];

afterAll(() => {
    const created = FORBIDDEN.filter((name) => existsSync(path.join(PROJECT_ROOT, name)));
    if (created.length > 0) {
        throw new Error(
            `A test wrote agent configuration into the source tree: ${created.join(', ')}\n` +
                `Mock 'node:child_process' and the filesystem, or pass an explicit cwd. ` +
                `Delete ${created.map((c) => `libs/sdk/cli/${c}`).join(', ')} before committing.`
        );
    }
});
