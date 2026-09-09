/**
 * The only sanctioned way for this harness to run a child process.
 *
 * Every ref, branch name and file path the harness handles originates in pull-request metadata,
 * which is untrusted input. A shell-interpolated branch name is a command-injection vector in a
 * tool destined to run in CI, so nothing here ever builds a shell string: `execFile` receives an
 * argument array and no shell is spawned. Constitution Principle III; guaranteed in contracts/cli.md.
 */
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);

/** Raised when a child process exits non-zero and the caller did not allow it. */
export class ExecError extends Error {
    constructor(command, args, cause) {
        super(`${command} ${args.join(' ')} failed: ${cause.message}`);
        this.name = 'ExecError';
        this.command = command;
        this.args = args;
        this.exitCode = cause.code;
        this.stderr = cause.stderr ?? '';
        this.cause = cause;
    }
}

/**
 * @param {string} command
 * @param {string[]} args      Passed through verbatim; never concatenated into a shell string.
 * @param {{ cwd?: string, allowFailure?: boolean, maxBuffer?: number }} [options]
 * @returns {Promise<{ stdout: string, stderr: string, exitCode: number }>}
 */
export async function run(command, args, options = {}) {
    if (!Array.isArray(args)) {
        throw new TypeError('exec.run requires an argument array — never a shell string');
    }
    const { cwd, allowFailure = false, maxBuffer = 64 * 1024 * 1024 } = options;
    try {
        const { stdout, stderr } = await execFileAsync(command, args, {
            cwd,
            maxBuffer,
            shell: false,
            encoding: 'utf8'
        });
        return { stdout, stderr, exitCode: 0 };
    } catch (error) {
        if (allowFailure) {
            return {
                stdout: error.stdout ?? '',
                stderr: error.stderr ?? '',
                exitCode: typeof error.code === 'number' ? error.code : 1
            };
        }
        throw new ExecError(command, args, error);
    }
}

/** Convenience wrapper for git, which is most of what the harness shells out to. */
export function git(args, options = {}) {
    return run('git', args, options);
}
