import { forkJoin, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

import { catchError, take, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotDialogActions, DotFieldVariable } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFieldVariablesService } from '../fields/dot-content-type-fields-variables/services/dot-field-variables.service';

@Component({
    selector: 'dot-binary-settings',
    imports: [
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
    @Input() isVisible = false;

    @Output() changeControls = new EventEmitter<DotDialogActions>();
    @Output() valid = new EventEmitter<boolean>();
    @Output() save = new EventEmitter<DotFieldVariable[]>();

    form: FormGroup;
    protected readonly systemOptions = [
        {
            key: 'allowURLImport',
            message: 'binary-field.settings.system.options.allow.url.import'
        },
        {
            key: 'allowCodeWrite',
            message: 'binary-field.settings.system.options.allow.code.write'
        },
        {
            key: 'allowGenerateImg',
            message: 'binary-field.settings.system.options.allow.generate.img'
        }
    ];
    private readonly fb: FormBuilder = inject(FormBuilder);
    private readonly fieldVariablesService = inject(DotFieldVariablesService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly destroyRef = inject(DestroyRef);
    private FIELD_VARIABLES: Record<string, DotFieldVariable> = {};

    ngOnChanges(changes: SimpleChanges) {
        const { isVisible } = changes;
        if (isVisible?.currentValue) {
            this.changeControls.emit(this.dialogActions());
        }
    }

    ngOnInit(): void {
        this.form = this.fb.group({
            accept: '',
            systemOptions: this.fb.group({
                allowURLImport: true,
                allowCodeWrite: true,
                allowGenerateImg: true
            })
        });

        this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
            this.valid.emit(this.form.valid);
        });

        this.fieldVariablesService.load(this.field).subscribe({
            next: (fieldVariables: DotFieldVariable[]) => {
                fieldVariables.forEach((variable) => {
                    const { key, value } = variable;
                    const control = this.form.get(key);
                    if (control instanceof FormGroup) {
                        const systemOptions = JSON.parse(value);

                        this.systemOptions.forEach(({ key }) => {
                            control.get(key)?.setValue(systemOptions[key]);
                        });
                    } else {
                        control.setValue(value);
                    }

                    this.FIELD_VARIABLES = { ...this.FIELD_VARIABLES, [key]: variable };
                });
            }
        });
    }

    saveSettings(): void {
        const updateActions = Object.keys(this.form.controls).map((key) => {
            const control = this.form.get(key);

            const value =
                control instanceof FormGroup ? JSON.stringify(control.value) : control.value;
            const fieldVariable: DotFieldVariable = {
                ...this.FIELD_VARIABLES[key],
                key,
                value
            };

            const controlIsEmpty = !value && !this.FIELD_VARIABLES[key]?.value;

            if (controlIsEmpty) {
                return of({});
            }

            return (
                value
                    ? this.fieldVariablesService.save(this.field, fieldVariable)
                    : this.fieldVariablesService.delete(this.field, fieldVariable)
            ).pipe(tap((variable) => (this.FIELD_VARIABLES[key] = variable))); // Update Variable Reference
        });

        forkJoin(updateActions)
            .pipe(
                take(1),
                catchError((err: HttpErrorResponse) =>
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1))
                )
            )
            .subscribe((value: DotFieldVariable[]) => {
                this.form.markAsPristine();
                this.save.emit(value);
            });
    }

    private dialogActions() {
        return {
            cancel: {
                label: this.dotMessageService.get('contenttypes.dropzone.action.cancel')
            },
            accept: {
                action: () => this.saveSettings(),
                disabled: this.form.invalid || this.form.pristine,
                label: this.dotMessageService.get('contenttypes.dropzone.action.save')
            }
        };
    }
}
