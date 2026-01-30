#!/usr/bin/env node

import cfonts from 'cfonts';
import chalk from 'chalk';
import { Command } from 'commander';
import { execa } from 'execa';
import ora, { Ora } from 'ora';
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
import {
    validateAndNormalizeFramework,
    validateConflictingParameters,
    validateProjectName,
    validateUrl
} from './utils/validation';

import type { DotCmsCliOptions, SupportedFrontEndFrameworks } from './types';

// Supported values

const program = new Command();

program
    .name('dotcms-create-app')
    .description('dotCMS CLI for creating applications')
    .version('0.1.0-beta');

program
    .argument('[projectName]', 'Name of the project')
    .option('-f, --framework <framework>', 'Framework to use [nextjs,astro,angular,angular-ssr]')
    // directory flags
    .option('-d, --directory <path>', 'Project directory')

    // cloud / no-cloud
    .option('--local', 'Use local dotCMS instance using docker')

    // cloud options (if cloud selected)
    .option('--url <url>', 'DotCMS instance url (skip in case of local)')
    .option('-u, --username <username>', 'DotCMS instance username (skip in case of local)')
    .option('-p, --password <password>', 'DotCMS instance password (skip in case of local)')

    .action(async (projectName: string, options: DotCmsCliOptions) => {
        // welcome cli
        printWelcomeScreen();

        try {
            // ✅ VALIDATE ALL CLI FLAGS IMMEDIATELY - BEFORE ANY INTERACTIVE PROMPTS
            const validatedFramework = validateAndNormalizeFramework(options.framework);
            validateUrl(options.url);
            validateConflictingParameters(options);
            validateProjectName(projectName);

            const projectNameFinal = projectName ?? (await askProjectName());
            // Validate project name from interactive prompt as well
            validateProjectName(projectNameFinal);
            const directoryInput = options.directory ?? (await askDirectory());
            const finalDirectory = await prepareDirectory(directoryInput, projectNameFinal);
            const selectedFramework = validatedFramework ?? (await askFramework());
            const isCloudInstanceSelected =
                options.local === undefined ? await askCloudOrLocalInstance() : !options.local;

            if (isCloudInstanceSelected) {
                const urlDotcmsInstance = options.url ?? (await askDotcmsCloudUrl());
                const userNameDotCmsInstance =
                    options.username ?? (await askUserNameForDotcmsCloud());
                const passwordDotCmsInstance =
                    options.password ?? (await askPasswordForDotcmsCloud());

                const healthApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_HEALTH_API;
                const emaConfigApiURL =
                    getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_EMA_CONFIG_API;
                const demoSiteApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_DEMO_SITE;
                const tokenApiUrl = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_TOKEN_API;

                const spinner = ora(`⏳ Connecting to dotCMS...`).start();

                const checkIfDotcmsIsRunning = await isDotcmsRunning(healthApiURL, 5);

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
                    spinner.succeed(
                        `Reterived site: Demo Site (${demoSite.val.entity.identifier})`
                    );
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
                await startScaffoldingFrontEnd({ spinner, selectedFramework, finalDirectory });
                console.log(chalk.white(`✅ Project setup complete!`));
                const relativePath = path.relative(process.cwd(), finalDirectory) || '.';
                displayFinalSteps({
                    host: urlDotcmsInstance,
                    relativePath,
                    token: dotcmsToken.val,
                    siteId: demoSite.val.entity.identifier,
                    selectedFramework: selectedFramework
                });
                return;
            }

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
                siteName: 'demo.dotcms.com',
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
            // required since git requires empty directory
            moveDockerComposeOneLevelUp(finalDirectory);
            await startScaffoldingFrontEnd({ spinner, selectedFramework, finalDirectory });
            moveDockerComposeBack(finalDirectory);
            console.log(chalk.white(`✅ Project setup complete!`));
            const relativePath = path.relative(process.cwd(), finalDirectory) || '.';
            displayFinalSteps({
                host: 'http://localhost:8082',
                relativePath,
                token: dotcmsToken.val,
                siteId: demoSite.val.entity.identifier,
                selectedFramework: selectedFramework
            });
        } catch (error) {
            // Handle validation and other errors gracefully
            if (error instanceof Error) {
                console.error(error.message);
            } else {
                console.error(chalk.red('❌ An unexpected error occurred'));
            }
            process.exit(1);
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
        // console.log(chalk.cyan(""));

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

async function isDotcmsRunning(url?: string, retries = 60): Promise<boolean> {
    try {
        // console.log(chalk.cyan("Waiting for DotCMS to be up ...."));
        const res = await fetchWithRetry(url ?? DOTCMS_HEALTH_API, retries, 5000);
        if (res && res.status === 200) {
            // console.log(chalk.green("✔ DotCMS container started sucessfully!\n"));
            return true;
        }
        return false;
    } catch {
        return false;
    }
}
function displayFinalSteps({
    selectedFramework,
    relativePath,
    token,
    siteId,
    host
}: {
    selectedFramework: string;
    relativePath: string;
    token: string;
    siteId: string;
    host: string;
}) {
    switch (selectedFramework) {
        case 'nextjs': {
            finalStepsForNextjs({
                projectPath: relativePath,
                token: token,
                siteId: siteId,
                urlDotCMSInstance: host
            });
            break;
        }
        case 'angular': {
            finalStepsForAngularAndAngularSSR({
                projectPath: relativePath,
                token: token,
                siteId: siteId,
                urlDotCMSInstance: host
            });
            break;
        }
        case 'angular-ssr': {
            finalStepsForAngularAndAngularSSR({
                projectPath: relativePath,
                token: token,
                siteId: siteId,
                urlDotCMSInstance: host
            });
            break;
        }
        case 'astro': {
            finalStepsForAstro({
                projectPath: relativePath,
                token: token,
                siteId: siteId,
                urlDotCMSInstance: host
            });
            break;
        }
    }
}

async function startScaffoldingFrontEnd({
    spinner,
    selectedFramework,
    finalDirectory
}: {
    spinner: Ora;
    selectedFramework: SupportedFrontEndFrameworks;
    finalDirectory: string;
}) {
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
}
function printWelcomeScreen() {
    cfonts.say('DOTCMS', {
        font: 'block', // define the font face
        align: 'left', // define text alignment
        colors: ['system'], // define all colors
        background: 'transparent', // define the background color, you can also use `backgroundColor` here as key
        letterSpacing: 1, // define letter spacing
        lineHeight: 1, // define the line height
        space: true, // define if the output text should have empty lines on top and on the bottom
        maxLength: '0', // define how many character can be on one line
        gradient: false, // define your two gradient colors
        independentGradient: false, // define if you want to recalculate the gradient for each new line
        transitionGradient: false, // define if this is a transition between colors directly
        rawMode: false, // define if the line breaks should be CRLF (`\r\n`) over the default LF (`\n`)
        env: 'node' // define the environment cfonts is being executed in
    });
    console.log(chalk.white('\nWelcome to dotCMS CLI'));
    console.log(chalk.bgGrey.white('\n ℹ️  Beta: Features may change \n'));
}
createApp();
