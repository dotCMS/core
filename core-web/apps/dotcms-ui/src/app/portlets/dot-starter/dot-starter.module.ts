import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { DotStarterComponent } from './dot-starter.component';
import { dotStarterRoutes } from './dot-starter.routes';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { ProgressBarModule } from 'primeng/progressbar';
import { MarkdownModule } from 'ngx-markdown';

@NgModule({
    declarations: [DotStarterComponent],
    imports: [
        CommonModule,
        RouterModule.forChild(dotStarterRoutes),
        AccordionModule,
        ProgressBarModule,
        ButtonModule,
        MarkdownModule
    ]
})
export class DotStarterModule {}
