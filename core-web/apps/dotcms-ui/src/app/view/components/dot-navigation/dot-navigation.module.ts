import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotIconModule } from '@dotcms/ui';

import { DotNavHeaderComponent } from './components/dot-nav-header/dot-nav-header.component';
import { DotNavIconModule } from './components/dot-nav-icon/dot-nav-icon.module';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { DotSubNavComponent } from './components/dot-sub-nav/dot-sub-nav.component';
import { DotNavigationComponent } from './dot-navigation.component';
import { DotNavigationService } from './services/dot-navigation.service';

import { DotRandomIconPipeModule } from '../../pipes/dot-radom-icon/dot-random-icon.pipe.module';
import { DotOverlayMaskModule } from '../_common/dot-overlay-mask/dot-overlay-mask.module';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        RouterModule,
        DotNavIconModule,
        DotIconModule,
        TooltipModule,
        DotOverlayMaskModule,
        DotRandomIconPipeModule
    ],
    declarations: [
        DotNavigationComponent,
        DotSubNavComponent,
        DotNavItemComponent,
        DotNavHeaderComponent
    ],
    providers: [DotNavigationService],
    exports: [DotNavigationComponent]
})
export class MainNavigationModule {}
