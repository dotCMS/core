import { InjectionToken } from '@angular/core';
import { dotcmsClient } from '@dotcms/client';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<any>('DOTCMS_CLIENT');

export const dotcmsClientProvider = (config: any) => {
  return {
    provide: DOTCMS_CLIENT_TOKEN,
    useValue: {
      ...dotcmsClient.init(config)
    }
  }
}
