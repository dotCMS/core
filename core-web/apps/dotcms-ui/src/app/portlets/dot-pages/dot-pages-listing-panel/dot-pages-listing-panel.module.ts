import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';

import { DotPagesListingPanelComponent } from './dot-pages-listing-panel.component';

@NgModule({
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        DropdownModule,
        DotAutofocusModule,
        DotMessagePipeModule,
        InputTextModule,
        SkeletonModule,
        TableModule,
        TooltipModule,
        UiDotIconButtonModule,
        RouterModule
    ],
    declarations: [DotPagesListingPanelComponent],
    exports: [DotPagesListingPanelComponent]
})
export class DotPagesListingPanelModule {}
