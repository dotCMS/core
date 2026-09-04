import chalk from 'chalk';
import fs from 'fs-extra';
import inquirer from 'inquirer';

import path from 'path';

import { FRAMEWORKS_CHOICES } from './constants';
import { validateProjectName, validateUrl } from './utils/validation';

import type { SupportedFrontEndFrameworks } from './types';

/**
 * Ask interactively if framework not specified
 */
export async function askFramework(): Promise<SupportedFrontEndFrameworks> {
    const ans = await inquirer.prompt<{ frameworks: SupportedFrontEndFrameworks }>([
        {
            type: 'select',
            name: 'frameworks',
            message: 'Select your frontend framework:',
            choices: FRAMEWORKS_CHOICES
        }
    ]);

    // Return the first selected framework (checkbox returns array)
    return ans.frameworks;
}
/**
 * Ask user name of the project
 */
export async function askProjectName() {
    const ans = await inquirer.prompt([
        {
            type: 'input',
            name: 'projectName',
            message: 'What is your project name ?',
            default: `my-dotcms-app`,
            validate: (input: string) => {
                try {
                    validateProjectName(input);
                    return true;
                } catch (error) {
                    return error instanceof Error ? error.message : String(error);
                }
            }
        }
    ]);
    return ans.projectName;
}

/**
 * Ask user where to create the project
 */
export async function askDirectory() {
    const ans = await inquirer.prompt([
        {
            type: 'input',
            name: 'directory',
            message: 'Where should we create your project?',
            default: `.`
        }
    ]);
    return ans.directory;
}

/**
 * Ask user the url of the dotCMS instance
 */
export async function askDotcmsCloudUrl() {
    const ans = await inquirer.prompt([
        {
            type: 'input',
            name: 'url',
            message: 'dotCMS instance URL:',
            default: `https://demo.dotcms.com`,
            validate: (input: string) => {
                try {
                    validateUrl(input);
                    return true;
                } catch (error) {
                    return error instanceof Error ? error.message : String(error);
                }
            }
        }
    ]);
    return ans.url;
}

/**
 * Ask user the username of the dotCMS instance
 */
export async function askUserNameForDotcmsCloud() {
    const ans = await inquirer.prompt([
        {
            type: 'input',
            name: 'username',
            message: 'Username:',
            default: `admin@dotcms.com`,
            validate: (input: string) => {
                if (!input || input.trim() === '') {
                    return 'Username cannot be empty';
                }
                return true;
            }
        }
    ]);
    return ans.username;
}

/**
 * Ask user the password of the dotCMS instance
 */
export async function askPasswordForDotcmsCloud() {
    const ans = await inquirer.prompt([
        {
            type: 'password',
            name: 'password',
            mask: '•',
            message: 'Password:',
            default: `admin`,
            validate: (input: string) => {
                if (!input || input.trim() === '') {
                    return 'Password cannot be empty';
                }
                return true;
            }
        }
    ]);
    return ans.password;
}

/**
 * Ask if the user wants to use cloud instance or local dotcms
 * Example:
 * user enters: "y/n"
 */
// export async function askCloudOrLocalInstance(): Promise<boolean> {
//     const ans = await inquirer.prompt([
//         {
//             type: 'confirm',
//             name: 'confirm',
//             message: `Running dotCMS in the cloud? If not, no worries — select No to spin up dotCMS using Docker.`,
//             default: false
//         }
//     ]);
//     return ans.confirm;
// }
//
/**
 * Ask if the user has cloud or want to set local
 */
export async function askCloudOrLocalInstance(): Promise<boolean> {
    const ans = await inquirer.prompt<{ isCloud: boolean }>([
        {
            type: 'select',
            name: 'isCloud',
            message: 'Do you have an existing dotCMS instance?',
            choices: [
                { name: 'Yes - I have a dotCMS instance URL', value: true },
                { name: 'No - Spin up dotCMS locally with Docker', value: false }
            ]
        }
    ]);

    // Return the first selected framework (checkbox returns array)
    return ans.isCloud;
}

/**
 * Prepare final project directory
 * Example:
 * user enters: "."
 * projectName: "my-app"
 * final path becomes "./my-app"
 *
 * @remarks
 * - Prevents nested directories when basePath already ends with projectName
 * - Example: basePath="/tmp/my-app" + projectName="my-app" → "/tmp/my-app" (not "/tmp/my-app/my-app")
 * - Handles both absolute and relative paths correctly
 */
