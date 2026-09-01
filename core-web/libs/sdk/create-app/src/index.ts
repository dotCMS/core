import cfonts from 'cfonts';
import chalk from 'chalk';
import { Command } from 'commander';
import { execa } from 'execa';
import fs from 'fs-extra';
import ora, { Ora } from 'ora';

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
import {
    CLOUD_HEALTH_CHECK_RETRIES,
    DOTCMS_HEALTH_API,
    DOTCMS_USER,
    LOCAL_HEALTH_CHECK_RETRIES
} from './constants';
import { FailedToCreateFrontendProjectError, FailedToDownloadDockerComposeError } from './errors';
import { installExitStateHandler, recordRecoverableState } from './exit-state';
import {
    cloneFrontEndSample,
    downloadDockerCompose,
    moveDockerComposeBack,
    moveDockerComposeOneLevelUp
} from './git';
import { type Result, Ok, Err } from './result';
import {
    checkDockerAvailability,
    checkPortsAvailability,
    displayDependencies,
    fetchWithRetry,
    finalStepsForAngularAndAngularSSR,
    finalStepsForAstro,
    finalStepsForNextjs,
    getDisplayPath,
    getDockerDiagnostics,
    getDotcmsApisByBaseUrl,
    getPortByFramework,
    installDependenciesForProject
} from './utils';
import { applyStarterUrl } from './utils/starter-url';
import {
    normalizeUrl,
    validateAndNormalizeFramework,
    validateConflictingParameters,
    validateProjectName,
    validateUrl
} from './utils/validation';
import { configureUVE } from './uve/configure-uve';

import type { DotCmsCliOptions, SupportedFrontEndFrameworks } from './types';

/** Budget for `docker compose up --wait`: a cold run pulls ~2GB and imports the demo starter. */
const COMPOSE_WAIT_TIMEOUT_SECONDS = 600;

// Supported values

/** Host the bundled compose stack publishes dotCMS on. */
const LOCAL_DOTCMS_HOST = 'http://localhost:8082';

// Registered before anything can fail: once a token exists, every terminal path — including
// the 17 process.exit() sites that `finally` cannot reach — prints it and writes .env (X1).
installExitStateHandler();

const program = new Command();

