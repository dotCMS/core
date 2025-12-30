import fs from 'fs-extra';
import inquirer from 'inquirer';

import path from 'path';

import { FRAMEWORKS_CHOICES } from './constants';

import type { SupportedFrontEndFrameworks } from './types';

/**
 * Ask interactively if framework not specified
 */
export async function askFramework(): Promise<SupportedFrontEndFrameworks> {
    const ans = await inquirer.prompt<{ frameworks: SupportedFrontEndFrameworks[] }>([
        {
            type: 'checkbox',
            name: 'frameworks',
            message: 'Select the frontend framework:',
            choices: FRAMEWORKS_CHOICES,
            validate(selected) {
                if (selected.length === 0) return 'Please select at least one framework.';
                return true;
            }
        }
    ]);

    // Return the first selected framework (checkbox returns array)
    return ans.frameworks[0];
}

/**
 * Ask user where to create the project
 */
export async function askDirectory() {
    const ans = await inquirer.prompt([
        {
            type: 'input',
            name: 'directory',
            message: 'Project directory:',
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
            message: 'DotCMS instance URL:',
            default: `https://demo.dotcms.com`
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
            default: `admin@dotcms.com`
        }
    ]);
    return ans.username;
}

/**
 * Ask user the username of the dotCMS instance
 */
export async function askPasswordForDotcmsCloud() {
    const ans = await inquirer.prompt([
        {
            type: 'input',
            name: 'password',
            message: 'Password:',
            default: `admin`
        }
    ]);
    return ans.password;
}

/**
 * Ask if the user wants to use cloud instance or local dotcms
 * Example:
 * user enters: "y/n"
 */
export async function askCloudOrLocalInstance(): Promise<boolean> {
    const ans = await inquirer.prompt([
        {
            type: 'confirm',
            name: 'confirm',
            message: `Running dotCMS in the cloud? If not, no worries — select No to spin up dotCMS using Docker.`,
            default: false
        }
    ]);
    return ans.confirm;
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
