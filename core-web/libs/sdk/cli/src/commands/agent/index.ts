
import { runSetup } from './setup';
import { TARGET_IDS } from './targets/registry';

import { renderSummary, writeOut } from '../../shared/ui';

import type { Command } from 'commander';

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
            const result = await runSetup({
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

            writeOut(
                renderSummary({
                    outcomes: result.outcomes,
                    connection: result.connection,
                    connectionReason: result.connectionReason
                })
            );
            process.exitCode = result.exitCode;
        });
}
