import {
    ConflictingAuthError,
    CredentialsRejectedError,
    InstanceUnreachableError,
    InvalidUrlError,
    MalformedConfigError,
    MissingInputError,
    NoConfigPathError,
    TokenRejectedError,
    UnknownTargetError,
    UsageError
} from './errors';

/**
 * FR-032a makes this a real test rather than a review note.
 *
 * There is no verbose flag, no debug mode and no log file — stdout IS the entire diagnostic
 * surface, so "re-run with more detail" is not a remedy that exists. A message that reports
 * only that something failed is a defect by definition, and this suite is what stops that
 * from creeping back in.
 */

/** Every error the tool can produce, with the identifier each one is expected to name. */
const CASES: { name: string; error: Error; mustName: string }[] = [
    { name: 'InvalidUrlError', error: new InvalidUrlError('demo.dotcms.com'), mustName: 'demo.dotcms.com' },
    { name: 'InstanceUnreachableError', error: new InstanceUnreachableError('https://x.example.com', 'Connection refused'), mustName: 'x.example.com' },
    { name: 'CredentialsRejectedError', error: new CredentialsRejectedError('https://x.example.com'), mustName: 'x.example.com' },
    { name: 'TokenRejectedError', error: new TokenRejectedError('https://x.example.com'), mustName: 'x.example.com' },
    { name: 'UnknownTargetError', error: new UnknownTargetError('nope', ['cursor', 'codex']), mustName: 'nope' },
    { name: 'ConflictingAuthError', error: new ConflictingAuthError('--authToken', '--password'), mustName: '--authToken' },
    { name: 'MissingInputError', error: new MissingInputError('A password'), mustName: 'A password' },
    { name: 'MalformedConfigError', error: new MalformedConfigError('/tmp/mcp.json'), mustName: '/tmp/mcp.json' },
    { name: 'NoConfigPathError', error: new NoConfigPathError('Codex', 'folder'), mustName: 'Codex' }
];

/** An imperative clause telling the developer what to do next. */
const ACTION = /check|pass|set |re-run|fix|drop|supply|install|valid ids|try again|sign in/i;

describe.each(CASES)('$name', ({ error, mustName }) => {
    it('names the file, address, option or target involved', () => {
        expect(error.message).toContain(mustName);
    });

    it('tells the developer what to do about it', () => {
        expect(error.message).toMatch(ACTION);
    });

    it('is a sentence, not a bare label', () => {
        expect(error.message.length).toBeGreaterThan(30);
        expect(error.message).toMatch(/[.!]/);
    });

    it('leaks no secret placeholder', () => {
        expect(error.message).not.toMatch(/AUTH_TOKEN=|password=|dot_[A-Za-z0-9]{10,}/);
    });
});

describe('ConflictingAuthError names the source actually used', () => {
    it('names environment variables when no flag was passed', () => {
        // Telling someone who set DOTCMS_AUTH_TOKEN to stop passing --authToken sends them
        // hunting for a flag they never typed.
        const err = new ConflictingAuthError('DOTCMS_AUTH_TOKEN', 'DOTCMS_PASSWORD');
        expect(err.message).toContain('DOTCMS_AUTH_TOKEN');
        expect(err.message).toContain('DOTCMS_PASSWORD');
        expect(err.message).not.toContain('--authToken');
        expect(err.message).not.toContain('--password');
    });

    it('names flags when flags were passed', () => {
        const err = new ConflictingAuthError('--authToken', '--user');
        expect(err.message).toContain('--authToken');
        expect(err.message).toContain('--user');
    });
});

describe('InvalidUrlError suggests a usable address', () => {
    it('strips a non-http scheme rather than prefixing https:// onto it', () => {
        // "ftp://demo.dotcms.com" previously suggested "https://ftp://demo.dotcms.com".
        const err = new InvalidUrlError('ftp://demo.dotcms.com');
        expect(err.message).toContain('https://demo.dotcms.com');
        expect(err.message).not.toContain('https://ftp://');
    });

    it.each(['file:///etc/passwd', 'javascript:alert(1)', 'ws://x.example.com'])(
        'produces no double scheme for %s',
        (bad) => {
            expect(new InvalidUrlError(bad).message).not.toMatch(/https:\/\/\w+:\/\//);
        }
    );

    it('still suggests a host for a bare address', () => {
        expect(new InvalidUrlError('demo.dotcms.com').message).toContain('https://demo.dotcms.com');
    });
});

describe('exit-code classification', () => {
    it('marks usage mistakes so the CLI can exit 2 rather than 1', () => {
        expect(new ConflictingAuthError('--authToken', '--user')).toBeInstanceOf(UsageError);
        expect(new UnknownTargetError('x', ['y'])).toBeInstanceOf(UsageError);
        expect(new MissingInputError('A password')).toBeInstanceOf(UsageError);
    });

    it('does NOT mark runtime failures as usage mistakes', () => {
        expect(new TokenRejectedError('https://x')).not.toBeInstanceOf(UsageError);
        expect(new InstanceUnreachableError('https://x', 'refused')).not.toBeInstanceOf(UsageError);
        expect(new MalformedConfigError('/tmp/x.json')).not.toBeInstanceOf(UsageError);
    });
});
