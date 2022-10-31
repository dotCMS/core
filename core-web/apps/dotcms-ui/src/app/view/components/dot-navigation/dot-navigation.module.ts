import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { DotNavigationComponent } from './dot-navigation.component';
import { DotNavigationService } from './services/dot-navigation.service';
import { DotNavIconModule } from './components/dot-nav-icon/dot-nav-icon.module';
import { DotIconModule } from '@dotcms/ui';
import { DotSubNavComponent } from './components/dot-sub-nav/dot-sub-nav.component';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { TooltipModule } from 'primeng/tooltip';
import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotRandomIconPipeModule } from '@pipes/dot-radom-icon/dot-random-icon.pipe.module';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        DotNavIconModule,
        DotIconModule,
        TooltipModule,
        DotOverlayMaskModule,
        DotRandomIconPipeModule
    ],
    declarations: [DotNavigationComponent, DotSubNavComponent, DotNavItemComponent],
    providers: [DotNavigationService],
    exports: [DotNavigationComponent]
})
export class MainNavigationModule {}
