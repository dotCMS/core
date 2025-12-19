import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    AfterViewChecked,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    inject
} from '@angular/core';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { takeUntil } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCopyContentTypeDialogFormFields, DotDialogActions } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSiteComponent,
    DotValidators
} from '@dotcms/ui';

import { DotMdIconSelectorComponent } from '../../../../../view/components/_common/dot-md-icon-selector/dot-md-icon-selector.component';
import { DotCMSAssetDialogCopyFields } from '../../dot-content-type.store';

@Component({
    selector: 'dot-content-type-copy-dialog',
    templateUrl: './dot-content-type-copy-dialog.component.html',
    styleUrls: ['./dot-content-type-copy-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DialogModule,
        ButtonModule,
        InputTextModule,
        DotFieldValidationMessageComponent,
        DotMdIconSelectorComponent,
        DotAutofocusDirective,
        DotFieldRequiredDirective,
        DotMessagePipe,
        DotSiteComponent
    ]
})
export class DotContentTypeCopyDialogComponent implements OnInit, AfterViewChecked, OnDestroy {
    private readonly fb = inject(UntypedFormBuilder);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly cd = inject(ChangeDetectorRef);
    private readonly destroy$ = new Subject<boolean>();

    dialogActions: DotDialogActions;
    inputNameWithType = '';
    dialogTitle = '';
    isVisibleDialog = false;

    @Input()
    isSaving$ = new Observable<boolean>();
    @Output() cancelBtn = new EventEmitter<boolean>();

    @Output()
    validFormFields = new EventEmitter<DotCopyContentTypeDialogFormFields>();
    form!: UntypedFormGroup;

    constructor() {
        this.initForm();
    }

    get variableControl() {
        return this.form.get('variable') as UntypedFormControl;
    }

    get nameControl() {
        return this.form.get('name') as UntypedFormControl;
    }

    /**
     * Open the copy dialog
     * @param DotCMSAssetDialogCopyFields config
     * @returns void
     * @memberof DotContentTypeCopyDialogComponent
     */
    openDialog(config: DotCMSAssetDialogCopyFields) {
        this.inputNameWithType = this.getNameFieldLabel(config.baseType);
        this.dialogTitle = config.title;
        this.isVisibleDialog = true;
        if (config.data) {
            this.patchForm(config.data);
        }
    }

    ngOnInit(): void {
        this.setDialogConfig();
    }

    /**
     * Emit all the values of the form only if all valid
     *
     * @memberof DotContentTypeCopyDialogComponent
     */
    submitForm() {
        if (this.form.valid) {
            this.validFormFields.emit(this.form.value);
        }
    }

    /**
     * Emit the action of close dialog
     *
     * @memberof DotContentTypeCopyDialogComponent
     */
    closeDialog(): void {
        this.cancelBtn.emit(true);
        this.initForm();
        this.isVisibleDialog = false;
    }

    // Due site selector problems this make the site selector shows second time the dialogs opens
    ngAfterViewChecked() {
        this.cd.detectChanges();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private setDialogConfig(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.submitForm();
                },
                label: this.dotMessageService.get('contenttypes.content.copy'),
                disabled: true
            },
            cancel: {
                action: () => {
                    this.closeDialog();
                },
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle.form.cancel')
            }
        };

        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.dialogActions = {
                ...this.dialogActions,
                accept: {
                    ...this.dialogActions.accept,
                    disabled: !this.form.valid
                }
            };
            this.cd.markForCheck();
        });
    }

    private initForm(): void {
        this.form = this.fb.group({
            name: ['', [Validators.required]],
            variable: ['', [DotValidators.alphaNumeric, Validators.maxLength(255)]],
            folder: [''],
            host: [''],
            icon: ['']
        });
    }

    private getNameFieldLabel(type: string): string {
        return `${this.dotMessageService.get(
            `contenttypes.content.${type.toLowerCase()}`
        )} ${this.dotMessageService.get('contenttypes.form.name')}`;
    }

    private patchForm(formData: { icon: string; host: string }) {
        this.form.patchValue(formData);
    }
}
