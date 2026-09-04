import chalk from 'chalk';
import { execa } from 'execa';

import net from 'net';
import path from 'path';

import { describeRequestFailure, type RetryReporter } from './fetch-retry';
import { httpGet, isHttpError } from './http';
import { REQUIRED_PORTS } from './ports';
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
import { Err, Ok, type Result } from '../result';

import type { SupportedFrontEndFrameworks } from '../types';

/**
 * Fetches a URL with retry logic for health checks and connection validation
 *
 * @param url - The URL to fetch
 * @param retries - Number of retry attempts (default: 5)
 * @param delay - Delay between retries in milliseconds (default: 5000)
 * @param requestTimeout - Per-request timeout in milliseconds (default: 10000)
 * @returns Promise resolving to the HTTP response
 * @throws Error with detailed failure information after all retries exhausted
 *
 * @remarks
 * - Accepts any 2xx HTTP status code (200-299) as success
 * - Designed for health check endpoints that may return various success codes
 * - Provides detailed error messages including timeout configuration
 */
export async function fetchWithRetry(
    url: string,
    retries = 5,
    delay = 5000,
    requestTimeout = 10000, // Per-request timeout in milliseconds
    /**
     * Where retry progress goes. Omitted means silent: this function must not write to stdout
     * itself, because the caller usually has an `ora` spinner repainting the last line and
     * concurrent writes tear it (AC-009).
     */
    onRetry?: RetryReporter
) {
    const errors: string[] = [];
    let lastError: unknown;

    for (let i = 0; i < retries; i++) {
        try {
            // Any 2xx is success — the same rule isDotcmsRunning applies, so a 204 cannot be
            // accepted here and rejected there. httpGet throws on anything else.
            return await httpGet(url, { timeoutMs: requestTimeout });
        } catch (err) {
            lastError = err;

            const errorMsg = describeRequestFailure(err);

            errors.push(`Attempt ${i + 1}: ${errorMsg}`);

            if (i === retries - 1) {
                // Last attempt failed - provide comprehensive error
                const errorType =
                    isHttpError(lastError) && lastError.code === 'ECONNREFUSED'
                        ? 'Connection Refused'
                        : isHttpError(lastError) && lastError.code === 'ETIMEDOUT'
                          ? 'Timeout'
                          : 'Connection Failed';

                throw new Error(
                    chalk.red(
                        `\n❌ Failed to connect to dotCMS after ${retries} attempts (${errorType})\n\n`
                    ) +
                        chalk.white(`URL: ${url}\n`) +
                        chalk.gray(`Request timeout: ${requestTimeout}ms per attempt\n`) +
                        chalk.gray(
                            `Total retry window: ~${(retries * (delay + requestTimeout)) / 1000}s\n\n`
                        ) +
                        chalk.yellow('Common causes:\n') +
                        chalk.white('  • dotCMS is still starting up (may need more time)\n') +
                        chalk.white('  • Container crashed or failed to start\n') +
                        chalk.white('  • Port conflict (8082 already in use)\n') +
                        chalk.white('  • Network/firewall blocking connection\n') +
                        chalk.white(
                            `  • Request timeout too short (current: ${requestTimeout}ms)\n\n`
                        ) +
                        chalk.gray(
                            'Detailed error history:\n' + errors.map((e) => `  • ${e}`).join('\n')
                        )
                );
            }

            // Reported, never printed — see the onRetry doc above.
            onRetry?.({
                attempt: i + 1,
                totalAttempts: retries,
                reason: errorMsg,
                nextDelayMs: delay
            });
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
        // have IP ACL restrictions that block requests from Docker host.
        // See: https://github.com/dotCMS/core/issues/34509
        DOTCMS_HEALTH_API: `${baseUrl}/api/v1/appconfiguration`,
        DOTCMS_TOKEN_API: `${baseUrl}/api/v1/authentication/api-token`,
        DOTCMS_EMA_CONFIG_API: `${baseUrl}/api/v1/apps/dotema-config-v2/`,
        DOTCMS_SITE_API: `${baseUrl}/api/v1/site/`
    };
}

/**
 * The connection details, rendered inside the Next Steps block.
 *
 * This used to be printed by the exit handler, which necessarily runs last — so a successful
 * run showed its details after the summary that was supposed to contain them. It belongs here,
 * where the reader is already looking.
 */
export function renderConnectionSummary(report: {
    wroteEnv: boolean;
    filename: string | null;
    host: string;
    siteId: string;
    contents: string;
}) {
    if (report.wroteEnv && report.filename) {
        console.log(
            chalk.green(`   ✔ Your dotCMS credentials are already in ${report.filename}\n`) +
                chalk.gray(`     host    : ${report.host}\n`) +
                chalk.gray(`     site id : ${report.siteId}\n`)
        );

        return;
    }

    // No file was written — the framework has none, one already exists, or the write failed.
    console.log(
        chalk.white(
            report.filename
                ? `   Add these to your ${report.filename}:\n`
                : '   Configuration for your project:\n'
        ) +
            chalk.gray(
                report.contents
                    .trimEnd()
                    .split('\n')
                    .map((l) => `     ${l}`)
                    .join('\n')
            ) +
            '\n'
    );
}

export function finalStepsForNextjs({
    projectPath,
    urlDotCMSInstance,
    connection
}: {
    projectPath: string;
    urlDotCMSInstance: string;
    connection?: Parameters<typeof renderConnectionSummary>[0] | null;
}) {
    console.log('\n');
    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.greenBright('📋 Next Steps:\n'));

    if (connection) {
        renderConnectionSummary(connection);
    }

    console.log(
        chalk.white('1. Navigate to your project:\n') +
            chalk.gray(`   $ cd ${escapeShellPath(projectPath)}\n`)
    );

    // No "create a .env and paste this" step: the CLI writes the file itself, and the exit
    // handler confirms it. Telling the user to do it as well duplicated the token into
    // scrollback and asked them to redo work that was already done.
    console.log(
        chalk.white('2. Start your development server:\n') + chalk.gray('   $ npm run dev\n')
    );

    console.log(
        chalk.white('3. Open your browser:\n') + chalk.gray('   → http://localhost:3000\n')
    );

    console.log(
        chalk.white('4. Edit your page content in dotCMS:\n') +
            chalk.gray(`   → ${urlDotCMSInstance}/dotAdmin/#/edit-page?url=/index\n`)
    );

    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.blueBright('📖 Documentation: ') + chalk.white('https://dev.dotcms.com'));

    console.log(chalk.blueBright('💬 Community: ') + chalk.white('https://community.dotcms.com\n'));
}

