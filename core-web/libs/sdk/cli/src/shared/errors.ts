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
        super(
            `"${value}" is not a valid instance address. Pass it with a scheme, ` +
                `e.g. https://${value.replace(/^https?:\/\//i, '').replace(/^\/+/, '') || 'demo.dotcms.com'}`
        );
    }
}

export class InstanceUnreachableError extends CliError {
    constructor(url: string, reason: string) {
        super(`Could not reach ${url} — ${reason}. Check the address and that the instance is running.`);
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
    constructor() {
        super(
            '--authToken cannot be combined with --user or --password. They are alternative ways ' +
                'to authenticate, not a fallback chain — pass one or the other. Nothing has been written.'
        );
    }
}

export class MissingInputError extends UsageError {
    constructor(what: string) {
        super(`${what} is required and there is no terminal to prompt on. Pass it as an option or set its environment variable.`);
    }
}

export class NoConfigPathError extends CliError {
    constructor(displayName: string, scope: string) {
        super(
            `${displayName} has no configuration file at ${scope} scope. ` +
                `Re-run with ${scope === 'folder' ? '-g/--global' : '--project'}, or drop it from --agent.`
        );
    }
}

export class MalformedConfigError extends CliError {
    constructor(file: string) {
        super(
            `${file} is not valid ${file.endsWith('.toml') ? 'TOML' : 'JSON'} — fix it or re-run with ` +
                `--skip-mcp. It has been left untouched.`
        );
    }
}
