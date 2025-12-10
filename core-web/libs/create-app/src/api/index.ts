import axios from 'axios';
import { Ok, type Result, Err } from 'ts-results';

import { DOTCMS_DEMO_SITE, DOTCMS_EMA_CONFIG_API, DOTCMS_TOKEN_API } from '../constants';
import {
    FailedToGetDemoSiteIdentifierError,
    FailedToGetDotcmsTokenError,
    FailedToSetUpUVEConfig
} from '../errors';

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
    }): Promise<Result<string, FailedToGetDotcmsTokenError>> {
        try {
            const endpoint = url ?? this.defaultTokenApi;
            const res = await axios.post<GetUserTokenResponse>(endpoint, payload);
            return Ok(res.data.entity.token);
        } catch (err) {
            console.error('dotCMS failed to get token' + JSON.stringify(err));
            return Err(new FailedToGetDotcmsTokenError());
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
            const endpoint = (url ?? this.defaultDemoSiteApi) + siteName;
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
            const endpoint = (url ?? this.defaultUveConfigApi) + siteId;
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
