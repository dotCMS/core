import { EnvironmentProviders, InjectionToken, makeEnvironmentProviders } from '@angular/core';

import { ClientConfig, DotCmsClient, dotcmsClient } from '@dotcms/client';

import { DotCMSPageResolverService } from './resolver/dotcms.resolver';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<DotCmsClient>('DOTCMS_CLIENT');

/**
 * This is a provider for the `DOTCMS_CLIENT_TOKEN` token.
 *
 * @param {*} config
 * @return {*}
 */
export const provideDotCMSClient = (config: ClientConfig): EnvironmentProviders => {

    return makeEnvironmentProviders([
        {
            provide: DOTCMS_CLIENT_TOKEN,
            useValue: dotcmsClient.init(config)
        },
        DotCMSPageResolverService
    ]);
};
