import { type PromptPort, resolveInstanceUrl, resolveRequiredInputs } from './prompts';

const URL_ = 'https://demo.dotcms.com';

function port(overrides: Partial<PromptPort> = {}): PromptPort & { calls: string[] } {
    const calls: string[] = [];
    return {
        calls,
        // A realistic answer: the prompt is followed by the same validation an option gets,
        // so an unparseable value would fail before the assertion under test.
        async text(message: string) {
            calls.push(`text:${message}`);
            return 'https://typed.example.com';
        },
        async password(message: string) {
            calls.push(`password:${message}`);
            return 'typed-pw';
        },
        async select<T extends string>(message: string, choices: { name: string; value: T }[]) {
            calls.push(`select:${message}`);
            return choices[0].value;
        },
        ...overrides
    } as PromptPort & { calls: string[] };
}

const OLD = process.env;
beforeEach(() => {
    process.env = { ...OLD };
});
afterAll(() => {
    process.env = OLD;
});

describe('nothing is asked when the required inputs are supplied (FR-003i)', () => {
    it('asks nothing with a url and a token, on a terminal', async () => {
        const p = port();
        const out = await resolveRequiredInputs({ url: URL_, authToken: 'tok' }, p, true);
        // `p.calls` is the real evidence nothing was asked; `out.prompted` was a flag the
        // function set about itself, and nothing outside this test ever read it.
        expect(p.calls).toEqual([]);
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
        const err = await resolveRequiredInputs(
            { url: URL_, user: 'a@b.com' },
            port(),
            false
        ).catch((e: Error) => e);
        expect((err as Error).message).toMatch(/password/i);
    });

    it('offers the choice of auth mode when neither was supplied', async () => {
        const p = port();
        await resolveRequiredInputs({ url: URL_ }, p, true);
        expect(p.calls.some((c) => c.startsWith('select:'))).toBe(true);
    });

    it('fails naming BOTH modes when neither was supplied and there is no terminal (FR-003h)', async () => {
        const err = await resolveRequiredInputs({ url: URL_ }, port(), false).catch(
            (e: Error) => e
        );
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
        const err = await resolveRequiredInputs(
            { url: URL_, user: 'a@b.com', yes: true },
            port(),
            false
        ).catch((e: Error) => e);
        expect((err as Error).message).toMatch(/password/i);
    });
});

describe('resolveInstanceUrl (FR-004)', () => {
    /**
     * These lived in `instance.spec.ts` against `resolveUrl`, a second implementation of this
     * same option -> env -> prompt chain that nothing in the CLI ever called. So the FR-004
     * suite was green while the shipped path had no precedence coverage at all.
     */
    const OLD_ENV = process.env;
    beforeEach(() => {
        process.env = { ...OLD_ENV };
    });
    afterAll(() => {
        process.env = OLD_ENV;
    });

    it('prefers the supplied option over the environment', async () => {
        process.env['DOTCMS_URL'] = 'https://from-env.example.com';
        await expect(resolveInstanceUrl({ url: 'https://from-option.example.com' })).resolves.toBe(
            'https://from-option.example.com'
        );
    });

    it('falls back to the environment when no option is supplied', async () => {
        process.env['DOTCMS_URL'] = 'https://from-env.example.com';
        await expect(resolveInstanceUrl({})).resolves.toBe('https://from-env.example.com');
    });

    it('strips trailing slashes', async () => {
        await expect(resolveInstanceUrl({ url: 'https://demo.dotcms.com///' })).resolves.toBe(
            'https://demo.dotcms.com'
        );
    });

    it('rejects an address with no scheme rather than writing it through verbatim', async () => {
        await expect(resolveInstanceUrl({ url: 'demo.dotcms.com' })).rejects.toThrow(
            /scheme|protocol|https?:\/\//i
        );
    });

    /**
     * `new URL()` alone is not enough: it happily parses ftp:, file: and javascript:. Only
     * an explicit http(s) check rejects those, and nothing else in the flow would — the
     * address is handed to fetch and written into an editor's config.
     */
    it.each(['ftp://demo.dotcms.com', 'file:///etc/passwd', 'javascript:alert(1)'])(
        'rejects the non-HTTP scheme %s',
        async (bad) => {
            await expect(resolveInstanceUrl({ url: bad })).rejects.toThrow(
                /scheme|protocol|https?:\/\//i
            );
        }
    );

    it('accepts plain http, not only https — local instances are the common case', async () => {
        await expect(resolveInstanceUrl({ url: 'http://localhost:8082' })).resolves.toBe(
            'http://localhost:8082'
        );
    });
});

describe('an address may not smuggle a credential (FR-022a)', () => {
    /**
     * `https://user:secret@host` passed validation, and the address is then written into
     * DOTCMS_URL in every editor's config and printed in the summary — so a password reached
     * disk in plaintext and the terminal in clear, which FR-022a forbids outright.
     */
    it.each([
        'https://admin:hunter2@demo.dotcms.com',
        'http://admin:hunter2@localhost:8082',
        'https://admin@demo.dotcms.com'
    ])('rejects %s', async (bad) => {
        await expect(resolveInstanceUrl({ url: bad })).rejects.toThrow();
    });

    it('never echoes the credential back in the error', async () => {
        const error = await resolveInstanceUrl({
            url: 'https://admin:hunter2@demo.dotcms.com'
        }).catch((e: Error) => e);
        // Not even partially: a redacted password is still a leaked password.
        expect((error as Error).message).not.toContain('hunter2');
        expect((error as Error).message).not.toContain('hunter');
        expect((error as Error).message).toContain('demo.dotcms.com');
        // And it must say WHY, with a remedy that is not the address it just refused.
        expect((error as Error).message).toMatch(/username or password/i);
        expect((error as Error).message).toMatch(/--authToken|sign in/i);
    });

    it('still accepts an ordinary address', async () => {
        await expect(resolveInstanceUrl({ url: 'https://demo.dotcms.com' })).resolves.toBe(
            'https://demo.dotcms.com'
        );
    });
});

describe('a password containing @ must not leak either (FR-022a)', () => {
    /**
     * The first fix stripped with `[^/@]*@`, which stops at the FIRST `@` — but the URL spec
     * makes the LAST one the userinfo delimiter. So `admin:p@ssw0rd` left `ssw0rd` in the
     * message, and the generic error went on to SUGGEST `https://ssw0rd@demo.dotcms.com` as
     * the corrected address. A redaction bug inside the redaction.
     */
    it.each([
        ['@ in the password', 'https://admin:p@ssw0rd@demo.dotcms.com', 'ssw0rd'],
        ['several @', 'https://a:b@c@d@demo.dotcms.com', 'd@demo'],
        ['@ and a path', 'https://admin:p@ss@demo.dotcms.com/x', 'p@ss']
    ])('%s', async (_name, url, leaked) => {
        const error = (await resolveInstanceUrl({ url }).catch((e: Error) => e)) as Error;
        expect(error.message).not.toContain(leaked);
        expect(error.message).not.toContain('ssw0rd');
        expect(error.message).toContain('demo.dotcms.com');
    });
});
