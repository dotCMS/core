import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { CanActivate } from '@angular/router';

import { map, take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

/**
 * Guard for the new Edit Content form
 *
 * @export
 * @class EditContentGuard
 * @implements {CanActivate}
 */
@Injectable()
export class EditContentGuard implements CanActivate {
    canActivate(): Observable<boolean> {
        return inject(DotPropertiesService)
            .getKey(FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLE)
            .pipe(
                take(1),
                map((enabled: string) => enabled === 'true')
            );
    }
}
