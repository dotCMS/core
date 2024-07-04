import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';

import { ClientConfig, dotcmsClient } from '@dotcms/client';
import { DOTCMS_CLIENT_TOKEN } from '@dotcms/angular';

/**
 * This is a provider for the `DOTCMS_CLIENT_TOKEN` token.
 *
 * @param {*} config
 * @return {*}
 */
export const provideDotcmsClient = (
  config: ClientConfig
): EnvironmentProviders => {
  return makeEnvironmentProviders([
    {
      provide: DOTCMS_CLIENT_TOKEN,
      useValue: dotcmsClient.init(config),
    },
  ]);
};
