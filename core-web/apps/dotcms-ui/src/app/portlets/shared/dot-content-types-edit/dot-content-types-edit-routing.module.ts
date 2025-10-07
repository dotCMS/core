import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { FeaturedFlags } from '@dotcms/dotcms-models';

import { DotContentTypesEditComponent } from '.';

import { DotFeatureFlagResolver } from '../resolvers/dot-feature-flag-resolver.service';

const routes: Routes = [
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

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)],
    providers: [DotFeatureFlagResolver]
})
export class DotContentTypesEditRoutingModule {}
