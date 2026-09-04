import { Command } from 'commander';

import { registerAgentCommand } from './commands/agent';
import { CliError, UsageError } from './shared/errors';

const program = new Command();

program
    .name('dotcms')
    .description('The dotCMS command line tool')
    .showHelpAfterError();

registerAgentCommand(program);

program.parseAsync(process.argv).catch((error: unknown) => {
    // There is no verbose mode, so a message here is the whole diagnostic surface (FR-032a).
    // An unhandled internal error must never reach the developer (FR-032).
    if (error instanceof CliError) {
        console.error(`\n${error.message}\n`);
        process.exitCode = error instanceof UsageError ? 2 : 1;
        return;
    }
    console.error(`\nSomething went wrong: ${(error as Error)?.message ?? String(error)}\n`);
    process.exitCode = 1;
});
