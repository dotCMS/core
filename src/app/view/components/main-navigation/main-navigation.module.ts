import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { MainNavigationComponent } from './main-navigation.component';
import { Accordion, AccordionGroup } from '../_common/accordion/accordion';

@NgModule({
    imports: [
        CommonModule,
        RouterModule
    ],
    declarations: [
        MainNavigationComponent,
        AccordionGroup,
        Accordion
    ],
    exports: [
        MainNavigationComponent
    ]
})
export class MainNavigationModule { }
