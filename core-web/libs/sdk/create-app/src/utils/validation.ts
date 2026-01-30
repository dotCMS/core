import chalk from 'chalk';

import { FRAMEWORKS } from '../constants';

import type { SupportedFrontEndFrameworks } from '../types';

/**
 * Framework aliases to accept common variations
 */
const FRAMEWORK_ALIASES: Record<string, SupportedFrontEndFrameworks> = {
    next: 'nextjs',
    'next.js': 'nextjs',
    ng: 'angular',
    'angular-server': 'angular-ssr'
};

/**
 * Validates and normalizes framework name
 * Supports case-insensitive matching and common aliases
 *
 * @param framework - The framework name from CLI flag
 * @returns Normalized framework name or undefined if not provided
 * @throws Error if framework is invalid
 */
export function validateAndNormalizeFramework(
    framework: string | undefined
): SupportedFrontEndFrameworks | undefined {
    if (!framework) return undefined; // Will be prompted interactively

    // Normalize: lowercase, remove spaces
    const normalized = framework.toLowerCase().replace(/\s+/g, '');

    // Check aliases first
    if (normalized in FRAMEWORK_ALIASES) {
        return FRAMEWORK_ALIASES[normalized];
    }

    // Check exact match against supported frameworks
    if (FRAMEWORKS.includes(normalized as SupportedFrontEndFrameworks)) {
        return normalized as SupportedFrontEndFrameworks;
    }

    // Invalid framework - throw helpful error
    throw new Error(
        chalk.red(`❌ Invalid framework: "${framework}"`) +
            '\n\n' +
            chalk.white('Supported frameworks:\n') +
            FRAMEWORKS.map((f) => chalk.cyan(`  • ${f}`)).join('\n') +
            '\n\n' +
            chalk.gray('💡 Tip: Framework names are case-insensitive\n') +
            chalk.gray('   Aliases: next → nextjs, ng → angular')
    );
}

/**
 * Validates URL format
 * Checks for protocol, valid format, and hostname
 *
 * @param url - The URL from CLI flag
 * @throws Error if URL format is invalid
 */
export function validateUrl(url: string | undefined): void {
    if (!url) return; // Will be prompted interactively

    // Basic format check before URL parsing
    if (!url.includes('://')) {
        throw new Error(
            chalk.red(`❌ Invalid URL format: "${url}"`) +
                '\n\n' +
                chalk.white('URLs must include the protocol (http:// or https://)\n\n') +
                chalk.cyan('Example:\n  https://demo.dotcms.com\n\n') +
                chalk.gray('Need help?\n') +
                chalk.gray('  • Use --local flag to run dotCMS in Docker\n') +
                chalk.gray('  • Check our docs: https://dev.dotcms.com')
        );
    }

    try {
        const parsed = new URL(url);

        // Protocol validation
        if (!['http:', 'https:'].includes(parsed.protocol)) {
            throw new Error(
                chalk.red(`❌ Unsupported protocol: ${parsed.protocol}`) +
                    '\n\n' +
                    chalk.white('Only HTTP and HTTPS are supported')
            );
        }

        // Hostname validation
        if (!parsed.hostname) {
            throw new Error(chalk.red('❌ URL missing hostname'));
        }

        // Warn about localhost without Docker flag
        if (parsed.hostname === 'localhost' && parsed.port !== '8082') {
            console.log(
                chalk.yellow('⚠️  Warning: ') +
                    chalk.white('Using localhost but port is not 8082 (default dotCMS port)')
            );
        }
    } catch (error) {
        if (error instanceof TypeError) {
            throw new Error(
                chalk.red(`❌ Invalid URL format: "${url}"`) +
                    '\n\n' +
                    chalk.white('Please provide a valid HTTP/HTTPS URL\n\n') +
                    chalk.cyan('Example:\n  https://demo.dotcms.com')
            );
        }
        throw error;
    }
}
