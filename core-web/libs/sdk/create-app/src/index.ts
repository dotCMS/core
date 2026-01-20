#!/usr/bin/env node

import chalk from 'chalk';
import { Command } from 'commander';
import { execa } from 'execa';
import ora from 'ora';
import { Result, Ok, Err } from 'ts-results';

import path from 'path';

import { DotCMSApi } from './api';
import {
    askCloudOrLocalInstance,
    askDirectory,
    askDotcmsCloudUrl,
    askFramework,
    askPasswordForDotcmsCloud,
    askProjectName,
    askUserNameForDotcmsCloud,
    prepareDirectory
} from './asks';
import { DOTCMS_HEALTH_API, DOTCMS_USER } from './constants';
import { FailedToCreateFrontendProjectError, FailedToDownloadDockerComposeError } from './errors';
import {
    cloneFrontEndSample,
    downloadDockerCompose,
    moveDockerComposeBack,
    moveDockerComposeOneLevelUp
} from './git';
import {
    displayDependencies,
    fetchWithRetry,
    finalStepsForAngularAndAngularSSR,
    finalStepsForAstro,
    finalStepsForNextjs,
    getDotcmsApisByBaseUrl,
    getPortByFramework,
    getUVEConfigValue,
    installDependenciesForProject
} from './utils';

import type { SupportedFrontEndFrameworks } from './types';

// Supported values

const program = new Command();

program
    .name('dotcms-create-app')
    .description('dotCMS CLI for creating applications')
    .version('0.1.0-beta');

