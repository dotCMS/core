import { Observable, Subject, fromEvent } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, inject } from '@angular/core';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FocusTrapModule } from 'primeng/focustrap';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';

import { map, startWith, takeUntil } from 'rxjs/operators';

import { DotTempFileUploadService } from '@dotcms/data-access';
import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotTemplateThumbnailFieldComponent } from './dot-template-thumbnail-field/dot-template-thumbnail-field.component';

import { DotThemeSelectorDropdownComponent } from '../../../../view/components/dot-theme-selector-dropdown/dot-theme-selector-dropdown.component';
import { DotTemplateItem } from '../store/dot-template.store';

@Component({
    selector: 'dot-template-props',
    templateUrl: './dot-template-props.component.html',
    styleUrls: ['./dot-template-props.component.scss'],
    providers: [DotTempFileUploadService],
    imports: [
        CommonModule,
        DotFieldValidationMessageComponent,
        ButtonModule,
        FocusTrapModule,
        FormsModule,
        InputTextModule,
        TextareaModule,
        ReactiveFormsModule,
        DotMessagePipe,
        DotTemplateThumbnailFieldComponent,
        DotThemeSelectorDropdownComponent,
        DotFieldRequiredDirective
    ]
})
export class DotTemplatePropsComponent implements OnInit, OnDestroy {
    private ref = inject(DynamicDialogRef);
    private config = inject(DynamicDialogConfig);
    private fb = inject(UntypedFormBuilder);
    private el = inject(ElementRef);

    private destroy$ = new Subject<void>();
    private originalTemplate: DotTemplateItem;

    form: UntypedFormGroup;

    isFormValid$: Observable<boolean>;

    ngOnInit(): void {
        const { template } = this.config.data;
        this.originalTemplate = template;

        const formGroupAttrs =
            template.theme !== undefined
                ? {
                      ...template,
                      title: [template.title, Validators.required],
                      theme: [template.theme]
                  }
                : {
                      ...template,
                      title: [template.title, Validators.required]
                  };

        this.form = this.fb.group(formGroupAttrs);

        this.isFormValid$ = this.form.valueChanges.pipe(
            map(() => {
                return (
                    JSON.stringify(this.form.value) !== JSON.stringify(template) && this.form.valid
                );
            }),
            startWith(false)
        );

        // Handle keyboard shortcuts (Cmd/Ctrl+Enter to save)
        fromEvent(this.el.nativeElement, 'keydown')
            .pipe(takeUntil(this.destroy$))
            .subscribe((keyboardEvent: KeyboardEvent) => {
                const nodeName = (keyboardEvent.target as Element).nodeName;
                const hasFormChanged =
                    JSON.stringify(this.form.value) !== JSON.stringify(this.originalTemplate);
                const isFormValid = this.form.valid && hasFormChanged;
                if (
                    isFormValid &&
                    nodeName !== 'TEXTAREA' &&
                    keyboardEvent.key === 'Enter' &&
                    (keyboardEvent.metaKey || keyboardEvent.altKey)
                ) {
                    keyboardEvent.preventDefault();
                    this.onSave();
                }
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * Handle save button
     *
     * @memberof DotTemplatePropsComponent
     */
    onSave(): void {
        this.config.data?.onSave?.(this.form.value);
        this.ref.close(false);
    }

    /**
     * Handle cancel button
     *
     * @memberof DotTemplatePropsComponent
     */
    onCancel(): void {
        this.ref.close(true);
    }
}
