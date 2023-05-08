// Create Angular Module
import { NgModule } from '@angular/core';

import { FeatureFlagDirective } from './dot-show-hide-feature/dot-feature-flag.directive';
import { DotShowHideFeatureDirective } from './dot-show-hide-feature/dot-show-hide-feature.directive';

@NgModule({
    declarations: [DotShowHideFeatureDirective, FeatureFlagDirective],
    exports: [DotShowHideFeatureDirective, FeatureFlagDirective]
})
export class DirectiveModule {}
