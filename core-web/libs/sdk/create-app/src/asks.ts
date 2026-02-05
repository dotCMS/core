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
 */
export async function prepareDirectory(basePath: string, projectName: string) {
    const targetPath = path.resolve(basePath, projectName);

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

    // Directory not empty → warn user
    const ans = await inquirer.prompt([
        {
            type: 'confirm',
            name: 'confirm',
            message: `⚠️  Directory "${targetPath}" is not empty. All files inside will be deleted. Continue?`,
            default: false
        }
    ]);

    if (!ans.confirm) {
        console.log('❌ Operation cancelled.');
        process.exit(1);
    }

    // Empty directory
    await fs.emptyDir(targetPath);

    return targetPath;
}