program
    .name('create-dotcms-app')
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

    // local options
    .option(
        '--starter <url>',
        'Custom starter URL for the local dotCMS Docker instance (sets CUSTOM_STARTER_URL)'
    )

    .action(async (projectName: string, options: DotCmsCliOptions) => {
        // welcome cli
        printWelcomeScreen();

        try {
            // ✅ VALIDATE ALL CLI FLAGS IMMEDIATELY - BEFORE ANY INTERACTIVE PROMPTS
            const validatedFramework = validateAndNormalizeFramework(options.framework);
            validateUrl(options.starter);
            validateUrl(options.url);
            validateConflictingParameters(options);
            validateProjectName(projectName); // Validate CLI flag if provided
            const starterOnlyMode = Boolean(options.starter);
            // `--starter` only applies to local Docker mode, so treat it as an implicit local selection.
            const isLocalModeRequested = options.local === true || starterOnlyMode;
            // `--url` implies cloud mode — skip the interactive prompt when it's provided.
            const isCloudExplicit = Boolean(options.url) && !isLocalModeRequested;

            // Get project name from CLI or prompt (prompt has built-in validation)
            const projectNameFinal = projectName ?? (await askProjectName());
            const directoryInput = options.directory ?? (await askDirectory());
            const finalDirectory = await prepareDirectory(directoryInput, projectNameFinal);
            const isCloudInstanceSelected = isLocalModeRequested
                ? false
                : isCloudExplicit || (await askCloudOrLocalInstance());

            if (isCloudInstanceSelected) {
                const urlInput = options.url ?? (await askDotcmsCloudUrl());
                // Validate and normalize URL (remove trailing slashes)
                validateUrl(urlInput);
                const urlDotcmsInstance = normalizeUrl(urlInput);

                const healthApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_HEALTH_API;
                const siteApiURL = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_SITE_API;
                const tokenApiUrl = getDotcmsApisByBaseUrl(urlDotcmsInstance).DOTCMS_TOKEN_API;

                const spinner = ora(`⏳ Connecting to dotCMS...`).start();

                const healthCheckResult = await isDotcmsRunning(
                    healthApiURL,
                    CLOUD_HEALTH_CHECK_RETRIES
                );

                if (!healthCheckResult.ok) {
                    spinner.fail(
                        'dotCMS is not running on the following url ' +
                            urlDotcmsInstance +
                            '. Please check the url and try again.'
                    );
                    console.error(healthCheckResult.val);
                    process.exit(1);
                }

                spinner.succeed('Connected to dotCMS successfully');

                // Authentication with retry on failure
                let dotcmsToken;
                let authAttempts = 0;
                const MAX_AUTH_ATTEMPTS = 3;

                while (authAttempts < MAX_AUTH_ATTEMPTS) {
                    authAttempts++;

                    const userNameDotCmsInstance =
                        options.username ?? (await askUserNameForDotcmsCloud());
                    const passwordDotCmsInstance =
                        options.password ?? (await askPasswordForDotcmsCloud());

                    dotcmsToken = await DotCMSApi.getAuthToken({
                        payload: {
                            user: userNameDotCmsInstance,
                            password: passwordDotCmsInstance,
                            expirationDays: '30',
                            label: 'token for frontend app'
                        },
                        url: tokenApiUrl
                    });

                    if (dotcmsToken.ok) {
                        spinner.succeed('Generated API authentication token');
                        break;
                    } else {
                        spinner.fail('Authentication failed');
                        console.error(dotcmsToken.val);

                        if (authAttempts < MAX_AUTH_ATTEMPTS) {
                            console.log(
                                chalk.yellow(
                                    `\nAttempt ${authAttempts}/${MAX_AUTH_ATTEMPTS} - Please try again\n`
                                )
                            );
                        } else {
                            console.log(
                                chalk.red(
                                    `\nMaximum authentication attempts (${MAX_AUTH_ATTEMPTS}) reached. Exiting.\n`
                                )
                            );
                            process.exit(1);
                        }
                    }
                }

                if (!dotcmsToken || !dotcmsToken.ok) {
                    process.exit(1);
                }

                const defaultSite = await DotCMSApi.getDefaultSite({
                    authenticationToken: dotcmsToken.val,
                    url: siteApiURL
                });

                if (!defaultSite.ok) {
                    spinner.fail('Failed to get default site identifier from Dotcms.');
                    process.exit(1);
                } else {
                    spinner.succeed(
                        `Retrieved default site (${defaultSite.val.entity.identifier})`
                    );
                }

                const selectedFramework = validatedFramework ?? (await askFramework());

                recordRecoverableState({
                    host: urlDotcmsInstance,
                    token: dotcmsToken.val,
                    siteId: defaultSite.val.entity.identifier,
                    projectDirectory: finalDirectory,
                    framework: selectedFramework
                });

                // Optional step: a failure here must not cost the user the run (contract X2).
                const uveOutcome = await configureUVE({
                    host: urlDotcmsInstance,
                    siteId: defaultSite.val.entity.identifier,
                    token: dotcmsToken.val,
                    mode: 'remote',
                    frontendUrl: `http://localhost:${getPortByFramework(selectedFramework as SupportedFrontEndFrameworks)}`,
                    report: (message) => spinner.info(message)
                });

                if (uveOutcome.kind === 'configured') {
                    spinner.succeed(`Configured the Universal Visual Editor`);
                } else {
                    spinner.warn('Skipped Universal Visual Editor configuration.');
                    console.log(chalk.yellow(uveOutcome.message));
                }
                await startScaffoldingFrontEnd({ spinner, selectedFramework, finalDirectory });
                console.log(chalk.white(`✅ Project setup complete!`));
                const relativePath = getDisplayPath(finalDirectory, process.cwd());
                displayFinalSteps({
                    host: urlDotcmsInstance,
                    relativePath,
                    token: dotcmsToken.val,
                    siteId: defaultSite.val.entity.identifier,
                    selectedFramework: selectedFramework
                });
                return; // Successful completion - exit code 0
            }

            const spinner = ora(`Checking Docker availability...`).start();

            // STEP 1 — Check if Docker is available
            const dockerAvailable = await checkDockerAvailability();
            if (!dockerAvailable.ok) {
                spinner.fail('Docker is not available');
                console.error(dockerAvailable.val);
                process.exit(1);
            }
            spinner.succeed('Docker is available');

            // STEP 2 — Check if required ports are available
            spinner.start('Checking port availability...');
            const portsAvailable = await checkPortsAvailability();
            if (!portsAvailable.ok) {
                spinner.fail('Required ports are busy');
                console.error(portsAvailable.val);
                process.exit(1);
            }
            spinner.succeed('All required ports are available');

            // STEP 3 — Download docker-compose
            spinner.start('Downloading Docker Compose configuration...');
            const downloaded = await downloadTheDockerCompose({
                directory: finalDirectory
            });
            if (!downloaded.ok) {
                spinner.fail('Failed to download Docker Compose file.');
                process.exit(1);
            }
            spinner.succeed('Docker Compose configuration downloaded');

            // STEP 4 — Run docker-compose
            spinner.start('Starting dotCMS containers...');
            const ran = await runDockerCompose({
                directory: finalDirectory,
                starterUrl: options.starter
            });
            if (!ran.ok) {
                spinner.fail('Failed to start Docker containers');
                const errorMessage = ran.val instanceof Error ? ran.val.message : String(ran.val);
                console.error(
                    chalk.red('\n❌ Docker Compose failed to start\n\n') +
                        chalk.white('Error details:\n') +
                        chalk.gray(errorMessage) +
                        '\n\n' +
                        chalk.yellow('Common solutions:\n') +
                        chalk.white('  • Ensure Docker Desktop is running\n') +
                        chalk.white('  • Try: ') +
                        chalk.cyan('docker compose down') +
                        chalk.white(' then run this command again\n') +
                        chalk.white('  • Check Docker logs for more details\n')
                );
                process.exit(1);
            }

            spinner.succeed('dotCMS containers started successfully.');

            spinner.start('Verifying if dotCMS is running...');

            const healthCheckResult = await isDotcmsRunning(
                DOTCMS_HEALTH_API,
                LOCAL_HEALTH_CHECK_RETRIES
            );

            if (!healthCheckResult.ok) {
                spinner.fail('dotCMS failed to start properly');
                console.error(healthCheckResult.val);
                console.error(await getDockerDiagnostics(finalDirectory));
                process.exit(1);
            }
            spinner.succeed('dotCMS is running locally at http://localhost:8082');
            spinner.succeed('Default credentials: admin@dotcms.com / admin');

            if (starterOnlyMode) {
                console.log(chalk.white(`✅ Project setup complete!`));
                console.log(
                    chalk.gray(
                        'Skipped frontend scaffolding and dotCMS UVE setup because --starter was provided.'
                    )
                );
                return;
            }

            const selectedFramework = validatedFramework ?? (await askFramework());

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
                process.exit(1);
            } else {
                spinner.succeed('Generated API authentication token');
            }

            const defaultSite = await DotCMSApi.getDefaultSite({
                authenticationToken: dotcmsToken.val
            });
            if (!defaultSite.ok) {
                spinner.fail('Failed to get default site identifier from Dotcms.');
                process.exit(1);
            } else {
                spinner.succeed(`Retrieved default site (${defaultSite.val.entity.identifier})`);
            }

            recordRecoverableState({
                host: LOCAL_DOTCMS_HOST,
                token: dotcmsToken.val,
                siteId: defaultSite.val.entity.identifier,
                projectDirectory: finalDirectory,
                framework: selectedFramework
            });

            // Optional step: a failure here must not cost the user the run (contract X2).
            const uveOutcome = await configureUVE({
                host: LOCAL_DOTCMS_HOST,
                siteId: defaultSite.val.entity.identifier,
                token: dotcmsToken.val,
                mode: 'local',
                frontendUrl: `http://localhost:${getPortByFramework(selectedFramework as SupportedFrontEndFrameworks)}`,
                report: (message) => spinner.info(message)
            });

            if (uveOutcome.kind === 'configured') {
                spinner.succeed(`Configured the Universal Visual Editor`);
            } else {
                spinner.warn('Skipped Universal Visual Editor configuration.');
                console.log(chalk.yellow(uveOutcome.message));
            }
            // required since git requires empty directory
            moveDockerComposeOneLevelUp(finalDirectory);
            await startScaffoldingFrontEnd({ spinner, selectedFramework, finalDirectory });
            moveDockerComposeBack(finalDirectory);
            console.log(chalk.white(`✅ Project setup complete!`));
            const relativePath = getDisplayPath(finalDirectory, process.cwd());
            displayFinalSteps({
                host: 'http://localhost:8082',
                relativePath,
                token: dotcmsToken.val,
                siteId: defaultSite.val.entity.identifier,
                selectedFramework: selectedFramework
            });
        } catch (error) {
            // Handle validation and other errors gracefully
            if (error instanceof Error) {
                console.error(error.message);
                // Preserve stack trace for debugging when DEBUG mode is enabled
                if (process.env.DEBUG) {
                    console.error('\n' + chalk.gray('Stack trace:'));
                    console.error(chalk.gray(error.stack || 'No stack trace available'));
                }
            } else {
                console.error(chalk.red('❌ An unexpected error occurred'));
                console.error(String(error));
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
    directory,
    starterUrl
}: {
    directory: string;
    starterUrl?: string;
}): Promise<Result<void, Error>> {
    try {
        // console.log(chalk.cyan("🐳 Starting Docker containers... (This might take some time)"));

        if (starterUrl) {
            await updateDockerComposeStarterUrl({ directory, starterUrl });
        }

        const env = starterUrl ? { ...process.env, CUSTOM_STARTER_URL: starterUrl } : process.env;

        // `--wait` blocks until every service with a healthcheck reports healthy, so the
        // success message below is only reached when the stack is genuinely usable. Without
        // it, `up -d` returns as soon as the containers are *created* — which is how the CLI
        // came to report "containers started successfully" about a dotcms that had already
        // exited (issue #37262, AC-002).
        //
        // The timeout is generous because a cold run pulls ~2GB and then imports the demo
        // starter. The bundled compose file sets `start_period: 180s` on dotcms; a boot
        // measured at ~46s leaves plenty of head-room inside this budget.
        await execa(
            'docker',
            [
                'compose',
                'up',
                '-d',
                '--wait',
                '--wait-timeout',
                String(COMPOSE_WAIT_TIMEOUT_SECONDS)
            ],
            { cwd: directory, env }
        );

        return Ok(undefined);
    } catch (err) {
        return Err(err as Error);
    }
}

async function updateDockerComposeStarterUrl({
    directory,
    starterUrl
}: {
    directory: string;
    starterUrl: string;
}): Promise<void> {
    const composePath = path.join(directory, 'docker-compose.yml');
    const composeContents = await fs.readFile(composePath, 'utf-8');
    // The string rewrite lives in ./utils/starter-url so a Jest spec can pin it against the
    // real bundled asset — it throws when the CUSTOM_STARTER_URL entry is missing.
    const updatedContents = applyStarterUrl(composeContents, starterUrl);

    await fs.writeFile(composePath, updatedContents);
}

async function isDotcmsRunning(url?: string, retries = 60): Promise<Result<boolean, string>> {
    try {
        // console.log(chalk.cyan("Waiting for DotCMS to be up ...."));
        const res = await fetchWithRetry(url ?? DOTCMS_HEALTH_API, retries, 5000);
        if (res && res.status === 200) {
            // console.log(chalk.green("✔ DotCMS container started sucessfully!\n"));
            return Ok(true);
        }
        return Err('dotCMS health check returned non-200 status');
    } catch (error) {
        const errorMessage = error instanceof Error ? error.message : String(error);
        return Err(errorMessage);
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
        process.exit(1);
    }

    // TODO need to insert here the dependices step
    spinner.succeed(`Frontend project (${selectedFramework}) scaffolded successfully.`);
    spinner.start(
        `📦 Installing dependencies...\n\n ${displayDependencies(selectedFramework as SupportedFrontEndFrameworks)}`
    );
    const result = await installDependenciesForProject(finalDirectory);
    if (!result) {
        spinner.fail(
            `Failed to install dependencies. Please check if npm is installed in your system`
        );
        process.exit(1);
    } else {
        spinner.succeed(`Dependencies installed`);
    }
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
