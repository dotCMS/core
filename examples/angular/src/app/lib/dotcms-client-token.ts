import { InjectionToken, Provider } from '@angular/core';
import { dotcmsClient } from '@dotcms/client';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<any>('DOTCMS_CLIENT');

/**
 * This is a provider for the `DOTCMS_CLIENT_TOKEN` token.
 *
 * @param {*} config
 * @return {*} 
 */
export const provideDotCMSClient = (config: any): Provider => {
  return {
      provide: DOTCMS_CLIENT_TOKEN,
      useValue: {
        ...dotcmsClient.init(config)
      }
    }
}
