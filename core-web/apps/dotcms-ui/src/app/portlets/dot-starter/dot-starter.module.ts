import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { KnobModule } from 'primeng/knob';
import { ProgressBarModule } from 'primeng/progressbar';
import { TagModule } from 'primeng/tag';

import { DotStarterComponent } from './dot-starter.component';
import { dotStarterRoutes } from './dot-starter.routes';

@NgModule({
    declarations: [DotStarterComponent],
    imports: [
        CommonModule,
        RouterModule.forChild(dotStarterRoutes),
        AccordionModule,
        FormsModule,
        ProgressBarModule,
        KnobModule,
        ButtonModule,
        MarkdownModule,
        TagModule
    ]
})
export class DotStarterModule {}