export function finalStepsForAstro({
    projectPath,
    urlDotCMSInstance,
    connection
}: {
    projectPath: string;
    urlDotCMSInstance: string;
    connection?: Parameters<typeof renderConnectionSummary>[0] | null;
}) {
    console.log('\n');
    console.log(chalk.white('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n'));

    console.log(chalk.greenBright('📋 Next Steps:\n'));

    if (connection) {
        renderConnectionSummary(connection);
    }

    console.log(
        chalk.white('1. Navigate to your project:\n') +
            chalk.gray(`   $ cd ${escapeShellPath(projectPath)}\n`)
    );

    // The CLI writes the env file itself; see finalStepsForNextjs for why the paste step went.
    console.log(
        chalk.white('2. Start your development server:\n') + chalk.gray('   $ npm run dev\n')
    );

    console.log(
        chalk.white('3. Open your browser:\n') + chalk.gray('   → http://localhost:3000\n')
    );

    console.log(
        chalk.white('4. Edit your page content in dotCMS:\n') +
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
/**
 * The environment a scaffolded project needs, in the shape that project actually reads.
 *
 * One owner for this, because there are two consumers — the block printed in the final steps
 * and the `.env` written by the exit-state handler — and they MUST agree. They did not: the
 * handler wrote a hand-rolled `DOTCMS_AUTH_TOKEN` while Next.js reads
 * `NEXT_PUBLIC_DOTCMS_AUTH_TOKEN`, so the file looked right and the app could not authenticate.
 * Found by running the CLI end to end (#37262, T054).
 *
 * `filename` is null for frameworks that do not use a dotenv file at all — Angular reads a
 * TypeScript `environment` object, so writing `.env` there would be cargo-culting.
 */
export interface EnvFileSpec {
    filename: string | null;
    contents: string;
}

export function getEnvFileSpec(
    framework: string | undefined,
    host: string,
    siteId: string,
    token: string
): EnvFileSpec {
    if (framework === 'astro') {
        return {
            filename: '.env',
            contents: dedentEnv(getEnvVariablesForAstro(host, siteId, token))
        };
    }

    if (framework === 'angular' || framework === 'angular-ssr') {
        return {
            filename: null,
            contents: dedentEnv(getEnvVariablesForAngular(host, siteId, token))
        };
    }

    return { filename: '.env', contents: dedentEnv(getEnvVariablesForNextJS(host, siteId, token)) };
}

/** The builders below indent for terminal display; a written file must not carry that. */
function dedentEnv(block: string): string {
    return (
        block
            .split('\n')
            .map((line) => line.trim())
            .filter(Boolean)
            .join('\n') + '\n'
    );
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

/**
 * Checks if Docker is installed and running
 * @returns Result with true if available, or error message if not
 */
export async function checkDockerAvailability(): Promise<Result<true, string>> {
    try {
        // Check if Docker is installed and running by executing 'docker info'
        await execa('docker', ['info']);
        return Ok(true);
    } catch {
        // Docker is either not installed or not running
        const errorMsg =
            chalk.red('\n❌ Docker is not available\n\n') +
            chalk.white('Docker is required to run dotCMS locally.\n\n') +
            chalk.yellow('How to fix:\n') +
            chalk.white('  1. Install Docker Desktop:\n') +
            chalk.cyan('     → https://www.docker.com/products/docker-desktop\n\n') +
            chalk.white('  2. Start Docker Desktop\n') +
            chalk.white(
                '  3. Wait for Docker to be running (check the Docker icon in your system tray)\n'
            ) +
            chalk.white('  4. Run this command again\n\n') +
            chalk.gray('Alternative: Use --url flag to connect to an existing dotCMS instance');

        return Err(errorMsg);
    }
}

/**
 * Checks if a specific port is available
 * @param port - Port number to check
 * @returns Promise resolving to true if available, false if in use
 */
function isPortAvailable(port: number): Promise<boolean> {
    return new Promise((resolve) => {
        const server = net.createServer();

        server.once('error', (err: NodeJS.ErrnoException) => {
            if (err.code === 'EADDRINUSE') {
                resolve(false); // Port is in use
            } else {
                // Unexpected error while checking port; log and treat as unavailable
                console.warn(
                    chalk.yellow(
                        `Warning: Unexpected error while checking port ${port}: ${err.message}`
                    )
                );
                resolve(false); // Conservative: treat as unavailable
            }
        });

        server.once('listening', () => {
            server.close();
            resolve(true); // Port is available
        });

        server.listen(port, '0.0.0.0');
    });
}

/**
 * Checks if required dotCMS ports are available
 * @returns Result with true if all ports available, or error message with busy ports
 */

/**
 * Which of the required ports are taken.
 *
 * Separate from `checkPortsAvailability` because a busy 8082 is not automatically a conflict:
 * it may be a dotCMS from a previous successful run, which `resolvePortConflict` can reuse
 * rather than refuse (AC-006).
 */
export async function findBusyPorts(): Promise<{ port: number; service: string }[]> {
    const busyPorts: { port: number; service: string }[] = [];

    for (const { port, service } of REQUIRED_PORTS) {
        const available = await isPortAvailable(port);
        if (!available) {
            busyPorts.push({ port, service });
        }
    }

    return busyPorts;
}

export async function checkPortsAvailability(): Promise<Result<true, string>> {
    const busyPorts = await findBusyPorts();

    if (busyPorts.length > 0) {
        const errorMsg =
            chalk.red('\n❌ Required ports are already in use\n\n') +
            chalk.white('The following ports are busy:\n') +
            busyPorts
                .map(
                    ({ port, service }) =>
                        chalk.yellow(`  • Port ${port}`) + chalk.gray(` (${service})`)
                )
                .join('\n') +
            '\n\n' +
            chalk.yellow('How to fix:\n') +
            chalk.white('  1. Stop services using these ports:\n') +
            chalk.gray("     • Check what's using the ports: ") +
            chalk.cyan(
                process.platform === 'win32'
                    ? `netstat -ano | findstr ":<port>"`
                    : `lsof -i :<port>`
            ) +
            '\n' +
            chalk.gray('     • Stop the conflicting service\n\n') +
            chalk.white('  2. Or stop existing dotCMS containers:\n') +
            chalk.cyan('     $ docker compose down\n\n') +
            chalk.white('  3. Run this command again\n\n') +
            chalk.gray('Alternative: Use --url flag to connect to an existing dotCMS instance');

        return Err(errorMsg);
    }

    return Ok(true);
}

/**
 * Gets comprehensive Docker diagnostics including container status and logs
 * @param directory - Optional directory where docker-compose was run
 * @returns Formatted diagnostic information string
 */
export async function getDockerDiagnostics(directory?: string): Promise<string> {
    const diagnostics: string[] = [];

    // Reuse the Docker availability check
    const dockerAvailable = await checkDockerAvailability();
    if (!dockerAvailable.ok) {
        return dockerAvailable.val as string; // Return the detailed error message (Err value is string)
    }

    try {
        // Get container status
        const { stdout: psOutput } = await execa(
            'docker',
            ['ps', '-a', '--format', '{{.Names}}\t{{.Status}}\t{{.Ports}}'],
            { cwd: directory }
        );

        if (!psOutput.trim()) {
            diagnostics.push(chalk.yellow('\n⚠️  No Docker containers found'));
            diagnostics.push(
                chalk.white('The docker-compose.yml may not have been started correctly\n')
            );
            return diagnostics.join('\n');
        }

        diagnostics.push(chalk.cyan('\n📋 Container Status:'));
        const containers = psOutput.trim().split('\n');

        for (const container of containers) {
            const [name, status, ports] = container.split('\t');
            const isHealthy = status.includes('Up') && !status.includes('unhealthy');
            const icon = isHealthy ? '✅' : '❌';
            diagnostics.push(`  ${icon} ${chalk.white(name)}: ${chalk.gray(status)}`);
            if (ports) {
                diagnostics.push(`     ${chalk.gray('Ports:')} ${chalk.white(ports)}`);
            }
        }

        // Check for unhealthy containers and get their logs
        const unhealthyContainers = containers.filter(
            (c) => !c.includes('Up') || c.includes('unhealthy') || c.includes('Exited')
        );

        if (unhealthyContainers.length > 0) {
            diagnostics.push(chalk.yellow('\n🔍 Recent logs from problematic containers:\n'));

            for (const container of unhealthyContainers) {
                const name = container.split('\t')[0];
                try {
                    const { stdout: logs } = await execa('docker', ['logs', '--tail', '20', name], {
                        cwd: directory,
                        reject: false
                    });
                    diagnostics.push(chalk.white(`\n--- ${name} ---`));
                    diagnostics.push(chalk.gray(logs.split('\n').slice(-10).join('\n')));
                } catch {
                    diagnostics.push(chalk.gray(`  Unable to fetch logs for ${name}`));
                }
            }
        }
    } catch (error) {
        diagnostics.push(chalk.red('\n❌ Failed to get Docker diagnostics'));
        diagnostics.push(chalk.gray(String(error)));
    }

    diagnostics.push(chalk.yellow('\n💡 Troubleshooting steps:'));
    diagnostics.push(chalk.white('  1. Check if all containers are running:'));
    diagnostics.push(chalk.gray('     docker ps'));
    diagnostics.push(chalk.white('  2. View logs for a specific container:'));
    diagnostics.push(chalk.gray('     docker logs <container-name>'));
    diagnostics.push(chalk.white('  3. Restart the containers:'));
    diagnostics.push(chalk.gray('     docker compose down && docker compose up -d'));
    // Must track REQUIRED_PORTS (utils/ports.ts): 9200/9600 are no longer published by the
    // bundled stack, and 8090 now is.
    diagnostics.push(chalk.white('  4. Check if ports 8082, 8443, and 8090 are available\n'));

    return diagnostics.join('\n');
}

/**
 * Returns a user-friendly display path for CLI output
 * Uses absolute path if cleaner, otherwise uses relative path
 *
 * @param targetPath - Absolute path to the target directory
 * @param cwd - Current working directory
 * @returns Formatted path string for display (relative or absolute)
 *
 * @remarks
 * - Uses absolute path when relative path would contain 3+ parent directory traversals (../)
 * - Uses relative path for simpler navigation (e.g., './my-project')
 * - Prevents confusing output like 'cd ../../../../tmp/test-dir'
 */
export function getDisplayPath(targetPath: string, cwd: string): string {
    const relativePath = path.relative(cwd, targetPath);

    // Count the number of parent directory traversals in the relative path
    const parentDirCount = (relativePath.match(/\.\.\//g) || []).length;

    // If relative path has 3+ parent directories, use absolute path instead
    if (parentDirCount >= 3) {
        return targetPath;
    }

    // Otherwise use relative path (e.g., './my-project' or 'my-project')
    return relativePath.startsWith('.') ? relativePath : `./${relativePath}`;
}
