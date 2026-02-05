import axios from 'axios';
import chalk from 'chalk';
import { execa } from 'execa';
import fs from 'fs-extra';
import { Err, Ok, Result } from 'ts-results';

import https from 'https';

import { escapeShellPath } from './validation';

import {
    ANGULAR_DEPENDENCIES,
    ANGULAR_DEPENDENCIES_DEV,
    ANGULAR_SSR_DEPENDENCIES,
    ANGULAR_SSR_DEPENDENCIES_DEV,
    ASTRO_DEPENDENCIES,
    ASTRO_DEPENDENCIES_DEV,
    NEXTJS_DEPENDENCIES,
    NEXTJS_DEPENDENCIES_DEV
} from '../constants';

import type { SupportedFrontEndFrameworks } from '../types';

export async function fetchWithRetry(
    url: string,
    retries = 5,
    delay = 5000,
    requestTimeout = 10000 // Per-request timeout in milliseconds
) {
    const errors: string[] = [];
    let lastError: unknown;

    for (let i = 0; i < retries; i++) {
        try {
            return await axios.get(url, {
                timeout: requestTimeout,
                validateStatus: (status) => status === 200
            });
        } catch (err) {
            lastError = err;

            // Track error for debugging with more context
            let errorMsg = '';
            if (axios.isAxiosError(err)) {
                if (err.code === 'ECONNREFUSED') {
                    errorMsg = 'Connection refused - service not accepting connections';
                } else if (err.code === 'ETIMEDOUT' || err.code === 'ECONNABORTED') {
                    errorMsg = 'Connection timeout - service too slow or not responding';
                } else if (err.response) {
                    errorMsg = `HTTP ${err.response.status}: ${err.response.statusText}`;
                } else {
                    errorMsg = err.code || err.message;
                }
            } else {
                errorMsg = String(err);
            }

            errors.push(`Attempt ${i + 1}: ${errorMsg}`);

            if (i === retries - 1) {
                // Last attempt failed - provide comprehensive error
                const errorType =
                    axios.isAxiosError(lastError) && lastError.code === 'ECONNREFUSED'
                        ? 'Connection Refused'
                        : axios.isAxiosError(lastError) &&
                            (lastError.code === 'ETIMEDOUT' || lastError.code === 'ECONNABORTED')
                          ? 'Timeout'
                          : 'Connection Failed';

                throw new Error(
                    chalk.red(
                        `\n❌ Failed to connect to dotCMS after ${retries} attempts (${errorType})\n\n`
                    ) +
                        chalk.white(`URL: ${url}\n\n`) +
                        chalk.yellow('Common causes:\n') +
                        chalk.white('  • dotCMS is still starting up (may need more time)\n') +
                        chalk.white('  • Container crashed or failed to start\n') +
                        chalk.white('  • Port conflict (8082 already in use)\n') +
                        chalk.white('  • Network/firewall blocking connection\n\n') +
                        chalk.gray(
                            'Detailed error history:\n' + errors.map((e) => `  • ${e}`).join('\n')
                        )
                );
            }

            console.log(
                chalk.yellow(`⏳ dotCMS not ready (attempt ${i + 1}/${retries})`) +
                    chalk.gray(` - ${errorMsg}`) +
                    chalk.gray(` - Retrying in ${delay / 1000}s...`)
            );
            await new Promise((r) => setTimeout(r, delay));
        }
    }
}

export function getUVEConfigValue(frontEndUrl: string) {
    return JSON.stringify({
        config: [
            {
                pattern: '.*',
                url: frontEndUrl
            }
        ]
    });
}

export function getPortByFramework(framework: SupportedFrontEndFrameworks): string {
    switch (framework) {
        case 'angular':
            return '4200';
        case 'angular-ssr':
            return '4200';
        case 'nextjs':
            return '3000';
        case 'astro':
            return '4321';
        default:
            throw new Error(`Unsupported framework: ${framework}`);
    }
}

export function getDotcmsApisByBaseUrl(baseUrl: string) {
    return {
        // Note: Using /appconfiguration instead of /probes/alive because the probe endpoints
        // have IP ACL restrictions that block requests from Docker host. See GitHub issue #34509
        DOTCMS_HEALTH_API: `${baseUrl}/api/v1/appconfiguration`,
        DOTCMS_TOKEN_API: `${baseUrl}/api/v1/authentication/api-token`,
        DOTCMS_EMA_CONFIG_API: `${baseUrl}/api/v1/apps/dotema-config-v2/`,
        DOTCMS_DEMO_SITE: `${baseUrl}/api/v1/site/`
    };
}

