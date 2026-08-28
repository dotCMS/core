import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

/**
 * Gates the redesigned dotAI config routes behind FEATURE_FLAG_DOTAI_CONFIG_UI.
 *
 * Uses `canMatch` (not `canActivate`): returning `false` makes the Router keep looking for
 * another route config matching the same path instead of blocking navigation outright, so it
 * falls through to the legacy `DotAiConfigDetailLegacyComponent` route declared right below in
 * `dot-apps.routes.ts`. `canActivate` cannot do this — it only allows or blocks the route it's
 * attached to.
 *
 * Uses `getFreshFeatureFlag` (uncached), not `getFeatureFlag`: an admin flipping this flag from
 * Maintenance and then navigating to the dotAI screen — without a full page reload — must see
 * the new value immediately, not whatever was cached from the first check this session.
 */
export const dotAiConfigDetailMatchGuard: CanMatchFn = () =>
    inject(DotPropertiesService).getFreshFeatureFlag(FeaturedFlags.FEATURE_FLAG_DOTAI_CONFIG_UI);
