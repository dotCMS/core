import { Routes } from '@angular/router';

import { FeaturedFlags } from '@dotcms/dotcms-models';

import { DotContentTypesEditComponent } from '.';

import { permissionsTabGuard, styleEditorTabGuard } from './dot-content-type-tabs.guard';
import { dotContentTypeTabsResolver } from './dot-content-type-tabs.resolver';

import { DotFeatureFlagResolver } from '../resolvers/dot-feature-flag-resolver.service';

export const dotContentTypesEditRoutes: Routes = [
    {
        component: DotContentTypesEditComponent,
        path: '',
        resolve: {
            featuredFlags: DotFeatureFlagResolver,
            tabPermissions: dotContentTypeTabsResolver
        },
        data: {
            featuredFlagsToCheck: [
                FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED,
                FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR
            ]
        },
        children: [
            { path: '', redirectTo: 'fields', pathMatch: 'full' },
            { path: 'fields', children: [] },
            { path: 'style-editor', canActivate: [styleEditorTabGuard], children: [] },
            { path: 'permissions', canActivate: [permissionsTabGuard], children: [] },
            { path: 'push-history', children: [] }
        ]
    }
];
