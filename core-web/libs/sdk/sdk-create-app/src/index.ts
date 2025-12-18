#!/usr/bin/env node

import chalk from 'chalk';
import { Command } from 'commander';
import { execa } from 'execa';
import ora from 'ora';
import { Result, Ok, Err } from 'ts-results';

import { DotCMSApi } from './api';
import {
    askCloudOrLocalInstance,
    askDirectory,
    askDotcmsCloudUrl,
    askFramework,
    askPasswordForDotcmsCloud,
    askUserNameForDotcmsCloud,
    prepareDirectory
} from './asks';
import { DOTCMS_HEALTH_API, DOTCMS_HOST, DOTCMS_USER } from './constants';
import { FailedToCreateFrontendProjectError, FailedToDownloadDockerComposeError } from './errors';
import { cloneFrontEndSample, downloadDockerCompose } from './git';
import {
    fetchWithRetry,
    getDotcmsApisByBaseUrl,
    getPortByFramework,
    getUVEConfigValue
} from './utils';

import type { SupportedFrontEndFrameworks } from './types';

// Supported values

const program = new Command();

program
    .name('dotcms-create-app')
    .description('dotCMS CLI for creating applications')
    .version('0.1.0-beta');

program
    .argument('<project-name>', 'Name of the project folder')
    .option('-f, --framework <framework>', 'Framework to use [nextjs,astro,angular,angular-ssr]')
    // directory flags
    .option('-d, --directory <path>', 'Project directory')

    // cloud / no-cloud
    .option('--local', 'Use local dotCMS instance using docker')

    // cloud options (if cloud selected)
    .option('--url <url>', 'DotCMS instance url (skip in case of local)')
    .option('-u, --username <username>', 'DotCMS instance username (skip in case of local)')
    .option('-p, --password <password>', 'DotCMS instance password (skip in case of local)')

    .action(async (projectName: string, options) => {
        // <-- Add beta notice here
        console.log(chalk.bgYellow.black(' ⚠️  Beta Version Notice  ⚠️ '));
        console.log(
            chalk.yellow(
                'This CLI is currently in beta. Features may change and some bugs may be present.\n'
            )
        );

        console.log(options);

        const { dir, directory } = options;
        const { url } = options;
        const { user, username } = options;
        const { pass, password } = options;
        const { framework, f } = options;

        let directoryInput: string;
        let finalDirectory: string;

        if (dir === undefined || directory === undefined) {
            directoryInput = await askDirectory();
            finalDirectory = await prepareDirectory(directoryInput, projectName);
        } else {
            directoryInput = dir || directory;
            finalDirectory = await prepareDirectory(directoryInput, projectName);
        }

        let selectedFramework: string;

        if (framework === undefined || f === undefined) {
            selectedFramework = await askFramework();
        } else {
            selectedFramework = framework || f;
        }

        const isCloudInstanceSelected: boolean =
            options.local === undefined ? await askCloudOrLocalInstance() : false;

        if (isCloudInstanceSelected) {
            let urlDotcmsInstance: string;
            let userNameDotCmsInstance: string;
            let passwordDotCmsInstance: string;

            if (url === undefined) await askDotcmsCloudUrl();
            else urlDotcmsInstance = url;

            if (user === undefined || username === undefined) await askUserNameForDotcmsCloud();
            else userNameDotCmsInstance = user || username;

            if (pass === undefined || password === undefined) await askPasswordForDotcmsCloud();
            else passwordDotCmsInstance = pass || password;

            const spinner = ora(`Scaffolding ${selectedFramework} application ...`).start();

            const created = await scaffoldFrontendProject({
                framework: selectedFramework as SupportedFrontEndFrameworks,
                directory: finalDirectory
            });

            if (!created.ok) {
                spinner.fail(`Failed to scaffold frontend project (${selectedFramework}).`);
                return;
            }

            spinner.succeed(`Frontend project (${selectedFramework}) scaffolded successfully.`);

            const healthApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_HEALTH_API;
            const emaConfigApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_EMA_CONFIG_API;
            const demoSiteApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_DEMO_SITE;
            const tokenApiUrl = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_TOKEN_API;

            spinner.start('Verifying if dotCMS is running...');

            const checkIfDotcmsIsRunning = await isDotcmsRunning(healthApiURL);

            if (!checkIfDotcmsIsRunning) {
                spinner.fail(
                    'dotCMS is not running on the following url ' +
                        urlDotcmsInstance +
                        '. Please check the url and try again.'
                );
                return;
            }

            spinner.succeed('dotCMS is running.');

            const dotcmsToken = await DotCMSApi.getAuthToken({
                payload: {
                    user: userNameDotCmsInstance,
                    password: passwordDotCmsInstance,
                    expirationDays: '30',
                    label: 'token for frontend app'
                },
                url: tokenApiUrl
            });

            if (!dotcmsToken.ok) {
                spinner.fail('Failed to get authentication token from Dotcms.');
                return;
            }

            const demoSite = await DotCMSApi.getDemoSiteIdentifier({
                siteName: 'demo.dotcms.com',
                authenticationToken: dotcmsToken.val,
                url: demoSiteApiURL
            });

            if (!demoSite.ok) {
                spinner.fail('Failed to get demo site identifier from Dotcms.');
                return;
            }

            const setUpUVE = await DotCMSApi.setupUVEConfig({
                payload: {
                    configuration: {
                        hidden: false,
                        value: getUVEConfigValue(
                            `http://localhost:${getPortByFramework(selectedFramework as SupportedFrontEndFrameworks)}`
                        )
                    }
                },
                siteId: demoSite.val.entity.identifier,
                authenticationToken: dotcmsToken.val,
                url: emaConfigApiURL
            });

            if (!setUpUVE.ok) {
                spinner.fail('Failed to setup UVE configuration in Dotcms.');
                return;
            }

            spinner.succeed('Project setup completed successfully.');

            console.log('\n');
            console.log(chalk.cyanBright('📄 Update your frontend environment variables:\n'));

            // ENV BLOCK — nicely spaced + grouped
            console.log(chalk.white('──────────────────────────────────────────────'));
            console.log(chalk.white('🌐  Host (site):      ') + chalk.green(urlDotcmsInstance));
            console.log(
                chalk.white('🏷️  Site ID:          ') + chalk.green(demoSite.val.entity.identifier)
            );
            console.log(chalk.white('🔐  API Token:        ') + chalk.green(dotcmsToken.val));
            console.log(chalk.white('──────────────────────────────────────────────\n'));

            // INSTALL DEPENDENCIES
            console.log(chalk.magentaBright('📦 Install frontend dependencies:'));
            console.log(chalk.white(`$ cd ${finalDirectory}`));
            console.log(chalk.white('$ npm install\n'));

            // START DEV SERVER
            console.log(chalk.blueBright('💻 Start your frontend development server:'));
            console.log(chalk.white('$ npm run dev\n'));

            // FINAL MESSAGE
            console.log(
                chalk.greenBright(
                    "🎉 You're all set! Start building your app with dotCMS + your chosen frontend framework.\n"
                )
            );

            return;
        }

        // console.log(chalk.green("✔ Starting DotCMS app setup...\n"));
        // const spinner = ora(`Scaffolding ${framework} application ...`).start();

        const spinner = ora(`Scaffolding ${selectedFramework} application ...`).start();

        // STEP 1 — Scaffold front-end
        const created = await scaffoldFrontendProject({
            framework: selectedFramework as SupportedFrontEndFrameworks,
            directory: finalDirectory
        });

        if (!created.ok) {
            spinner.fail(`Failed to scaffold frontend project (${selectedFramework}).`);
            return;
        }

        spinner.succeed(`Frontend project (${selectedFramework}) scaffolded successfully.`);

        spinner.start('Setting up dotCMS with Docker Compose...');

        // STEP 2 — Download docker-compose
        const downloaded = await downloadTheDockerCompose({
            directory: finalDirectory
        });
        if (!downloaded.ok) {
            spinner.fail('Failed to download Docker Compose file.');
            return;
        }

        spinner.succeed('Docker Compose Download completed.');

        spinner.start(
            'Starting dotCMS with Docker Compose (This may take sometime enjoy a coffee ☕️ )...'
        );

        // STEP 3 — Run docker-compose
        const ran = await runDockerCompose({ directory: finalDirectory });
        if (!ran.ok) {
            spinner.fail(
                'Failed to start dotCMS with Docker Compose.Please make sure docker is installed and running.'
            );
            return;
        }

        spinner.succeed('dotCMS containers started successfully.');

        spinner.start('Verifying if dotCMS is running...');

        const checkIfDotcmsIsRunning = await isDotcmsRunning();

        if (!checkIfDotcmsIsRunning) {
            spinner.fail('dotCMS is not running. Please check the docker containers.');
            return;
        }

        spinner.succeed('dotCMS is running.');

        const dotcmsToken = await DotCMSApi.getAuthToken({
            payload: {
                user: DOTCMS_USER.username,
                password: DOTCMS_USER.password,
                expirationDays: '30',
                label: 'token for frontend app'
            }
        });

        if (!dotcmsToken.ok) {
            spinner.fail('Failed to get authentication token from Dotcms.');
            return;
        }

        const demoSite = await DotCMSApi.getDemoSiteIdentifier({
            siteName: 'demo.dotcms.com',
            authenticationToken: dotcmsToken.val
        });

        if (!demoSite.ok) {
            spinner.fail('Failed to get demo site identifier from Dotcms.');
            return;
        }

        const setUpUVE = await DotCMSApi.setupUVEConfig({
            payload: {
                configuration: {
                    hidden: false,
                    value: getUVEConfigValue(
                        `http://localhost:${getPortByFramework(selectedFramework as SupportedFrontEndFrameworks)}`
                    )
                }
            },
            siteId: demoSite.val.entity.identifier,
            authenticationToken: dotcmsToken.val
        });

        if (!setUpUVE.ok) {
            spinner.fail('Failed to setup UVE configuration in Dotcms.');
            return;
        }

        spinner.succeed('Project setup completed successfully.');

        // ADD FINAL INSTRUCTIONS
        console.log('\n');
        console.log(chalk.cyanBright('📄 Update your frontend environment variables:\n'));

        // ENV BLOCK — nicely spaced + grouped
        console.log(chalk.white('──────────────────────────────────────────────'));
        console.log(chalk.white('🌐  Host (site):      ') + chalk.green(DOTCMS_HOST));
        console.log(
            chalk.white('🏷️  Site ID:          ') + chalk.green(demoSite.val.entity.identifier)
        );
        console.log(chalk.white('🔐  API Token:        ') + chalk.green(dotcmsToken.val));
        console.log(chalk.white('──────────────────────────────────────────────\n'));

        // INSTALL DEPENDENCIES
        console.log(chalk.magentaBright('📦 Install frontend dependencies:'));
        console.log(chalk.white(`$ cd ${finalDirectory}`));
        console.log(chalk.white('$ npm install\n'));

        // START DEV SERVER
        console.log(chalk.blueBright('💻 Start your frontend development server:'));
        console.log(chalk.white('$ npm run dev\n'));

        // FINAL MESSAGE
        console.log(
            chalk.greenBright(
                "🎉 You're all set! Start building your app with dotCMS + your chosen frontend framework.\n"
            )
        );
    });

