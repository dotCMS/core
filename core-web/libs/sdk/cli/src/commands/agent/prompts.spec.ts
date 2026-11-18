import inquirer from 'inquirer';

import { PROMPT_TYPES } from './prompts';

/**
 * This adapter is the one module the rules-vs-mechanics split leaves untested: `shared/prompts`
 * owns WHEN to ask and is covered, this owns HOW and needs a terminal.
 *
 * It shipped `type: 'list'`, which inquirer renamed to `select` in v9/v10. An unregistered type
 * does not throw — the prompt just hangs, so the CLI stopped dead at "How should we
 * authenticate?". Asserting the types against inquirer's own registry catches exactly that,
 * with no TTY required.
 */
describe('inquirer prompt types', () => {
    const registered = Object.keys(inquirer.createPromptModule().prompts);

    it.each(PROMPT_TYPES)('%s is a type inquirer actually registers', (type) => {
        expect(registered).toContain(type);
    });

    it('does not use the pre-v9 name for a select', () => {
        expect(PROMPT_TYPES).not.toContain('list');
    });
});
