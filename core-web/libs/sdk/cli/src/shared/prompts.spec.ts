import { resolveRequiredInputs, type PromptPort } from './prompts';

const URL_ = 'https://demo.dotcms.com';

function port(overrides: Partial<PromptPort> = {}): PromptPort & { calls: string[] } {
    const calls: string[] = [];
    return {
        calls,
        // A realistic answer: the prompt is followed by the same validation an option gets,
        // so an unparseable value would fail before the assertion under test.
        async text(message: string) { calls.push(`text:${message}`); return 'https://typed.example.com'; },
        async password(message: string) { calls.push(`password:${message}`); return 'typed-pw'; },
        async select<T extends string>(message: string, choices: { name: string; value: T }[]) {
            calls.push(`select:${message}`);
            return choices[0].value;
        },
        ...overrides
    } as PromptPort & { calls: string[] };
}

const OLD = process.env;
beforeEach(() => { process.env = { ...OLD }; });
afterAll(() => { process.env = OLD; });

describe('nothing is asked when the required inputs are supplied (FR-003i)', () => {
    it('asks nothing with a url and a token, on a terminal', async () => {
        const p = port();
        const out = await resolveRequiredInputs({ url: URL_, authToken: 'tok' }, p, true);
        expect(p.calls).toEqual([]);
        expect(out.prompted).toBe(false);
        expect(out.authToken).toBe('tok');
    });

    it('asks nothing with a url and a username/password, with NO terminal', async () => {
        const p = port();
        await resolveRequiredInputs({ url: URL_, user: 'a@b.com', password: 'pw' }, p, false);
        expect(p.calls).toEqual([]);
    });

    it('takes a secret from the environment without prompting (FR-003e)', async () => {
        process.env['DOTCMS_AUTH_TOKEN'] = 'from-env';
        const p = port();
        const out = await resolveRequiredInputs({ url: URL_ }, p, true);
        expect(p.calls).toEqual([]);
        expect(out.authToken).toBe('from-env');
    });
});

describe('a missing required input prompts, or fails by name (FR-003c, FR-003k)', () => {
    it('prompts for the password alone when the username was supplied', async () => {
        const p = port();
        const out = await resolveRequiredInputs({ url: URL_, user: 'a@b.com' }, p, true);
        expect(p.calls.filter((c) => c.startsWith('password:'))).toHaveLength(1);
        expect(p.calls.filter((c) => c.startsWith('text:'))).toHaveLength(0);
        expect(out.password).toBe('typed-pw');
    });

    it('uses a masked password prompt, never a plain text one (FR-003g)', async () => {
        const p = port();
        await resolveRequiredInputs({ url: URL_, user: 'a@b.com' }, p, true);
        expect(p.calls.some((c) => c.startsWith('password:'))).toBe(true);
    });

    it('fails naming the password when there is no terminal', async () => {
        const err = await resolveRequiredInputs({ url: URL_, user: 'a@b.com' }, port(), false)
            .catch((e: Error) => e);
        expect((err as Error).message).toMatch(/password/i);
    });

    it('offers the choice of auth mode when neither was supplied', async () => {
        const p = port();
        await resolveRequiredInputs({ url: URL_ }, p, true);
        expect(p.calls.some((c) => c.startsWith('select:'))).toBe(true);
    });

    it('fails naming BOTH modes when neither was supplied and there is no terminal (FR-003h)', async () => {
        const err = await resolveRequiredInputs({ url: URL_ }, port(), false).catch((e: Error) => e);
        expect((err as Error).message).toMatch(/password/i);
        expect((err as Error).message).toMatch(/token/i);
    });

    it('prompts for the url when it is missing', async () => {
        const p = port();
        await resolveRequiredInputs({ authToken: 'tok' }, p, true);
        expect(p.calls.some((c) => c.startsWith('text:'))).toBe(true);
    });
});

describe('--yes governs confirmations only (FR-003l)', () => {
    /** The conventional implementation of -y is "assume defaults for everything", and it is
     *  WRONG here: it would skip a required input rather than answer a confirmation. */
    it('still prompts for a missing password when --yes is set', async () => {
        const p = port();
        await resolveRequiredInputs({ url: URL_, user: 'a@b.com', yes: true }, p, true);
        expect(p.calls.filter((c) => c.startsWith('password:'))).toHaveLength(1);
    });

    it('still fails by name with --yes and no terminal — it does not invent a value', async () => {
        const err = await resolveRequiredInputs({ url: URL_, user: 'a@b.com', yes: true }, port(), false)
            .catch((e: Error) => e);
        expect((err as Error).message).toMatch(/password/i);
    });
});
