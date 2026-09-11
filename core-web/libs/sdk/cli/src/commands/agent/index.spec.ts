import { pausingConfirm, pausingPort } from './index';

import type { PromptPort } from '../../shared/prompts';

/**
 * `npx dotcms agent setup` looked hung at `Checking https://demo.dotcms.com`. It was not: the
 * `Checking` spinner was never stopped, so ora kept repainting over the username prompt that
 * inquirer had already drawn. The developer saw a spinning step and a bare cursor, and the
 * process sat waiting for an answer to an invisible question.
 *
 * Nothing between `step('Checking …')` and the credential prompts stopped the spinner — the
 * next `step()` is `Minting an access token`, which cannot run until those answers arrive.
 *
 * ORDER is the whole assertion. Pausing after the prompt has been drawn fixes nothing, and a
 * test that only checked "pause was called" would pass against exactly that bug.
 */
describe('a prompt owns the terminal before it draws', () => {
    function harness() {
        const calls: string[] = [];
        const progress = { pause: () => void calls.push('pause') };
        const port: PromptPort = {
            async text() {
                calls.push('text');
                return 'answer';
            },
            async password() {
                calls.push('password');
                return 'secret';
            },
            async select<T extends string>() {
                calls.push('select');
                return 'a' as T;
            },
            async multiSelect<T extends string>() {
                calls.push('multiSelect');
                return [] as T[];
            }
        };
        return { calls, progress, port };
    }

    it('stops the spinner before asking for text', async () => {
        const { calls, progress, port } = harness();
        await pausingPort(port, progress).text('dotCMS instance address');
        expect(calls).toEqual(['pause', 'text']);
    });

    it('stops the spinner before asking for a password', async () => {
        const { calls, progress, port } = harness();
        await pausingPort(port, progress).password('Password');
        expect(calls).toEqual(['pause', 'password']);
    });

    it('stops the spinner before a select and a multi-select', async () => {
        const { calls, progress, port } = harness();
        const wrapped = pausingPort(port, progress);
        await wrapped.select('How should we authenticate?', []);
        await wrapped.multiSelect('Which editors?', []);
        expect(calls).toEqual(['pause', 'select', 'pause', 'multiSelect']);
    });

    it('passes the arguments and the answer straight through', async () => {
        const seen: unknown[] = [];
        const port = {
            text: async (m: string, d?: string) => {
                seen.push([m, d]);
                return 'answer';
            }
        } as unknown as PromptPort;
        const answer = await pausingPort(port, { pause: () => undefined }).text('Q', 'default');
        expect(seen).toEqual([['Q', 'default']]);
        expect(answer).toBe('answer');
    });

    it('does the same for the confirmations, which run under the writing spinner', async () => {
        const calls: string[] = [];
        const confirm = async (file: string) => {
            calls.push(`confirm:${file}`);
            return true;
        };
        const wrapped = pausingConfirm(confirm, { pause: () => void calls.push('pause') });
        await expect(wrapped('.mcp.json')).resolves.toBe(true);
        expect(calls).toEqual(['pause', 'confirm:.mcp.json']);
    });
});
