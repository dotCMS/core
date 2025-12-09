import { Routes } from '@angular/router';

import { FeaturedFlags } from '@dotcms/dotcms-models';

import { DotContentTypesEditComponent } from '.';

import { DotFeatureFlagResolver } from '../resolvers/dot-feature-flag-resolver.service';

export const dotContentTypesEditRoutes: Routes = [
    {
        component: DotContentTypesEditComponent,
        path: '',
        resolve: {
            featuredFlags: DotFeatureFlagResolver
        },
        data: {
            featuredFlagsToCheck: [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]
        }
    }
];
