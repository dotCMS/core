import axios from 'axios';
import fs from 'fs-extra';

import https from 'https';

import type { SupprotedFrontEndFramworks } from '../types';

export async function fetchWithRetry(url: string, retries = 5, delay = 5000) {
    for (let i = 0; i < retries; i++) {
        try {
            return await axios.get(url);
        } catch (err) {
            console.log(`Dotcms still not up 😴 ${i + 1}. Retrying in ${delay / 1000}s...`);

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

export function getPortByFramework(framework: SupprotedFrontEndFramworks): string {
    switch (framework) {
        case 'angular':
            return '4200';
        case 'angular-ssr':
            return '4200';
        case 'nextjs':
            return '3000';
        case 'vuejs':
            return '5173';
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
