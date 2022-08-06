import { Component, OnInit, OnDestroy } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import {
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@services/dot-push-publish-filters/dot-push-publish-filters.service';
import { Observable, of, Subject } from 'rxjs';
import { catchError, map, take, takeUntil } from 'rxjs/operators';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { getDownloadLink } from '@shared/dot-utils';

enum DownloadType {
    UNPUBLISH = 'unpublish',
    PUBLISH = 'publish'
}

const DOWNLOAD_URL = '/api/bundle/_generate';

@Component({
    selector: 'dot-download-bundle-dialog',
    templateUrl: './dot-download-bundle-dialog.component.html',
    styleUrls: ['./dot-download-bundle-dialog.component.scss']
})
export class DotDownloadBundleDialogComponent implements OnInit, OnDestroy {
    downloadOptions: SelectItem[];
    filterOptions: SelectItem[];
    dialogActions: DotDialogActions;
    form: UntypedFormGroup;
    showDialog = false;
    errorMessage = '';

    private currentFilterKey: string;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private filters: SelectItem[] = null;

    constructor(
        public fb: UntypedFormBuilder,
        private dotMessageService: DotMessageService,
        private dotPushPublishFiltersService: DotPushPublishFiltersService,
        private dotDownloadBundleDialogService: DotDownloadBundleDialogService
    ) {}

    ngOnInit() {
        this.dotDownloadBundleDialogService.showDialog$
            .pipe(takeUntil(this.destroy$))
            .subscribe((bundleId: string) => {
                if (this.filters) {
                    this.initDialog(bundleId);
                } else {
                    this.loadFilters()
                        .pipe(take(1))
                        .subscribe((options: SelectItem[]) => {
                            this.filters = options;
                            this.downloadOptions = this.getDownloadOptions();
                            this.initDialog(bundleId);
                        });
                }
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * hide the dialog
     * @memberof DotDownloadBundleDialogComponent
     */
    close(): void {
        this.showDialog = false;
    }

    /**
     * show the native download file dialog in the browser
     * @memberof DotDownloadBundleDialogComponent
     */
    handleSubmit(): void {
        if (this.form.valid) {
            this.errorMessage = '';
            const value = this.form.value;
            const bundleForm = {};
            bundleForm['bundleId'] = value.bundleId;
            bundleForm['operation'] =
                value.downloadOptionSelected === DownloadType.PUBLISH ? '0' : '1';
            bundleForm['filterKey'] = value.filterKey;

            this.dialogActions.accept.disabled = true;
            this.dialogActions.accept.label = this.dotMessageService.get(
                'download.bundle.downloading'
            );
            this.dialogActions.cancel.disabled = true;
            this.downloadFile(bundleForm);
        }
    }

    private getDownloadOptions(): SelectItem[] {
        return [
            {
                label: this.dotMessageService.get('download.bundle.publish'),
                value: DownloadType.PUBLISH
            },
            {
                label: this.dotMessageService.get('download.bundle.unPublish'),
                value: DownloadType.UNPUBLISH
            }
        ];
    }

    private initDialog(bundleId: string): void {
        this.setDialogActions();
        this.errorMessage = '';
        this.filterOptions = this.filters;
        this.form = this.fb.group({
            downloadOptionSelected: [this.downloadOptions[0].value, [Validators.required]],
            filterKey: this.currentFilterKey,
            bundleId: bundleId
        });
        this.listenForChanges();
        this.showDialog = true;
    }

    private loadFilters(): Observable<SelectItem[]> {
        return this.dotPushPublishFiltersService.get().pipe(
            map((options: DotPushPublishFilter[]) => {
                this.currentFilterKey = options
                    .filter(({ defaultFilter }: DotPushPublishFilter) => defaultFilter)
                    .map(({ key }: DotPushPublishFilter) => key)
                    .join();

                return options
                    .map((filter: DotPushPublishFilter) => {
                        return {
                            label: filter.title,
                            value: filter.key
                        };
                    })
                    .sort((a: SelectItem, b: SelectItem) => {
                        if (a.label > b.label) {
                            return 1;
                        }

                        if (a.label < b.label) {
                            return -1;
                        }

                        // a must be equal to b
                        return 0;
                    });
            }),
            catchError(() => of([]))
        );
    }

    private setDialogActions(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.handleSubmit();
                },
                label: this.dotMessageService.get('download.bundle.download'),
                disabled: false
            },
            cancel: {
                action: () => {
                    this.close();
                },
                label: this.dotMessageService.get('dot.common.cancel'),
                disabled: false
            }
        };
    }

    private listenForChanges(): void {
        this.form
            .get('downloadOptionSelected')
            .valueChanges.pipe(takeUntil(this.destroy$))
            .subscribe((state: string) => {
                this.handleDropDownState(state);
            });
    }

    private handleDropDownState(state: string): void {
        const filterKey = this.form.controls['filterKey'];
        if (state === DownloadType.UNPUBLISH) {
            filterKey.disable();
            filterKey.setValue('');
            this.filterOptions = [];
        } else {
            this.filterOptions = this.filters;
            filterKey.enable();
            filterKey.setValue(this.currentFilterKey);
        }
    }

    private downloadFile(bundleForm: Record<string, unknown>) {
        let fileName = '';

        fetch(DOWNLOAD_URL, {
            method: 'POST',
            mode: 'cors',
            cache: 'no-cache',
            headers: {
                'Content-Type': 'application/json'
            },

            body: JSON.stringify(bundleForm) // body data type must match "Content-Type" header
        })
            .then((res: Response) => {
                const contentDisposition = res.headers.get('content-disposition');
                fileName = this.getFilenameFromContentDisposition(contentDisposition);

                return res.blob();
            })
            .then((blob: Blob) => {
                getDownloadLink(blob, fileName).click();
                this.close();
            })
            .catch(() => {
                this.setDialogActions();
                this.errorMessage = this.dotMessageService.get('download.bundle.error');
            });
    }

    private getFilenameFromContentDisposition(contentDisposition: string): string {
        const key = 'filename=';

        return contentDisposition.slice(contentDisposition.indexOf(key) + key.length);
    }
}
