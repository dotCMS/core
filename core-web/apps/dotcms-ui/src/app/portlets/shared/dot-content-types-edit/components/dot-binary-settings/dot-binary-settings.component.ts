import { forkJoin, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    inject
} from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

import { catchError, tap, take } from 'rxjs/operators';

import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

@Component({
    selector: 'dot-binary-settings',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        InputTextModule,
        InputSwitchModule,
        DividerModule,
        DotMessagePipe
    ],
    templateUrl: './dot-binary-settings.component.html',
    styleUrl: './dot-binary-settings.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinarySettingsComponent implements OnInit, OnChanges {
    @Input() field: DotCMSContentTypeField;
    @Input() isVisible: boolean = false;

    @Output() changeControls = new EventEmitter<DotDialogActions>();
    @Output() valid = new EventEmitter<boolean>();
    @Output() save = new EventEmitter<DotFieldVariable[]>();

    protected form: FormGroup;

    private fb: FormBuilder = inject(FormBuilder);
    private fieldVariablesService = inject(DotFieldVariablesService);
    private dotMessageService = inject(DotMessageService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    private settingsMap = {
        accept: {
            key: 'accept',
            variable: null
        },
        systemOptions: {
            key: 'systemOptions',
            variable: null
        }
    };

    get settings() {
        return Object.values(this.settingsMap);
    }

    protected readonly systemOptions = [
        {
            key: 'allowURLImport',
            message: 'binary-field.settings.system.options.allow.url.import',
            variable: null
        },
        {
            key: 'allowCodeWrite',
            message: 'binary-field.settings.system.options.allow.code.write',
            variable: null
        },
        {
            key: 'allowFileNameEdit',
            message: 'binary-field.settings.system.options.allow.file.name.edit',
            variable: null
        }
    ];

    ngOnChanges(changes: SimpleChanges) {
        const { isVisible } = changes;
        if (isVisible?.currentValue) {
            this.changeControls.emit(this.dialogActions());
        }
    }

    ngOnInit(): void {
        this.form = this.fb.group({
            accept: '',
            // This is an object called systemOptions, this is going to the backend as a field variable
            allowURLImport: false,
            allowCodeWrite: false,
            allowFileNameEdit: false
        });

        this.form.valueChanges.subscribe(() => {
            this.valid.emit(this.form.valid);
        });

        this.fieldVariablesService
            .load(this.field)
            .subscribe((fieldVariables: DotFieldVariable[]) => {
                fieldVariables.forEach((variable) => {
                    const { key, value } = variable;

                    if (key === 'accept') {
                        this.settingsMap.accept.variable = variable;
                        this.form.get(key)?.setValue(value);
                    } else if (key === 'systemOptions') {
                        this.settingsMap.systemOptions.variable = variable;
                        const systemOptions = JSON.parse(value);

                        this.systemOptions.forEach(({ key }) => {
                            this.form.get(key)?.setValue(systemOptions[key]);
                        });
                    }
                });
            });
    }

    saveSettings(): void {
        const updateActions = this.settings.map(({ variable, key }) => {
            let fieldVariable: DotFieldVariable;
            let value: string | boolean;

            if (key === 'accept') {
                value = this.form.get(key).value;
                fieldVariable = {
                    ...variable,
                    key,
                    value
                };
            } else if (key === 'systemOptions') {
                value = JSON.stringify(
                    this.systemOptions.reduce((acc, { key }) => {
                        acc[key] = this.form.get(key).value;

                        return acc;
                    }, {})
                );

                fieldVariable = {
                    ...variable,
                    key,
                    value
                };
            }

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
        });

        forkJoin(updateActions)
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