export async function createApp() {
    program.parse();
}

/* -------------------------------------------------------
 * STEP FUNCTIONS (no spinner, only chalk)
 * -----------------------------------------------------*/

async function scaffoldFrontendProject({
    framework,
    directory
}: {
    framework: SupportedFrontEndFrameworks;
    directory: string;
}): Promise<Result<void, FailedToCreateFrontendProjectError>> {
    try {
        await cloneFrontEndSample({ directory, framework });
        return Ok(undefined);
    } catch (err) {
        console.log(
            chalk.red(
                `❌ Failed to create ${framework} project. Please check git installation and network connection.` +
                    JSON.stringify(err)
            )
        );
        return Err(new FailedToCreateFrontendProjectError(framework));
    }
}

async function downloadTheDockerCompose({
    directory
}: {
    directory: string;
}): Promise<Result<void, FailedToDownloadDockerComposeError>> {
    try {
        // console.log(chalk.cyan("📥 Downloading docker-compose.yml..."));

        await downloadDockerCompose(directory);

        // console.log(chalk.green(`✔ docker-compose.yml downloaded successfully!\n`));

        return Ok(undefined);
    } catch (err) {
        console.log(chalk.red('❌ Failed to download docker-compose.yml.' + JSON.stringify(err)));
        return Err(new FailedToDownloadDockerComposeError());
    }
}

async function runDockerCompose({
    directory
}: {
    directory: string;
}): Promise<Result<void, Error>> {
    try {
        // console.log(chalk.cyan("🐳 Starting Docker containers... (This might take some time)"));

        await execa('docker', ['compose', 'up', '-d'], { cwd: directory });
        await execa('docker', ['ps'], { cwd: directory });

        // console.log(chalk.green("✔ Docker containers started successfully!\n"));

        return Ok(undefined);
    } catch (err) {
        return Err(err as Error);
    }
}

async function isDotcmsRunning(url?: string): Promise<boolean> {
    try {
        // console.log(chalk.cyan("Waiting for DotCMS to be up ...."));
        const res = await fetchWithRetry(url ?? DOTCMS_HEALTH_API, 20, 5000);
        if (res && res.status === 200) {
            // console.log(chalk.green("✔ DotCMS container started sucessfully!\n"));
            return true;
        }
        return false;
    } catch (err) {
        console.log(
            chalk.red(
                '❌ Failed to run docker-compose. Please make sure if docker is installed and running inside your machine.',
                +JSON.stringify(err)
            )
        );
        return false;
    }
}

createApp();
