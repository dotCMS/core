import axios from 'axios';
import chalk from 'chalk';
import fs from 'fs-extra';

import https from 'https';

import type { SupportedFrontEndFrameworks } from '../types';

export async function fetchWithRetry(url: string, retries = 5, delay = 5000) {
    for (let i = 0; i < retries; i++) {
        try {
            return await axios.get(url);
        } catch (err) {
            console.log(`dotCMS still not up 😴 ${i + 1}. Retrying in ${delay / 1000}s...`);

            if (i === retries - 1) throw err; // throw after last attempt

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
        DOTCMS_HEALTH_API: `${baseUrl}/api/v1/probes/alive`,
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
    console.log(chalk.cyanBright('📄 Update your frontend environment variables:\n'));

    console.log(chalk.white('🪜  Steps:\n'));
    console.log(chalk.white('1- cd ') + chalk.green(projectPath));
    console.log(
        chalk.white('2- Create a new file with the name ') +
            chalk.green('.env') +
            ' and paste the following:\n'
    );

    // ENV BLOCK — nicely spaced + grouped
    console.log(chalk.white('──────────────────────────────────────────────'));
    console.log(chalk.white(getEnvVariablesForNextJS(urlDotCMSInstance, siteId, token)));
    console.log(chalk.white('──────────────────────────────────────────────\n'));

    // INSTALL DEPENDENCIES
    console.log(chalk.magentaBright('📦 Install frontend dependencies:'));
    console.log(chalk.white('$ npm install\n'));

    // START DEV SERVER
    console.log(chalk.blueBright('💻 Start your frontend development server:'));
    console.log(chalk.white('$ npm run dev\n'));

    console.log(
        chalk.greenBright(
            "🎉 You're all set! Start building your app with dotCMS + your chosen frontend framework.\n"
        )
    );
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
    console.log(chalk.cyanBright('📄 Update your frontend environment variables:\n'));

    console.log(chalk.white('🪜  Steps:\n'));
    console.log(chalk.white('1- cd ') + chalk.green(projectPath));
    console.log(
        chalk.white('2- Create a new file with the name ') +
            chalk.green('.env') +
            ' and paste the following:\n'
    );

    // ENV BLOCK — nicely spaced + grouped
    console.log(chalk.white('──────────────────────────────────────────────'));
    console.log(chalk.white(getEnvVariablesForAstro(urlDotCMSInstance, siteId, token)));
    console.log();

    console.log(chalk.white('──────────────────────────────────────────────\n'));

    // INSTALL DEPENDENCIES
    console.log(chalk.magentaBright('📦 Install frontend dependencies:'));
    console.log(chalk.white('$ npm install\n'));

    // START DEV SERVER
    console.log(chalk.blueBright('💻 Start your frontend development server:'));
    console.log(chalk.white('$ npm run dev\n'));

    console.log(
        chalk.greenBright(
            "🎉 You're all set! Start building your app with dotCMS + your chosen frontend framework.\n"
        )
    );
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
    console.log(chalk.cyanBright('📄 Update your frontend environment variables:\n'));

    console.log(chalk.white('🪜  Steps:\n'));
    console.log(chalk.white('1- cd ') + chalk.green(projectPath) + '/src/environments');
    console.log(
        chalk.white(
            '2- Replace the content of the file ' +
                chalk.green('environment.ts') +
                ' and ' +
                chalk.green('environment.development.ts') +
                ' with the following:'
        )
    );

    // ENV BLOCK — nicely spaced + grouped
    console.log(chalk.white('──────────────────────────────────────────────'));
    console.log(chalk.white(getEnvVariablesForAngular(urlDotCMSInstance, siteId, token)));
    console.log(chalk.white('──────────────────────────────────────────────\n'));

    // INSTALL DEPENDENCIES
    console.log(chalk.magentaBright('📦 Install frontend dependencies:'));
    console.log(chalk.white('$ npm install\n'));

    // START DEV SERVER
    console.log(chalk.blueBright('💻 Start your frontend development server:'));
    console.log(chalk.white('$ ng serve\n'));
    console.log(
        chalk.greenBright(
            "🎉 You're all set! Start building your app with dotCMS + your chosen frontend framework.\n"
        )
    );
}

function getEnvVariablesForNextJS(host: string, siteId: string, token: string) {
    return `
        NEXT_PUBLIC_DOTCMS_AUTH_TOKEN=${token}
        NEXT_PUBLIC_DOTCMS_HOST=${host}
        NEXT_PUBLIC_DOTCMS_SITE_ID=${siteId}
        NEXT_PUBLIC_DOTCMS_MODE='production'
        NODE_TLS_REJECT_UNAUTHORIZED=0
    `;
}

function getEnvVariablesForAstro(host: string, siteId: string, token: string) {
    return `
        PUBLIC_DOTCMS_AUTH_TOKEN=${token}
        PUBLIC_DOTCMS_HOST=${host}
        PUBLIC_DOTCMS_SITE_ID=${siteId}
        PUBLIC_EXPERIMENTS_API_KEY=analytic-api-key-from-dotcms-portlet
        PUBLIC_EXPERIMENTS_DEBUG=true
        # If your local dotcms instance is running in https, this setting allows Node.js to connect to servers with invalid SSL certificates.
        # For testing purposes only.
        NODE_TLS_REJECT_UNAUTHORIZED=0
    `;
}

function getEnvVariablesForAngular(host: string, siteId: string, token: string) {
    return `
    export const environment = {
        dotcmsUrl: ${host},
        authToken: ${token},
        siteId: ${siteId},
    };
    `;
}
