import inquirer from 'inquirer';

import type { AgentTarget } from './targets/types';
import type { PromptPort } from '../../shared/prompts';

/**
 * The inquirer-backed port.
 *
 * Kept behind `PromptPort` so `shared/prompts.ts` owns the RULES about when to ask and this
 * module owns only HOW — which is what lets those rules be tested without a terminal.
 *
 * Uses `inquirer`'s own prompt module rather than the `@inquirer/*` sub-packages: `inquirer` is
 * already a declared dependency of this package and of `create-app`, and pulling in five more
 * would add install weight for every `npx dotcms` user to no benefit.
 */
export const inquirerPort: PromptPort = {
    async text(message, defaultValue) {
        const { value } = await inquirer.prompt<{ value: string }>([
            { type: 'input', name: 'value', message, default: defaultValue }
        ]);
        return value;
    },
    async password(message) {
        // `type: 'password'` with a mask is what satisfies FR-003g — a prompted secret is
        // never echoed to the terminal.
        const { value } = await inquirer.prompt<{ value: string }>([
            { type: 'password', name: 'value', message, mask: '*' }
        ]);
        return value;
    },
    async select<T extends string>(message: string, choices: { name: string; value: T }[]) {
        const { value } = await inquirer.prompt<{ value: T }>([
            { type: 'list', name: 'value', message, choices }
        ]);
        return value;
    }
};

/** Detected editors arrive pre-checked; any supported editor stays selectable (FR-010). */
export async function chooseTargets(all: AgentTarget[], detected: Set<string>): Promise<string[]> {
    const { value } = await inquirer.prompt<{ value: string[] }>([
        {
            type: 'checkbox',
            name: 'value',
            message: 'Which editors should we configure?',
            choices: all.map((t) => ({ name: t.displayName, value: t.id, checked: detected.has(t.id) }))
        }
    ]);
    return value;
}

async function ask(message: string): Promise<boolean> {
    const { value } = await inquirer.prompt<{ value: boolean }>([
        { type: 'confirm', name: 'value', message, default: true }
    ]);
    return value;
}

export const confirmOverwrite = (file: string): Promise<boolean> =>
    ask(`${file} already has a dotcms entry. Replace it?`);

export const confirmExclude = (files: string[]): Promise<boolean> =>
    ask(
        `Add ${files.length === 1 ? 'this file' : `these ${files.length} files`} to .gitignore? ` +
            `They contain an access token.`
    );
