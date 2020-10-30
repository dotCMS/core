import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { FormGroup, FormBuilder, FormControl, Validators } from '@angular/forms';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import {
    DotApps,
    DotAppsExportConfiguration,
    DotAppsSites
} from '@shared/models/dot-apps/dot-apps.model';
import { Subject } from 'rxjs/internal/Subject';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'dot-apps-export-dialog',
    templateUrl: './dot-apps-export-dialog.component.html'
})
export class DotAppsExportDialogComponent implements OnInit, OnDestroy {
    @Input() app?: DotApps;
    @Input() site?: DotAppsSites;

    showExportDialog = false;
    form: FormGroup;
    dialogExportActions: DotDialogActions;
    exportErrorMessage: string;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotAppsService: DotAppsService,
        private dotMessageService: DotMessageService,
        private fb: FormBuilder
    ) {}

    ngOnInit() {
        this.form = this.fb.group({
            password: new FormControl('', Validators.required)
        });

        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.dialogExportActions = {
                ...this.dialogExportActions,
                accept: {
                    ...this.dialogExportActions.accept,
                    disabled: !this.form.valid
                }
            };
        });
        this.setExportDialogActions();
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
        this.exportErrorMessage = '';
        this.form.reset();
        this.site = null;
        this.showExportDialog = false;
    }

    private setExportDialogActions(): void {
        this.dialogExportActions = {
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
                                this.exportErrorMessage = this.dotMessageService.get(
                                    'apps.confirmation.export.error'
                                );
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

    private getAllKeySitesConfig(): { [key: string]: string[] } {
        const keySitesConf = {};
        if (this.app) {
            this.app.sites.forEach((site: DotAppsSites) => {
                if (site.configured) {
                    keySitesConf[site.id] = [this.app.key];
                }
            });
        }
        return keySitesConf;
    }
}
