import ora, { type Ora } from 'ora';

import { confirmExclude, confirmOverwrite, inquirerPort } from './prompts';
import { runSetup } from './setup';
import { TARGET_IDS } from './targets/registry';

import { canPrompt, type PromptPort } from '../../shared/prompts';
import { printBanner, renderSummary, writeOut } from '../../shared/ui';

import type { Command } from 'commander';

/**
 * Progress reporting.
 *
 * The connection check spawns `npx`, which on a cold cache downloads the server and can run
 * for the better part of a minute — silence there reads as a hang. A spinner where someone can
 * watch, plain lines where they cannot, so a CI log still shows movement.
 *
 * Wrapped in a closure rather than a bare `let`: TypeScript does not track assignments made
 * inside a callback, so a module-level `spinner` narrows to `null` and any later `.stop()` is
 * an error on `never`.
 */
function makeProgress(interactive: boolean) {
    let spinner: Ora | null = null;
    return {
        step(text: string): void {
            if (!interactive) {
                writeOut(`  · ${text}`);
                return;
            }
            spinner?.succeed();
            spinner = ora().start(text);
        },
        done(): void {
            spinner?.stop();
            spinner = null;
        },
        /**
         * Hand the terminal over to a question.
         *
         * Keeps the finished step visible, then stops repainting. `done()` erases the line;
         * here the step really did complete, so it should stay on screen above the prompt.
         */
        pause(): void {
            spinner?.succeed();
            spinner = null;
        },
        /**
         * Leave the failed step visible instead of a spinner that never stops.
         *
         * `warn` was a byte-identical second copy of this: a retry notice and a failure render
         * the same way — mark the attempt failed, then carry on.
         */
        fail(text?: string): void {
            if (spinner) spinner.fail(text);
            else if (text) writeOut(`  ✗ ${text}`);
            spinner = null;
        },
        warn(text: string): void {
            this.fail(text);
        }
    };
}

/** The part of the progress reporter a prompt needs. */
export interface Pausable {
    pause(): void;
}

/**
 * Give the terminal to whoever is asking a question.
 *
 * ora repaints on a timer; inquirer draws its prompt once and then waits. Run both and the
 * spinner overwrites the question: the developer sees the PREVIOUS step still spinning with a
 * cursor after it and no visible prompt, so the run looks hung on that step. It is not hung —
 * it is waiting for an answer to a question it has already erased.
 *
 * Wrapping the port is the only place this rule can live once. `setup.ts` does not know a
 * spinner exists (and should not), and the port does not know which step is running.
 */
export function pausingPort(port: PromptPort, progress: Pausable): PromptPort {
    return {
        text(message, defaultValue) {
            progress.pause();
            return port.text(message, defaultValue);
        },
        password(message) {
            progress.pause();
            return port.password(message);
        },
        select<T extends string>(message: string, choices: { name: string; value: T }[]) {
            progress.pause();
            return port.select(message, choices);
        },
        multiSelect<T extends string>(
            message: string,
            choices: { name: string; value: T; checked: boolean }[]
        ) {
            progress.pause();
            return port.multiSelect(message, choices);
        }
    };
}

/**
 * The same rule for the confirmations, which are prompts too — and which run under the
 * `Writing configuration…` spinner, where an invisible question is just as fatal.
 */
export function pausingConfirm<A extends unknown[]>(
    confirm: (...args: A) => Promise<boolean>,
    progress: Pausable
): (...args: A) => Promise<boolean> {
    return (...args: A) => {
        progress.pause();
        return confirm(...args);
    };
}

/**
 * The `agent` command group.
 *
 * Only `setup` ships in this release — `status` and `remove` were specified and deliberately
 * cut. The group exists from day one because it is the seam `create-app` and the dotCLI port
 * fold into later, without changing how this command is invoked (FR-002).
 */
export function registerAgentCommand(program: Command): void {
    const agent = program.command('agent').description('Connect an AI coding agent to dotCMS');

    agent
        .command('setup')
        .description('Configure your editors to talk to a dotCMS instance')
        .option('--url <url>', 'dotCMS instance address (or set DOTCMS_URL)')
        .option('--user <user>', 'username, to mint a token')
        .option(
            '--password <password>',
            'password. Visible in the process list and shell history — prefer DOTCMS_PASSWORD or the prompt'
        )
        .option(
            '--authToken <token>',
            'an existing token. Visible in the process list and shell history — prefer DOTCMS_AUTH_TOKEN or the prompt. Cannot be combined with --user/--password'
        )
        .option(
            '--agent <id>',
            `editor to configure, repeatable (${TARGET_IDS.join(', ')})`,
            (value: string, previous: string[] = []) => [...previous, value]
        )
        .option('-g, --global', 'write to your user account instead of this folder')
        .option('--skip-mcp', 'do not write configuration')
        .option('--skip-skills', 'do not install the dotCMS skills')
        .option('--skip-verify', 'do not launch the server to confirm it responds')
        .option('-y, --yes', 'accept confirmations (never skips a required input)')
        .option('--force', 'replace an existing dotcms entry without asking')
        .action(async (options: Record<string, unknown>) => {
            const interactive = canPrompt();
            // Only when someone is watching — a banner in a CI log is noise.
            if (interactive) printBanner();
            // A spinner only where someone can see it; in CI the same steps are plain lines.
            const progress = makeProgress(interactive);
            try {
                const result = await runSetup({
                    onProgress: progress.step,
                    onAuthRetry: (message, attempt, max) =>
                        progress.warn(`${message}  (attempt ${attempt} of ${max})`),
                    onWarning: (message) => progress.warn(message),
                    promptPort: interactive ? pausingPort(inquirerPort, progress) : undefined,
                    confirmOverwrite: interactive
                        ? pausingConfirm(confirmOverwrite, progress)
                        : undefined,
                    confirmExclude: interactive
                        ? pausingConfirm(confirmExclude, progress)
                        : undefined,
                    url: options['url'] as string | undefined,
                    user: options['user'] as string | undefined,
                    password: options['password'] as string | undefined,
                    authToken: options['authToken'] as string | undefined,
                    agents: options['agent'] as string[] | undefined,
                    scope: options['global'] ? 'global' : 'folder',
                    skipMcp: Boolean(options['skipMcp']),
                    skipSkills: Boolean(options['skipSkills']),
                    skipVerify: Boolean(options['skipVerify']),
                    yes: Boolean(options['yes']),
                    force: Boolean(options['force'])
                });

                progress.done();
                writeOut(
                    renderSummary({
                        outcomes: result.outcomes,
                        versionControl: result.versionControl,
                        warnings: result.warnings,
                        connection: result.connection,
                        connectionReason: result.connectionReason
                    })
                );
                process.exitCode = result.exitCode;
            } catch (error) {
                // Without this the spinner spins forever under the error message — the run is
                // over and the terminal still says it is working.
                progress.fail();
                throw error;
            }
        });
}
