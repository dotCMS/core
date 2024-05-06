import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

/**
 * Route Guard that based on DOTFAVORITEPAGE_FEATURE_ENABLE flag allows/denies access to Pages portlet.
 */
@Injectable()
export class PagesGuardService implements CanActivate {
    constructor(private dotConfigurationService: DotPropertiesService) {}

    /**
     * Guard checks if DOTFAVORITEPAGE_FEATURE_ENABLE flag is true in dotmarketing-config.properties.
     * @returns Observable<boolean>
     */
    canActivate(): Observable<boolean> {
        return this.dotConfigurationService.getFeatureFlag(
            FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE
        );
    }
}
