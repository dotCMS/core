#!/usr/bin/env node
/**
 * CLI entry point for dotcli.
 *
 * Usage:
 *   dotcli <command> [options]
 *
 * Commands:
 *   init              Initialize a dotcli project
 *   auth              Manage server instances and authentication
 *   content           Pull, push, and manage contentlets
 *   cache             Manage local caches
 *   status            Show changed files across all entities
 */
import { defineCommand, runMain } from 'citty';

import { authCommand } from './commands/auth';
import { cacheCommand } from './commands/cache';
import { contentCommand } from './commands/content/index';
import { initCommand } from './commands/init';

const main = defineCommand({
    meta: {
        name: 'dotcli',
        version: '0.0.1',
        description: 'dotCMS CLI for Content-as-Code'
    },
    subCommands: {
        init: initCommand,
        auth: authCommand,
        content: contentCommand,
        cache: cacheCommand
    }
});

runMain(main);
