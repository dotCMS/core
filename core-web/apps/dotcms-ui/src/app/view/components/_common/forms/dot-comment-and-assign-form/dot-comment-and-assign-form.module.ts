import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { TextareaModule } from 'primeng/textarea';

import { DotPageSelectorModule } from '@components/_common/dot-page-selector/dot-page-selector.module';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotRolesService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DotSafeHtmlPipe,
        TextareaModule,
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
