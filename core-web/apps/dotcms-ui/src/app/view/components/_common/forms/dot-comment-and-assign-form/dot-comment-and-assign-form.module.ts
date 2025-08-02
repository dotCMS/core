import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotRolesService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotCommentAndAssignFormComponent } from './dot-comment-and-assign-form.component';

import { DotPageSelectorModule } from '../../dot-page-selector/dot-page-selector.module';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DotSafeHtmlPipe,
        InputTextareaModule,
        DropdownModule,
        DotPageSelectorModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    declarations: [DotCommentAndAssignFormComponent],
    exports: [DotCommentAndAssignFormComponent],
    providers: [DotRolesService]
})
export class DotCommentAndAssignFormModule {}
