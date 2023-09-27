import { Observable } from 'rxjs';

import { inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

/**
 *  Check if the Edit Content new form is enabled
 * @returns Observable<boolean>
 */
export const editContentGuard = (): Observable<boolean> =>
    inject(DotPropertiesService)
        .getKey(FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLE)
        .pipe(map((enabled: string) => enabled === 'true'));
