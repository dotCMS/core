import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewChild,
    inject
} from '@angular/core';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { take, takeUntil } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    dialogAction,
    DotApp,
    DotAppsExportConfiguration,
    DotAppsImportConfiguration,
    DotAppsSite,
    DotDialogActions
} from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotDialogComponent,
    DotFieldRequiredDirective,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';

@Component({
    selector: 'dot-apps-import-export-dialog',
    templateUrl: './dot-apps-import-export-dialog.component.html',
    styleUrls: ['./dot-apps-import-export-dialog.component.scss'],
    imports: [
        CommonModule,
        ReactiveFormsModule,
        InputTextModule,
        PasswordModule,
        DotDialogComponent,
        DotAutofocusDirective,
        DotSafeHtmlPipe,
        DotFieldRequiredDirective,
        DotMessagePipe
    ]
})
export class DotAppsImportExportDialogComponent implements OnChanges, OnDestroy {
    private dotAppsService = inject(DotAppsService);
    private dotMessageService = inject(DotMessageService);
    private fb = inject(UntypedFormBuilder);

    @ViewChild('importFile') importFile: ElementRef;
    @Input() action?: string;
    @Input() app?: DotApp;
    @Input() site?: DotAppsSite;
    @Input() show? = false;
    @Output() resolved: EventEmitter<boolean> = new EventEmitter();
    @Output() shutdown: EventEmitter<boolean> = new EventEmitter();

    form: UntypedFormGroup;
    dialogActions: DotDialogActions;
    errorMessage: string;
    dialogHeaderKey = '';

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.action?.currentValue) {
            this.setDialogForm(changes.action.currentValue);
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Close the dialog and clear the form
     *
     * @memberof DotAppsConfigurationComponent
     */
    closeExportDialog(): void {
        this.errorMessage = '';
        this.form.reset();
        this.site = null;
        this.show = false;
        this.shutdown.emit();
    }

    /**
     * Updates form control value for inputFile field
     *
     * @param { File[] } files
     * @memberof DotAppsConfigurationComponent
     */
    onFileChange(files: File[]) {
        this.form.controls['importFile'].setValue(files[0] ? files[0].name : '');
    }

    /**
     * Sets dialog form based on action Import/Export
     *
     * @param { dialogAction } action
     * @memberof DotAppsConfigurationComponent
     */
    setDialogForm(action: dialogAction): void {
        if (action === dialogAction.EXPORT) {
            this.dialogHeaderKey = 'apps.confirmation.export.header';
            this.form = this.fb.group({
                password: new UntypedFormControl('', Validators.required)
            });
            this.setExportDialogActions();
        } else if (action === dialogAction.IMPORT) {
            this.dialogHeaderKey = 'apps.confirmation.import.header';
            this.form = this.fb.group({
                password: new UntypedFormControl('', Validators.required),
                importFile: new UntypedFormControl('', Validators.required)
            });
            this.setImportDialogActions();
        }

        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.dialogActions = {
                ...this.dialogActions,
                accept: {
                    ...this.dialogActions.accept,
                    disabled: !this.form.valid
                }
            };
        });
    }

    private setExportDialogActions(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    const requestConfiguration: DotAppsExportConfiguration = {
                        password: this.form.value.password,
                        exportAll: this.app ? false : true,
                        appKeysBySite: this.site
                            ? { [this.site.id]: [this.app.key] }
                            : this.getAllKeySitesConfig()
                    };

                    this.dotAppsService
                        .exportConfiguration(requestConfiguration)
                        .then((errorMsg: string) => {
                            if (errorMsg) {
                                this.errorMessage =
                                    this.dotMessageService.get('apps.confirmation.export.error') +
                                    ': ' +
                                    errorMsg;
                            } else {
                                this.closeExportDialog();
                            }
                        });
                },
                label: this.dotMessageService.get('dot.common.dialog.accept'),
                disabled: true
            },
            cancel: {
                label: this.dotMessageService.get('dot.common.dialog.reject'),
                action: () => {
                    this.closeExportDialog();
                }
            }
        };
    }

    private setImportDialogActions(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    const requestConfiguration: DotAppsImportConfiguration = {
                        file: this.importFile.nativeElement.files[0],
                        json: { password: this.form.value.password }
                    };

                    this.dotAppsService
                        .importConfiguration(requestConfiguration)
                        .pipe(take(1))
                        .subscribe((status: string) => {
                            if (status !== '400') {
                                this.resolved.emit(true);
                                this.closeExportDialog();
                            }
                        });
                },
                label: this.dotMessageService.get('dot.common.dialog.accept'),
                disabled: true
            },
            cancel: {
                label: this.dotMessageService.get('dot.common.dialog.reject'),
                action: () => {
                    this.closeExportDialog();
                }
            }
        };
    }

    private getAllKeySitesConfig(): { [key: string]: string[] } {
        const keySitesConf = {};
        if (this.app) {
            this.app.sites.forEach((site: DotAppsSite) => {
                if (site.configured) {
                    keySitesConf[site.id] = [this.app.key];
                }
            });
        }

        return keySitesConf;
    }
}
