import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { DotNavigationComponent } from './dot-navigation.component';
import { AccordionComponent, AccordionGroupComponent } from '../_common/accordion/accordion';
import { DotNavigationService } from './dot-navigation.service';
import { DotIconModule } from '../_common/dot-icon/dot-icon.module';

@NgModule({
    imports: [CommonModule, RouterModule, DotIconModule],
    declarations: [DotNavigationComponent, AccordionGroupComponent, AccordionComponent],
    providers: [DotNavigationService],
    exports: [DotNavigationComponent]
})
export class MainNavigationModule {}
