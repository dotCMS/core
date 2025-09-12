export interface EnvironmentConfig {
    baseUrl: string;
    reuseServer: boolean;
    headless: boolean;
    timeout: number;
}

export const environments: Record<string, EnvironmentConfig> = {
    dev: {
        baseUrl: 'http://localhost:4200',
        reuseServer: true,
        headless: false,
        timeout: 30000
    },
    ci: {
        baseUrl: 'http://localhost:8080',
        reuseServer: false,
        headless: true,
        timeout: 60000
    }
};

export function getEnvironmentConfig(): EnvironmentConfig {
    const env = process.env['CURRENT_ENV'] || 'dev';
    return environments[env] || environments.dev;
}