export async function prepareDirectory(basePath: string, projectName: string) {
    // Resolve basePath to absolute path for consistent comparison
    const resolvedBasePath = path.resolve(basePath);
    const basePathDirName = path.basename(resolvedBasePath);

    // Check if basePath already ends with the project name
    // This prevents nested directories like "/tmp/my-app/my-app"
    let targetPath: string;
    if (basePathDirName === projectName) {
        // User specified full path including project name (e.g., "-d /tmp/my-app" with projectName="my-app")
        targetPath = resolvedBasePath;
    } else {
        // User specified parent directory (e.g., "-d /tmp" with projectName="my-app")
        targetPath = path.resolve(resolvedBasePath, projectName);
    }

    // If path doesn't exist → create
    if (!fs.existsSync(targetPath)) {
        fs.mkdirSync(targetPath, { recursive: true });
        return targetPath;
    }

    // Directory exists → check if empty
    const files = fs.readdirSync(targetPath);

    if (files.length === 0) {
        return targetPath; // empty → OK
    }

    // A docker-compose.yml here means a previous run left a stack behind — possibly one that is
    // still running. Emptying the directory would delete the only file that can tear it down,
    // stranding containers the user then has to hunt for by hand. That is the CLI destroying its
    // own recovery path, which is the shape of the failure in #37262, so it is refused outright
    // rather than folded into the blanket "all files will be deleted" confirmation.
    const composePath = path.join(targetPath, 'docker-compose.yml');
    const hasComposeFile = fs.existsSync(composePath);

    // Directory not empty → warn user
    const ans = await inquirer.prompt([
        {
            type: 'confirm',
            name: 'confirm',
            message: hasComposeFile
                ? `⚠️  Directory "${targetPath}" contains a docker-compose.yml from a previous run. Everything EXCEPT that file will be deleted. Continue?`
                : `⚠️  Directory "${targetPath}" is not empty. All files inside will be deleted. Continue?`,
            default: false
        }
    ]);

    if (!ans.confirm) {
        console.log('❌ Operation cancelled.');
        process.exit(1);
    }

    if (hasComposeFile) {
        const preserved = await fs.readFile(composePath);
        await fs.emptyDir(targetPath);
        await fs.writeFile(composePath, preserved);
    } else {
        await fs.emptyDir(targetPath);
    }

    return targetPath;
}

/**
 * Asked when a healthy dotCMS is already listening on 8082.
 *
 * The earlier version said "A dotCMS instance is already running on port 8082. What would you
 * like to do?" and offered only reuse or quit. That states a fact and then abandons the user:
 * it never says WHAT is running, and anyone who did not want that instance had to leave the CLI
 * and run docker by hand. Replacing it is the documented recovery for a bricked instance
 * (#37262), so the CLI can now do it.
 *
 * `canReplace` is false when nothing identifiable owns the ports — something started outside
 * compose is not ours to destroy, so the option is withheld rather than offered and then failed.
 */
export async function askPortConflictAction({
    description,
    canReplace
}: {
    description: string;
    canReplace: boolean;
}): Promise<'reuse' | 'replace' | 'cancel'> {
    console.log(
        '\n' +
            chalk.yellow('⚠  Found a dotCMS already running at ') +
            chalk.cyan('http://localhost:8082') +
            '\n' +
            chalk.gray(`   ${description}`) +
            '\n'
    );

    // One line per choice. A `\n` inside a choice name breaks inquirer's line accounting and
    // the list renders blank — the hint belongs in `description`, which it prints under the
    // highlighted option.
    const choices: {
        name: string;
        value: 'reuse' | 'replace' | 'cancel';
        description: string;
    }[] = [
        {
            name: 'Use this instance for my project',
            value: 'reuse',
            description: 'Fastest. Keeps its existing content.'
        }
    ];

    if (canReplace) {
        choices.push({
            name: 'Replace it with a clean instance',
            value: 'replace',
            description: 'Stops it and DELETES its data, then starts fresh.'
        });
    }

    choices.push({
        name: 'Cancel',
        value: 'cancel',
        description: 'Change nothing and exit.'
    });

    const { action } = await inquirer.prompt([
        // `select`, NOT `list`. Inquirer 13 is built on @inquirer/prompts, where the type is
        // `select`; `list` is the inquirer 8/9 name and is not registered, so the message renders
        // and the choices silently do not. Every other prompt in this file already uses `select`.
        { type: 'select', name: 'action', message: 'How would you like to continue?', choices }
    ]);

    return action;
}
