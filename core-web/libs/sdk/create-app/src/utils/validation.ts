import chalk from 'chalk';

import path from 'path';

import { FRAMEWORKS } from '../constants';

import type { DotCmsCliOptions, SupportedFrontEndFrameworks } from '../types';

/**
 * Maximum allowed length for project names (typical filesystem limit)
 */
const MAX_PROJECT_NAME_LENGTH = 255;

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
    if (!url || url.trim() === '') return; // Will be prompted interactively

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

/**
 * Validates project name for filesystem safety and best practices
 *
 * @param projectName - The project name from CLI flag or prompt
 * @returns Validated project name (preserves original casing)
 * @throws Error if project name is invalid
 */
export function validateProjectName(projectName: string | undefined): string | undefined {
    if (projectName === undefined || projectName === null) {
        return undefined; // Will be prompted interactively
    }

    const trimmed = projectName.trim();

    // Empty or whitespace-only
    if (trimmed.length === 0) {
        throw new Error(
            chalk.red('❌ Invalid project name: cannot be empty') +
                '\n\n' +
                chalk.white('Project name must contain at least one character\n\n') +
                chalk.cyan('Valid examples:\n') +
                chalk.gray('  • my-dotcms-app\n') +
                chalk.gray('  • my_project\n') +
                chalk.gray('  • MyProject123')
        );
    }

    // Path traversal check
    if (
        trimmed === '.' ||
        trimmed === '..' ||
        trimmed.includes('../') ||
        trimmed.includes('..\\')
    ) {
        throw new Error(
            chalk.red(`❌ Invalid project name: "${projectName}"`) +
                '\n\n' +
                chalk.white('Project name cannot contain path traversal patterns (..)\n\n') +
                chalk.gray('Use the --directory flag to specify the parent directory')
        );
    }

    // Absolute path check
    if (path.isAbsolute(trimmed)) {
        throw new Error(
            chalk.red(`❌ Invalid project name: "${projectName}"`) +
                '\n\n' +
                chalk.white('Project name cannot be an absolute path\n\n') +
                chalk.gray('Use the --directory flag to specify the location')
        );
    }

    // Invalid filesystem characters (cross-platform)
    const invalidChars = /[<>:"|?*]/;
    // Control characters (ASCII 0-31) checked separately to avoid regex complexity
    const CONTROL_CHAR_THRESHOLD = 32; // ASCII control characters are 0-31
    const hasControlChar = [...trimmed].some((c) => c.charCodeAt(0) < CONTROL_CHAR_THRESHOLD);
    if (invalidChars.test(trimmed) || hasControlChar) {
        throw new Error(
            chalk.red(`❌ Invalid project name: "${projectName}"`) +
                '\n\n' +
                chalk.white('Project name contains invalid characters\n\n') +
                chalk.white('Avoid: < > : " | ? * and control characters\n\n') +
                chalk.cyan('Valid examples:\n') +
                chalk.gray('  • my-project\n') +
                chalk.gray('  • my_project\n') +
                chalk.gray('  • project-123')
        );
    }

    // Windows reserved names
    const reservedNames = [
        'CON',
        'PRN',
        'AUX',
        'NUL',
        'COM1',
        'COM2',
        'COM3',
        'COM4',
        'COM5',
        'COM6',
        'COM7',
        'COM8',
        'COM9',
        'LPT1',
        'LPT2',
        'LPT3',
        'LPT4',
        'LPT5',
        'LPT6',
        'LPT7',
        'LPT8',
        'LPT9'
    ];
    if (reservedNames.includes(trimmed.toUpperCase())) {
        throw new Error(
            chalk.red(`❌ Invalid project name: "${projectName}"`) +
                '\n\n' +
                chalk.white('This is a reserved system name on Windows\n\n') +
                chalk.gray('Please choose a different name')
        );
    }

    // Length validation
    if (trimmed.length > MAX_PROJECT_NAME_LENGTH) {
        throw new Error(
            chalk.red('❌ Project name too long') +
                '\n\n' +
                chalk.white(
                    `Maximum: ${MAX_PROJECT_NAME_LENGTH} characters (you provided: ${trimmed.length})\n\n`
                ) +
                chalk.gray('Please use a shorter name')
        );
    }

    // Warning for hidden files
    if (trimmed.startsWith('.') && trimmed.length > 1) {
        console.warn(
            chalk.yellow('\n⚠️  Warning: Project name starts with a dot') +
                '\n' +
                chalk.gray('This will create a hidden directory on Unix systems\n')
        );
    }

    return projectName; // Return original to preserve user's casing
}

/**
 * Escapes a file path for safe use in shell commands across platforms
 * Handles spaces, special characters, and cross-platform compatibility
 *
 * @param filePath - The file path to escape
 * @returns Shell-safe escaped path
 */
export function escapeShellPath(filePath: string): string {
    if (!filePath) return '""';

    // Already quoted - return as-is (idempotent)
    if (
        (filePath.startsWith('"') && filePath.endsWith('"')) ||
        (filePath.startsWith("'") && filePath.endsWith("'"))
    ) {
        return filePath;
    }

    // Check if escaping needed (brackets moved to end to avoid ESLint warnings)
    // Platform-specific: Windows paths use backslashes natively, Unix paths need backslash escaping
    const needsEscaping =
        process.platform === 'win32'
            ? /[\s'"`$!&*(){};<>?*|\n\r\t[\]]/.test(filePath) // Skip backslash on Windows
            : /[\s'"`$!&*(){};<>?*|\\\n\r\t[\]]/.test(filePath); // Include backslash on Unix

    if (needsEscaping) {
        // Use double quotes, escape internal quotes and backslashes
        const escaped = filePath.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
        return `"${escaped}"`;
    }

    return filePath; // Simple path - no escaping needed
}

/**
 * Validates CLI options for conflicting parameters
 * Warns user when both local and cloud parameters are provided
 *
 * @param options - CLI options to validate
 */
export function validateConflictingParameters(options: DotCmsCliOptions): void {
    const isLocalSet = options.local === true;

    if (!isLocalSet) return; // No conflict possible

    // Check for cloud parameters
    const cloudParams: string[] = [];
    if (options.url) cloudParams.push('--url');
    if (options.username) cloudParams.push('--username');
    if (options.password) cloudParams.push('--password');

    // Warn about conflict
    if (cloudParams.length > 0) {
        console.warn(
            chalk.yellow('\n⚠️  Warning: Conflicting parameters detected\n') +
                chalk.white('You provided ') +
                chalk.cyan('--local') +
                chalk.white(' flag along with cloud parameters: ') +
                chalk.cyan(cloudParams.join(', ')) +
                '\n\n' +
                chalk.gray('The ') +
                chalk.gray.bold('--local') +
                chalk.gray(' flag will be used (Docker deployment)') +
                '\n' +
                chalk.gray('Cloud parameters will be ignored\n')
        );
    }
}
