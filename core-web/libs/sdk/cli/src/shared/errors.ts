/**
 * Named, self-sufficient errors.
 *
 * This release ships no verbose flag, no debug mode and no log file (spec Assumptions), so a
 * message here IS the entire diagnostic surface. FR-032a makes that explicit: a message that
 * only reports that something failed is a defect, because "re-run with more detail" is not an
 * available remedy. Every error below names the thing involved and what to do about it.
 */

export class CliError extends Error {
    constructor(message: string) {
        super(message);
        this.name = new.target.name;
    }
}

export class UsageError extends CliError {}

export class InvalidUrlError extends CliError {
    constructor(value: string) {
        // Describing the problem is not enough: with no verbose mode, the message has to
        // carry the fix too (FR-032a).
        // Strip ANY scheme, not just http(s): stripping only http(s) turned
        // "ftp://demo.dotcms.com" into the suggestion "https://ftp://demo.dotcms.com".
        // Drop any `user:password@` before the value is used at all. A rejected address may
        // be rejected BECAUSE it carries a credential, and echoing it — even partially — would
        // put that password on screen (FR-022a).
        const safe = value.replace(/^([a-z][a-z0-9+.-]*:\/\/)[^/@]*@/i, '$1');
        const host = safe.replace(/^[a-z][a-z0-9+.-]*:\/\//i, '').replace(/^\/+/, '');
        super(
            `"${safe}" is not a valid instance address. Pass it with an http:// or https:// ` +
                `scheme, e.g. https://${host || 'demo.dotcms.com'}`
        );
    }
}

/**
 * An address carrying `user:password@`.
 *
 * Its own error rather than a flavour of InvalidUrlError, because the REMEDY is different:
 * the address is fine, it is the credential riding along that is not. Reusing the generic
 * message produced "\"https://demo.dotcms.com\" is not a valid instance address … e.g.
 * https://demo.dotcms.com" — the suggestion and the complaint were the same string.
 */
export class CredentialInUrlError extends CliError {
    constructor(url: string) {
        // Built from the STRIPPED address; the password must not reach the screen (FR-022a).
        const safe = url.replace(/^([a-z][a-z0-9+.-]*:\/\/)[^/@]*@/i, '$1');
        super(
            `The instance address must not carry a username or password. Use ${safe} on its own — ` +
                `setup will ask you to sign in, or pass --authToken.`
        );
    }
}

export class InstanceUnreachableError extends CliError {
    constructor(url: string, reason: string) {
        super(
            `Could not reach ${url} — ${reason}. Check the address and that the instance is running.`
        );
    }
}

export class NotADotCmsInstanceError extends CliError {
    constructor(url: string) {
        // Says what is wrong, not how it was determined. The endpoint we probe and the shape we
        // look for are our business; the developer needs the verdict and the fix.
        super(`${url} is not a valid dotCMS instance. Check the address.`);
    }
}

export class CredentialsRejectedError extends CliError {
    constructor(url: string) {
        super(`The username and password were rejected by ${url}. Check them and try again.`);
    }
}

export class TokenRejectedError extends CliError {
    constructor(url: string) {
        super(
            `The token was rejected by ${url} — it is invalid, expired, or revoked. ` +
                `Nothing has been written. Supply a valid token, or sign in to mint a new one.`
        );
    }
}

export class UnknownTargetError extends UsageError {
    constructor(id: string, valid: readonly string[]) {
        super(`"${id}" is not a supported editor. Valid ids: ${valid.join(', ')}.`);
    }
}

export class ConflictingAuthError extends UsageError {
    /**
     * Names the sources the developer actually used. Reporting `--authToken` to someone who set
     * `DOTCMS_AUTH_TOKEN` and never typed a flag sends them looking for a flag that is not there.
     */
    constructor(tokenSource: string, credentialSource: string) {
        super(
            `${tokenSource} cannot be combined with ${credentialSource}. They are alternative ways ` +
                'to authenticate, not a fallback chain — use one or the other. Nothing has been written.'
        );
    }
}

export class MissingInputError extends UsageError {
    constructor(what: string) {
        super(
            `${what} is required and there is no terminal to prompt on. Pass it as an option or set its environment variable.`
        );
    }
}

export class NoConfigPathError extends CliError {
    constructor(displayName: string, scope: string) {
        super(
            `${displayName} has no configuration file at ${scope} scope. ` +
                // `--project` was never a flag: `index.ts` registers only `-g/--global`, so the
                // remedy named something the developer could not type. With no verbose mode the
                // message IS the diagnostic surface (FR-032a).
                `Re-run ${scope === 'folder' ? 'with -g/--global' : 'without -g/--global'}, or drop it from --agent.`
        );
    }
}

export class MalformedConfigError extends CliError {
    /** The format is passed, not sniffed from the extension: both callers know exactly which
     *  parser just refused the file. */
    constructor(file: string, format: 'JSON' | 'TOML' = 'JSON') {
        super(
            `${file} is not valid ${format} — fix it or re-run with ` +
                `--skip-mcp. It has been left untouched.`
        );
    }
}
