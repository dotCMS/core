import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotRolesService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotCommentAndAssignFormComponent } from './dot-comment-and-assign-form.component';

import { DotPipesModule } from '../../../../pipes/dot-pipes.module';
import { DotPageSelectorModule } from '../../dot-page-selector/dot-page-selector.module';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DotPipesModule,
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
