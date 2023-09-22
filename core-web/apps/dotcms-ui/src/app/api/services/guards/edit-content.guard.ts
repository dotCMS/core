import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { CanActivate } from '@angular/router';

import { map } from 'rxjs/operators';

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
    private dotPropertiesService: DotPropertiesService = inject(DotPropertiesService);
    canActivate(): Observable<boolean> {
        return this.dotPropertiesService
            .getKey(FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLE)
            .pipe(map((enabled: string) => enabled === 'true'));
    }
}
