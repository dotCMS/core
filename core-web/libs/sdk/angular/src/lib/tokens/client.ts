import { InjectionToken } from '@angular/core';

import { DotCmsClient } from '@dotcms/client';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<DotCmsClient>('DOTCMS_CLIENT');