/** Utility to download a file using https */
export function downloadFile(url: string, dest: string): Promise<void> {
    return new Promise((resolve, reject) => {
        const file = fs.createWriteStream(dest);

        https
            .get(url, (response) => {
                if (response.statusCode !== 200) {
                    return reject(new Error(`Failed to download file: ${response.statusCode}`));
                }

                response.pipe(file);
                file.on('finish', () => file.close(() => resolve()));
            })
            .on('error', (err) => {
                fs.unlink(dest);
                reject(err);
            });
    });
}
export function finalStepsForNextjs({
    projectPath,
    urlDotCMSInstance,
    siteId,
    token
}: {
    projectPath: string;
    urlDotCMSInstance: string;
    siteId: string;
    token: string;
}) {
    console.log('\n');
    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.greenBright('📋 Next Steps:\n'));

    console.log(
        chalk.white('1. Navigate to your project:\n') +
            chalk.gray(`   $ cd ${escapeShellPath(projectPath)}\n`)
    );

    console.log(
        chalk.white('2. Create your environment file:\n') + chalk.gray('   $ touch .env\n')
    );

    console.log(chalk.white('3. Add your dotCMS configuration to ') + chalk.green('.env') + ':\n');

    console.log(chalk.white('──────────────────────────────────────────────\n'));
    console.log(chalk.white(getEnvVariablesForNextJS(urlDotCMSInstance, siteId, token)));
    console.log(chalk.white('\n──────────────────────────────────────────────\n'));

    console.log(chalk.gray('   💡 Tip: Copy the block above and paste into your .env file\n'));

    console.log(
        chalk.white('4. Start your development server:\n') + chalk.gray('   $ npm run dev\n')
    );

    console.log(
        chalk.white('5. Open your browser:\n') + chalk.gray('   → http://localhost:3000\n')
    );

    console.log(
        chalk.white('6. Edit your page content in dotCMS:\n') +
            chalk.gray(`   → ${urlDotCMSInstance}/dotAdmin/#/edit-page?url=/index\n`)
    );

    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.blueBright('📖 Documentation: ') + chalk.white('https://dev.dotcms.com'));

    console.log(chalk.blueBright('💬 Community: ') + chalk.white('https://community.dotcms.com\n'));
}

export function finalStepsForAstro({
    projectPath,
    urlDotCMSInstance,
    siteId,
    token
}: {
    projectPath: string;
    urlDotCMSInstance: string;
    siteId: string;
    token: string;
}) {
    console.log('\n');
    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.greenBright('📋 Next Steps:\n'));

    console.log(
        chalk.white('1. Navigate to your project:\n') +
            chalk.gray(`   $ cd ${escapeShellPath(projectPath)}\n`)
    );

    console.log(
        chalk.white('2. Create your environment file:\n') + chalk.gray('   $ touch .env\n')
    );

    console.log(chalk.white('3. Add your dotCMS configuration to ') + chalk.green('.env') + ':\n');

    console.log(chalk.white('──────────────────────────────────────────────\n'));
    console.log(chalk.white(getEnvVariablesForAstro(urlDotCMSInstance, siteId, token)));
    console.log(chalk.white('\n──────────────────────────────────────────────\n'));

    console.log(chalk.gray('   💡 Tip: Copy the block above and paste into your .env file\n'));

    console.log(
        chalk.white('4. Start your development server:\n') + chalk.gray('   $ npm run dev\n')
    );

    console.log(
        chalk.white('5. Open your browser:\n') + chalk.gray('   → http://localhost:3000\n')
    );

    console.log(
        chalk.white('6. Edit your page content in dotCMS:\n') +
            chalk.gray(`   → ${urlDotCMSInstance}/dotAdmin/#/edit-page?url=/index\n`)
    );

    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.blueBright('📖 Documentation: ') + chalk.white('https://dev.dotcms.com'));

    console.log(chalk.blueBright('💬 Community: ') + chalk.white('https://community.dotcms.com\n'));
}

