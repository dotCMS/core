import {
    AfterViewChecked,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import {
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotCopyContentTypeDialogFormFields } from '@dotcms/dotcms-models';
import { combineLatest, Observable, of } from 'rxjs';
import { DotCMSAssetDialogCopyFields } from '@portlets/shared/dot-content-types-listing/dot-content-type.store';
import { DotValidators } from '@shared/validators/dotValidators';
import { map } from 'rxjs/operators';

@Component({
    selector: 'dot-content-type-copy-dialog',
    templateUrl: './dot-content-type-copy-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentTypeCopyDialogComponent implements OnInit, AfterViewChecked {
    @ViewChild('dot-site-selector-field') siteSelector;
    dialogActions: DotDialogActions;
    inputNameWithType = '';
    dialogTitle = '';
    isVisibleDialog = false;

    dialogActions$: Observable<DotDialogActions>;

    @Input()
    isSaving$ = new Observable<boolean>();
    @Output() cancelBtn = new EventEmitter<boolean>();

    @Output()
    validFormFields = new EventEmitter<DotCopyContentTypeDialogFormFields>();
    form!: UntypedFormGroup;

    constructor(
        private readonly fb: UntypedFormBuilder,
        private readonly dotMessageService: DotMessageService,
        private readonly cd: ChangeDetectorRef
    ) {
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

    private setDialogConfig(): void {
        const dialogActions$: Observable<DotDialogActions> = of({
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
        });

        this.dialogActions$ = combineLatest([dialogActions$, this.form.valueChanges]).pipe(
            map(([dialogActions]) => {
                dialogActions.accept.disabled = !this.form.valid;

                return dialogActions;
            })
        );
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
