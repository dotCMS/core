import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ContextMenuModule } from 'primeng/contextmenu';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { DotAutofocusDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotPagesListingPanelComponent } from './dot-pages-listing-panel.component';

@NgModule({
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        FormsModule,
        DotAutofocusDirective,
        DotMessagePipe,
        DotRelativeDatePipe,
        DropdownModule,
        InputTextModule,
        SkeletonModule,
        TableModule,
        TooltipModule,
        RouterModule,
        ContextMenuModule
    ],
    declarations: [DotPagesListingPanelComponent],
    exports: [DotPagesListingPanelComponent]
})
export class DotPagesListingPanelModule {}
