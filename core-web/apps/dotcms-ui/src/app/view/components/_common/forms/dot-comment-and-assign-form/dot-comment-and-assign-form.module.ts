import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotPageSelectorModule } from '@components/_common/dot-page-selector/dot-page-selector.module';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotRolesService } from '@dotcms/data-access';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DotPipesModule,
        InputTextareaModule,
        DropdownModule,
        DotPageSelectorModule
    ],
    declarations: [DotCommentAndAssignFormComponent],
    exports: [DotCommentAndAssignFormComponent],
    providers: [DotRolesService]
})
export class DotCommentAndAssignFormModule {}
