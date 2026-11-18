import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

/**
 * Guard against an inquirer-version trap that costs more to diagnose than to prevent.
 *
 * This package uses inquirer 13, which is built on @inquirer/prompts. There the single-choice
 * prompt is `select`. `list` is the inquirer 8/9 name: it is not registered, so the prompt
 * renders its MESSAGE and then silently renders no choices at all. Nothing throws, nothing
 * warns, and the user is left staring at a question with no answers under it.
 *
 * That shipped once (#37262) and survived a round of "fixes" aimed at the wrong cause, because
 * the symptom looks like a rendering/styling problem rather than a wrong prompt type.
 */
describe('prompt types are the ones inquirer 13 actually registers', () => {
    const source = readFileSync(resolve(__dirname, 'asks.ts'), 'utf8');

    it("never uses type: 'list' — inquirer 13 calls it 'select'", () => {
        const offenders = source
            .split('\n')
            .map((line, i) => ({ line: line.trim(), number: i + 1 }))
            .filter(({ line }) => /type:\s*['"]list['"]/.test(line) && !line.startsWith('//'));

        expect(offenders).toEqual([]);
    });

    it('uses only prompt types this inquirer version registers', () => {
        const registered = [
            'input',
            'select',
            'checkbox',
            'confirm',
            'password',
            'expand',
            'editor',
            'number',
            'rawlist',
            'search'
        ];
        const used = [...source.matchAll(/^\s*type:\s*['"]([a-z]+)['"]/gm)].map((m) => m[1]);

        expect(used.length).toBeGreaterThan(0);
        expect(used.filter((t) => !registered.includes(t))).toEqual([]);
    });
});