program
    .option('-n, --name <projectName>', 'Name of the project')
    .option('-f, --framework <framework>', 'Framework to use [nextjs,astro,angular,angular-ssr]')
    // directory flags
    .option('-d, --directory <path>', 'Project directory')

    // cloud / no-cloud
    .option('--local', 'Use local dotCMS instance using docker')

    // cloud options (if cloud selected)
    .option('--url <url>', 'DotCMS instance url (skip in case of local)')
    .option('-u, --username <username>', 'DotCMS instance username (skip in case of local)')
    .option('-p, --password <password>', 'DotCMS instance password (skip in case of local)')

    .action(async (options) => {
        // <-- Add beta notice here
        console.log(chalk.white('\nWelcome to dotCMS CLI\n'));
        console.log(chalk.bgGrey.white('\n ℹ️  Beta: Features may change \n'));

        const { dir, directory } = options;
        const { name, projectName } = options;
        const { url } = options;
        const { user, username } = options;
        const { pass, password } = options;
        const { framework, f } = options;

        let projectNameFinal: string;
        let directoryInput: string;
        let finalDirectory: string;
        let isCloudInstanceSelected: boolean;
        let selectedFramework: string;

        if (name === undefined && projectName === undefined) {
            projectNameFinal = await askProjectName();
        } else {
            projectNameFinal = name || projectName;
        }

        if (dir === undefined && directory === undefined) {
            directoryInput = await askDirectory();
            finalDirectory = await prepareDirectory(directoryInput, projectNameFinal);
        } else {
            directoryInput = dir || directory;
            finalDirectory = await prepareDirectory(directoryInput, projectNameFinal);
        }

        if (framework === undefined && f === undefined) {
            selectedFramework = await askFramework();
        } else {
            selectedFramework = framework || f;
        }
        if (options.local === undefined) {
            isCloudInstanceSelected = await askCloudOrLocalInstance();
        } else {
            isCloudInstanceSelected = !JSON.parse(options.local);
        }

        if (isCloudInstanceSelected) {
            const urlDotcmsInstance = url === undefined ? await askDotcmsCloudUrl() : url;

            const userNameDotCmsInstance =
                user === undefined && username === undefined
                    ? await askUserNameForDotcmsCloud()
                    : user || username;

            const passwordDotCmsInstance =
                pass === undefined && password === undefined
                    ? await askPasswordForDotcmsCloud()
                    : pass || password;
            // mark here
            //
            const healthApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_HEALTH_API;
            const emaConfigApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_EMA_CONFIG_API;
            const demoSiteApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_DEMO_SITE;
            const tokenApiUrl = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_TOKEN_API;

            const spinner = ora(`⏳ Connecting to dotCMS...`).start();

            const checkIfDotcmsIsRunning = await isDotcmsRunning(healthApiURL);

            if (!checkIfDotcmsIsRunning) {
                spinner.fail(
                    'dotCMS is not running on the following url ' +
                        urlDotcmsInstance +
                        '. Please check the url and try again.'
                );
                return;
            }

            spinner.succeed('Connected to dotCMS sucessfully');

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
            } else {
                spinner.succeed('Generated API autentication token');
            }

            const demoSite = await DotCMSApi.getDemoSiteIdentifier({
                siteName: 'demo.dotcms.com',
                authenticationToken: dotcmsToken.val,
                url: demoSiteApiURL
            });

            if (!demoSite.ok) {
                spinner.fail('Failed to get demo site identifier from Dotcms.');
                return;
            } else {
                spinner.succeed(`Reterived site: Demo Site (${demoSite.val.entity.identifier})`);
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
            } else {
                spinner.succeed(`Configured the Universal Visual Editor`);
            }

            spinner.start(`⏳ Scaffolding ${selectedFramework} project...`);
            const created = await scaffoldFrontendProject({
                framework: selectedFramework as SupportedFrontEndFrameworks,
                directory: finalDirectory
            });

            if (!created.ok) {
                spinner.fail(`Failed to scaffold frontend project (${selectedFramework}).`);
                return;
            }

            // TODO need to insert here the dependices step
            spinner.succeed(`Frontend project (${selectedFramework}) scaffolded successfully.`);
            spinner.start(
                `📦 Installing dependencies...\n\n ${displayDependencies(selectedFramework as SupportedFrontEndFrameworks)}`
            );
            const result = await installDependenciesForProject(finalDirectory);
            if (!result)
                spinner.fail(
                    `Failed to install dependencies.Please check if npm is installed in your system`
                );
            else spinner.succeed(`Dependencies installed`);
            console.log('\n\n');
            spinner.stop();
            console.log(chalk.white(`✅ Project setup complete!`));
            const relativePath = path.relative(process.cwd(), finalDirectory) || '.';
            switch (selectedFramework) {
                case 'nextjs': {
                    finalStepsForNextjs({
                        projectPath: relativePath,
                        token: dotcmsToken.val,
                        siteId: demoSite.val.entity.identifier,
                        urlDotCMSInstance: urlDotcmsInstance
                    });
                    break;
                }
                case 'angular': {
                    finalStepsForAngularAndAngularSSR({
                        projectPath: relativePath,
                        token: dotcmsToken.val,
                        siteId: demoSite.val.entity.identifier,
                        urlDotCMSInstance: urlDotcmsInstance
                    });
                    break;
                }
                case 'angular-ssr': {
                    finalStepsForAngularAndAngularSSR({
                        projectPath: relativePath,
                        token: dotcmsToken.val,
                        siteId: demoSite.val.entity.identifier,
                        urlDotCMSInstance: urlDotcmsInstance
                    });
                    break;
                }
                case 'astro': {
                    finalStepsForAstro({
                        projectPath: relativePath,
                        token: dotcmsToken.val,
                        siteId: demoSite.val.entity.identifier,
                        urlDotCMSInstance: urlDotcmsInstance
                    });
                    break;
                }
            }
            return;
        }

        // console.log(chalk.green("✔ Starting DotCMS app setup...\n"));
        // const spinner = ora(`Scaffolding ${framework} application ...`).start();

        const spinner = ora(`Starting dotCMS with Docker`).start();

        // STEP 2 — Download docker-compose
        const downloaded = await downloadTheDockerCompose({
            directory: finalDirectory
        });
        if (!downloaded.ok) {
            spinner.fail('Failed to download Docker Compose file.');
            return;
        }

        // STEP 3 — Run docker-compose
        const ran = await runDockerCompose({ directory: finalDirectory });
        if (!ran.ok) {
            spinner.fail(
                'Failed to start dotCMS ensure docker is running and ports 8082, 8443, 9200, and 9600 are free.'
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
        spinner.succeed('dotCMS is running locally at http://localhost:8082');
        spinner.succeed('Default credentials: admin@dotcms.com / admin');

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
        } else {
            spinner.succeed('Generated API autentication token');
        }

        const demoSite = await DotCMSApi.getDemoSiteIdentifier({
            siteName: 'default',
            authenticationToken: dotcmsToken.val
        });
        if (!demoSite.ok) {
            spinner.fail('Failed to get demo site identifier from Dotcms.');
            return;
        } else {
            spinner.succeed(`Reterived site: Demo Site (${demoSite.val.entity.identifier})`);
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
        } else {
            spinner.succeed(`Configured the Universal Visual Editor`);
        }
        // STEP 1 — Scaffold front-end
        spinner.start(`⏳ Scaffolding ${selectedFramework} project...`);
        // this is required because git needs empty folder to start
        moveDockerComposeOneLevelUp(finalDirectory);
        const created = await scaffoldFrontendProject({
            framework: selectedFramework as SupportedFrontEndFrameworks,
            directory: finalDirectory
        });
        moveDockerComposeBack(finalDirectory);

        if (!created.ok) {
            spinner.fail(`Failed to scaffold frontend project (${selectedFramework}).`);
            return;
        }

        // TODO need to insert here the dependices step
        spinner.succeed(`Frontend project (${selectedFramework}) scaffolded successfully.`);
        spinner.start(
            `📦 Installing dependencies...\n\n ${displayDependencies(selectedFramework as SupportedFrontEndFrameworks)}`
        );
        const result = await installDependenciesForProject(finalDirectory);
        if (!result)
            spinner.fail(
                `Failed to install dependencies.Please check if npm is installed in your system`
            );
        else spinner.succeed(`Dependencies installed`);
        console.log('\n\n');
        spinner.stop();
        console.log(chalk.white(`✅ Project setup complete!`));
        const relativePath = path.relative(process.cwd(), finalDirectory) || '.';
        switch (selectedFramework) {
            case 'nextjs': {
                finalStepsForNextjs({
                    projectPath: relativePath,
                    token: dotcmsToken.val,
                    siteId: demoSite.val.entity.identifier,
                    urlDotCMSInstance: 'http://localhost:8082'
                });
                break;
            }
            case 'angular': {
                finalStepsForAngularAndAngularSSR({
                    projectPath: relativePath,
                    token: dotcmsToken.val,
                    siteId: demoSite.val.entity.identifier,
                    urlDotCMSInstance: 'http://localhost:8082'
                });
                break;
            }
            case 'angular-ssr': {
                finalStepsForAngularAndAngularSSR({
                    projectPath: relativePath,
                    token: dotcmsToken.val,
                    siteId: demoSite.val.entity.identifier,
                    urlDotCMSInstance: 'http://localhost:8082'
                });
                break;
            }
            case 'astro': {
                finalStepsForAstro({
                    projectPath: relativePath,
                    token: dotcmsToken.val,
                    siteId: demoSite.val.entity.identifier,
                    urlDotCMSInstance: 'http://localhost:8082'
                });
                break;
            }
        }
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
        const res = await fetchWithRetry(url ?? DOTCMS_HEALTH_API, 40, 5000);
        if (res && res.status === 200) {
            // console.log(chalk.green("✔ DotCMS container started sucessfully!\n"));
            return true;
        }
        return false;
    } catch {
        return false;
    }
}

createApp();
