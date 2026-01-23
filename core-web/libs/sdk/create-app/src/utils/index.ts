import axios from 'axios';
import chalk from 'chalk';
import { execa } from 'execa';
import fs from 'fs-extra';
import { Err, Ok, Result } from 'ts-results';

import https from 'https';

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
    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.greenBright('📋 Next Steps:\n'));

    console.log(
        chalk.white('1. Navigate to your project:\n') + chalk.gray(`   $ cd ${projectPath}\n`)
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
        chalk.white('1. Navigate to your project:\n') + chalk.gray(`   $ cd ${projectPath}\n`)
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

// export function finalStepsForAstro({
//     projectPath,
//     urlDotCMSInstance,
//     siteId,
//     token
// }: {
//     projectPath: string;
//     urlDotCMSInstance: string;
//     siteId: string;
//     token: string;
// }) {
//     console.log('\n');
//     console.log(chalk.cyanBright('📄 Update your frontend environment variables:\n'));
//
//     console.log(chalk.white('🪜  Steps:\n'));
//     console.log(chalk.white('1- cd ') + chalk.green(projectPath));
//     console.log(
//         chalk.white('2- Create a new file with the name ') +
//         chalk.green('.env') +
//         ' and paste the following:\n'
//     );
//
//     // ENV BLOCK — nicely spaced + grouped
//     console.log(chalk.white('──────────────────────────────────────────────'));
//     console.log(chalk.white(getEnvVariablesForAstro(urlDotCMSInstance, siteId, token)));
//     console.log();
//
//     console.log(chalk.white('──────────────────────────────────────────────\n'));
//
//     // START DEV SERVER
//     console.log(chalk.blueBright('💻 Start your frontend development server:'));
//     console.log(chalk.white('$ npm run dev\n'));
//
//     console.log(chalk.greenBright("🎉 You're all set!.\n"));
//
//     console.log(
//         chalk.greenBright(`Edit your page in ${urlDotCMSInstance}/dotAdmin/#/edit-page?url=/index`)
//     );
// }

// export function finalStepsForAngularAndAngularSSR({
//     projectPath,
//     urlDotCMSInstance,
//     siteId,
//     token
// }: {
//     projectPath: string;
//     urlDotCMSInstance: string;
//     siteId: string;
//     token: string;
// }) {
//     console.log('\n');
//     console.log(chalk.cyanBright('📄 Update your frontend environment variables:\n'));
//
//     console.log(chalk.white('🪜  Steps:\n'));
//     console.log(chalk.white('1- cd ') + chalk.green(projectPath) + '/src/environments');
//     console.log(
//         chalk.white(
//             '2- Replace the content of the file ' +
//             chalk.green('environment.ts') +
//             ' and ' +
//             chalk.green('environment.development.ts') +
//             ' with the following:'
//         )
//     );
//
//     // ENV BLOCK — nicely spaced + grouped
//     console.log(chalk.white('──────────────────────────────────────────────'));
//     console.log(chalk.white(getEnvVariablesForAngular(urlDotCMSInstance, siteId, token)));
//     console.log(chalk.white('──────────────────────────────────────────────\n'));
//
//     // START DEV SERVER
//     console.log(chalk.blueBright('💻 Start your frontend development server:'));
//     console.log(chalk.white('$ ng serve\n'));
//     console.log(chalk.greenBright("🎉 You're all set!.\n"));
//     console.log(
//         chalk.greenBright(`Edit your page in ${urlDotCMSInstance}/dotAdmin/#/edit-page?url=/index`)
//     );
// }
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
            chalk.gray(`   $ cd ${projectPath}/src/environments\n`)
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
        dotcmsUrl: ${host},
        authToken: ${token},
        siteId: ${siteId},
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