export function finalStepsForAngularAndAngularSSR({
    projectPath,
    urlDotCMSInstance,
    siteId,
    token
}: {
    projectPath: string;
    urlDotCMSInstance: string;
    siteId: string;
    token: string;
}) {
    console.log('\n');
    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.greenBright('📋 Next Steps:\n'));

    console.log(
        chalk.white('1. Navigate to your environments directory:\n') +
            chalk.gray(`   $ cd ${escapeShellPath(projectPath)}/src/environments\n`)
    );

    console.log(
        chalk.white('2. Update the environment files:\n') +
            chalk.gray(
                '   Replace the contents of the following files:\n' +
                    '   • environment.ts\n' +
                    '   • environment.development.ts\n\n'
            )
    );

    console.log(chalk.white('3. Add your dotCMS configuration:\n'));

    console.log(chalk.white('──────────────────────────────────────────────\n'));
    console.log(chalk.white(getEnvVariablesForAngular(urlDotCMSInstance, siteId, token)));
    console.log(chalk.white('\n──────────────────────────────────────────────\n'));

    console.log(
        chalk.gray('   💡 Tip: Copy the block above and paste it into both environment files\n')
    );

    console.log(chalk.white('4. Start your development server:\n') + chalk.gray('   $ ng serve\n'));

    console.log(
        chalk.white('5. Open your browser:\n') + chalk.gray('   → http://localhost:4200\n')
    );

    console.log(
        chalk.white('6. Edit your page content in dotCMS:\n') +
            chalk.gray(`   → ${urlDotCMSInstance}/dotAdmin/#/edit-page?url=/index\n`)
    );

    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.blueBright('📖 Documentation: ') + chalk.white('https://dev.dotcms.com'));

    console.log(chalk.blueBright('💬 Community: ') + chalk.white('https://community.dotcms.com\n'));
}
function getEnvVariablesForNextJS(host: string, siteId: string, token: string) {
    return `
        NEXT_PUBLIC_DOTCMS_AUTH_TOKEN=${token}
        NEXT_PUBLIC_DOTCMS_HOST=${host}
        NEXT_PUBLIC_DOTCMS_SITE_ID=${siteId}
        NEXT_PUBLIC_DOTCMS_MODE='production'
    `;
}

function getEnvVariablesForAstro(host: string, siteId: string, token: string) {
    return `
        PUBLIC_DOTCMS_AUTH_TOKEN=${token}
        PUBLIC_DOTCMS_HOST=${host}
        PUBLIC_DOTCMS_SITE_ID=${siteId}
        PUBLIC_EXPERIMENTS_API_KEY=analytic-api-key-from-dotcms-portlet
        PUBLIC_EXPERIMENTS_DEBUG=true
    `;
}

function getEnvVariablesForAngular(host: string, siteId: string, token: string) {
    return `
    export const environment = {
        dotcmsUrl: '${host}',
        authToken: '${token}',
        siteId: '${siteId}',
    };
    `;
}

export async function installDependenciesForProject(
    projectPath: string
): Promise<Result<boolean, string>> {
    try {
        await execa('npm', ['install'], {
            cwd: projectPath
            // stdio: 'inherit', // optional: shows npm output in terminal
        });

        return Ok(true);
    } catch {
        return Err('Failed to install dependencies. Please make sure npm is installed');
    }
}

export function displayDependencies(selectedFrameWork: SupportedFrontEndFrameworks): string {
    switch (selectedFrameWork) {
        case 'nextjs':
            return formatDependencies(NEXTJS_DEPENDENCIES, NEXTJS_DEPENDENCIES_DEV);
        case 'astro':
            return formatDependencies(ASTRO_DEPENDENCIES, ASTRO_DEPENDENCIES_DEV);
        case 'angular':
            return formatDependencies(ANGULAR_DEPENDENCIES, ANGULAR_DEPENDENCIES_DEV);
        case 'angular-ssr':
            return formatDependencies(ANGULAR_SSR_DEPENDENCIES, ANGULAR_SSR_DEPENDENCIES_DEV);
        default:
            return '';
    }
}

function formatDependencies(dependencies: string[], devDependencies: string[]): string {
    const lines: string[] = [];

    lines.push(chalk.white('Dependencies:'));
    dependencies.forEach((item) => lines.push(chalk.grey(`- ${item}`)));

    lines.push(''); // blank line

    lines.push(chalk.white('Dev Dependencies:'));
    devDependencies.forEach((item) => lines.push(chalk.grey(`- ${item}`)));

    return lines.join('\n');
}
