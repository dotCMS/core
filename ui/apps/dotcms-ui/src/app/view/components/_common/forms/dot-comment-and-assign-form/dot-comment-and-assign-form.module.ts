import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotRolesService } from '@services/dot-roles/dot-roles.service';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { DotPageSelectorModule } from '@components/_common/dot-page-selector/dot-page-selector.module';

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
