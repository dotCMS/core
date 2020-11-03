import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplatePropsComponent } from './dot-template-props.component';
import { DotFormDialogModule } from '@components/dot-form-dialog/dot-form-dialog.module';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

@NgModule({
    declarations: [DotTemplatePropsComponent],
    imports: [
        CommonModule,
        DotFormDialogModule,
        FormsModule,
        InputTextModule,
        InputTextareaModule,
        ReactiveFormsModule
    ]
})
export class DotTemplatePropsModule {}
