import { forkJoin, of, Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

import { catchError, take, takeUntil, tap } from 'rxjs/operators';

// Services
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { getEditorBlockOptions } from '@dotcms/block-editor';
import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

/* Uncomment this when Content Assets variable is ready
const BLOCK_EDITOR_ASSETS = [
    { label: 'Youtube Videos', code: 'videos'},
    { label: 'Images', code: 'images'},
    { label: 'PDF Files', code: 'PDF'}
]
*/

@Component({
    selector: 'dot-block-editor-settings',
    templateUrl: './dot-block-editor-settings.component.html',
    styleUrls: ['./dot-block-editor-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBlockEditorSettingsComponent implements OnInit, OnDestroy, OnChanges {
    @Output() changeControls = new EventEmitter<DotDialogActions>();
    @Output() valid = new EventEmitter<boolean>();
    @Output() save = new EventEmitter<DotFieldVariable[]>();

    @Input() field: DotCMSContentTypeField;
    @Input() isVisible = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    public form: FormGroup;
    public settingsMap = {
        allowedBlocks: {
            label: 'Allowed Blocks',
            placeholder: 'Select Blocks',
            options: getEditorBlockOptions(),
            key: 'allowedBlocks',
            variable: null
        }
        /* Uncomment this when Content Assets variable is ready
        contentAssets: {
            label: 'Allowed Content Assets',
            placeholder: 'Select Assets',
            options: BLOCK_EDITOR_ASSETS,
            key: 'contentAssets',
            required: true
        }
        */
    };

    get settings() {
        return Object.values(this.settingsMap);
    }

    constructor(
        private readonly dotHttpErrorManagerService: DotHttpErrorManagerService,
        private readonly fieldVariablesService: DotFieldVariablesService,
        private readonly dotMessageService: DotMessageService,
        private readonly fb: FormBuilder
    ) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            allowedBlocks: [null]
            /* Uncomment this when Content Assets variable is ready
            contentAssets: [null, Validators.required]
            */
        });

        this.fieldVariablesService
            .load(this.field)
            .pipe(take(1))
            .subscribe((fieldVariables: DotFieldVariable[]) => {
                fieldVariables.forEach((variable) => {
                    const { key, value } = variable;

                    if (this.form.get(key)) {
                        this.settingsMap[key].variable = variable;
                        this.form.get(key)?.setValue(value.split(','));
                    }
                });
            });

        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.valid.emit(this.form.valid);
        });
    }

    ngOnChanges(changes: SimpleChanges) {
        const { isVisible } = changes;
        if (isVisible?.currentValue) {
            this.changeControls.emit(this.dialogActions());
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    saveSettings(): void {
        forkJoin(
            this.settings.map(({ variable, key }) => {
                const value = this.form.get(key).value?.join(',');
                const fieldVariable = {
                    ...variable,
                    key,
                    value
                };

                // This is to prevent endpoints from breaking.
                // fieldVariablesService.save -> breaks if there is not current value.
                // fieldVariablesService.delete -> breaks if there is not previus exinting variable.
                if (!value && !variable) {
                    return of({});
                }

                return (
                    value
                        ? this.fieldVariablesService.save(this.field, fieldVariable)
                        : this.fieldVariablesService.delete(this.field, fieldVariable)
                ).pipe(tap((variable) => (this.settingsMap[key].variable = variable))); // Update Variable Reference
            })
        )
            .pipe(
                take(1),
                catchError((err: HttpErrorResponse) =>
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1))
                )
            )
            .subscribe((value: DotFieldVariable[]) => {
                this.save.emit(value);
                this.form.markAsPristine();
            });
    }

    private dialogActions() {
        return {
            accept: {
                action: () => this.saveSettings(),
                label: this.dotMessageService.get('contenttypes.dropzone.action.save'),
                disabled: this.form.invalid || this.form.pristine
            },
            cancel: {
                label: this.dotMessageService.get('contenttypes.dropzone.action.cancel')
            }
        };
    }
}
