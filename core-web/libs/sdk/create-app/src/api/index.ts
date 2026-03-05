import axios from 'axios';
import chalk from 'chalk';
import { Ok, type Result, Err } from 'ts-results';

import { DOTCMS_DEMO_SITE, DOTCMS_EMA_CONFIG_API, DOTCMS_TOKEN_API } from '../constants';
import { FailedToGetDemoSiteIdentifierError, FailedToSetUpUVEConfig } from '../errors';

import type {
    DemoSiteResponse,
    GetUserTokenRequest,
    GetUserTokenResponse,
    UVEConfigRequest,
    UVEConfigResponse
} from '../types';
export class DotCMSApi {
    private static defaultTokenApi = DOTCMS_TOKEN_API;
    private static defaultDemoSiteApi = DOTCMS_DEMO_SITE;
    private static defaultUveConfigApi = DOTCMS_EMA_CONFIG_API;

    /** Get authentication token */
    static async getAuthToken({
        payload,
        url
    }: {
        payload: GetUserTokenRequest;
        url?: string;
    }): Promise<Result<string, string>> {
        const endpoint = url || this.defaultTokenApi;

        try {
            const res = await axios.post<GetUserTokenResponse>(endpoint, payload);
            return Ok(res.data.entity.token);
        } catch (err) {
            // Provide specific error messages based on error type
            if (axios.isAxiosError(err)) {
                if (err.response?.status === 401) {
                    return Err(
                        chalk.red('\n❌ Authentication failed\n\n') +
                            chalk.white('Invalid username or password.\n\n') +
                            chalk.yellow('Please check your credentials and try again:\n') +
                            chalk.white('  • Verify your username is correct\n') +
                            chalk.white('  • Ensure your password is correct\n') +
                            chalk.white('  • Check if your account is active\n')
                    );
                } else if (err.code === 'ECONNREFUSED') {
                    return Err(
                        chalk.red('\n❌ Connection refused\n\n') +
                            chalk.white(`Could not connect to dotCMS at: ${endpoint}\n\n`) +
                            chalk.yellow('Please verify:\n') +
                            chalk.white('  • The URL is correct\n') +
                            chalk.white('  • The dotCMS instance is running\n') +
                            chalk.white('  • There are no firewall issues\n')
                    );
                } else if (err.response) {
                    return Err(
                        chalk.red(`\n❌ Server error (${err.response.status})\n\n`) +
                            chalk.white('The dotCMS server returned an error.\n') +
                            chalk.gray(`Details: ${err.response.statusText || 'Unknown error'}\n`)
                    );
                }
            }
            return Err(
                chalk.red('\n❌ Failed to get authentication token\n\n') +
                    chalk.gray(err instanceof Error ? err.message : String(err))
            );
        }
    }

    /** Get demo site identifier */
    static async getDemoSiteIdentifier({
        siteName,
        authenticationToken,
        url
    }: {
        siteName: string;
        authenticationToken: string;
        url?: string;
    }): Promise<Result<DemoSiteResponse, FailedToGetDemoSiteIdentifierError>> {
        try {
            const endpoint = (url || this.defaultDemoSiteApi) + siteName;
            const res = await axios.get<DemoSiteResponse>(endpoint, {
                headers: { Authorization: `Bearer ${authenticationToken}` }
            });
            return Ok(res.data);
        } catch (err) {
            console.error('failed to get demo site identifier : ' + JSON.stringify(err));
            return Err(new FailedToGetDemoSiteIdentifierError());
        }
    }

    /** Setup UVE Config */
    static async setupUVEConfig({
        payload,
        siteId,
        authenticationToken,
        url
    }: {
        payload: UVEConfigRequest;
        siteId: string;
        authenticationToken: string;
        url?: string;
    }): Promise<Result<'Ok', FailedToSetUpUVEConfig>> {
        try {
            const endpoint = (url || this.defaultUveConfigApi) + siteId;
            const res = await axios.post<UVEConfigResponse>(endpoint, payload, {
                headers: { Authorization: `Bearer ${authenticationToken}` }
            });
            return Ok(res.data.entity);
        } catch (err) {
            console.error('failed to setup UVE config' + JSON.stringify(err));
            return Err(new FailedToSetUpUVEConfig());
        }
    }
}
