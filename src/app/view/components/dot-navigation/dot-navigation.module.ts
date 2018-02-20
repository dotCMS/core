import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { DotNavigationComponent } from './dot-navigation.component';
import { AccordionComponent, AccordionGroupComponent } from '../_common/accordion/accordion';
import { DotNavigationService } from './dot-navigation.service';

@NgModule({
    imports: [CommonModule, RouterModule],
    declarations: [DotNavigationComponent, AccordionGroupComponent, AccordionComponent],
    providers: [DotNavigationService],
    exports: [DotNavigationComponent]
})
export class MainNavigationModule {}
