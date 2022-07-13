import { Observable, Subject, throwError } from 'rxjs';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { finalize, map, switchMap, take, takeUntil } from 'rxjs/operators';
import { DotCMSTempFile, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '@dotcms/app/api/services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotRolesService } from '@dotcms/app/api/services/dot-roles/dot-roles.service';
import { DotRole } from '@dotcms/app/shared/models/dot-role/dot-role.model';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { HttpErrorResponse } from '@angular/common/http';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';

export interface DotFavoritePage {
    isAdmin?: boolean;
    thumbnail?: Blob;
    title: string;
    url: string;
    order: number;
    deviceHeight?: number;
    deviceWidth?: number;
    pageRenderedHtml?: string;
    deviceId?: string;
    hostId?: string;
    languageId?: string;
}

const CMS_OWNER_ROLE_ID = '6b1fa42f-8729-4625-80d1-17e4ef691ce7';

@Component({
    selector: 'dot-favorite-page',
    templateUrl: 'dot-favorite-page.component.html'
})
export class DotFavoritePageComponent implements OnInit, OnDestroy {
    form: FormGroup;
    isFormValid$: Observable<boolean>;
    pageThumbnail: string;
    roleOptions: DotRole[];
    currentUserRole: DotRole;

    pageRenderedHtml: string;
    isAdmin: boolean;

    imgRatio43 = 1.333;
    imgWidth = this.config.data.page.deviceWidth || 1024;
    imgHeight = this.config.data.page.deviceHeight || (this.config.data.page.deviceWidth || 1024) / this.imgRatio43;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private ref: DynamicDialogRef,
        private config: DynamicDialogConfig,
        private fb: FormBuilder,
        private dotMessageService: DotMessageService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotTempFileUploadService: DotTempFileUploadService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotRolesService: DotRolesService
    ) {}

    ngOnInit(): void {
        const { page }: { page: DotFavoritePage } = this.config.data;
        this.pageRenderedHtml = page.pageRenderedHtml;
        this.isAdmin = page.isAdmin;

        console.log('****page', page)

        this.dotRolesService
            .search()
            .pipe(take(1))
            .subscribe((roles: DotRole[]) => {
                this.currentUserRole = roles.find((item: DotRole) => item.name === 'Current User');
                this.roleOptions = roles.filter((item: DotRole) => item.name !== 'Current User');
            });

        const url =
            `${page.url}?language_id=${page.languageId}` +
            (page.deviceId ? `&device_id=${page.deviceId}` : '') +
            (page.hostId ? `&host_id=${page.hostId}` : '');

        const formGroupAttrs = {
            thumbnail: ['', Validators.required],
            title: [page.title, Validators.required],
            url: [url, Validators.required],
            order: [page.order, Validators.required],
            permissions: []
        };

        this.form = this.fb.group(formGroupAttrs);

        this.isFormValid$ = this.form.valueChanges.pipe(
            takeUntil(this.destroy$),
            map(() => {
                return this.form.valid;
            })
        );

        // This is needed to wait until the Web Component is rendered
        setTimeout(() => {
            const dotHtmlToImageElement = document.querySelector('dot-html-to-image');
            dotHtmlToImageElement.addEventListener('pageThumbnail', (event: CustomEvent) => {
                this.form.setValue({
                    ...this.form.getRawValue(),
                    thumbnail: event.detail
                });
            });
        }, 0);
    }

    /**
     * Handle save button
     *
     * @memberof DotFavoritePageComponent
     */
    onSave(): void {
        const formValue = this.form.value;
        const file = new File([formValue.thumbnail], 'image.png');
        const individualPermissions = {
            READ: []
        };
        individualPermissions.READ = formValue.permissions
            ? formValue.permissions.map((role: DotRole) => role.id)
            : [];
        individualPermissions.READ.push(this.currentUserRole.id, CMS_OWNER_ROLE_ID);

        this.dotTempFileUploadService
            .upload(file)
            .pipe(
                switchMap(([{ id, image }]: DotCMSTempFile[]) => {
                    if (!image) {
                        return throwError(
                            this.dotMessageService.get('favoritePage.dialog.error.tmpFile.upload')
                        );
                    }

                    return this.dotWorkflowActionsFireService.publishContentletAndWaitForIndex<DotCMSContentlet>(
                        'Screenshot',
                        {
                            screenshot: id,
                            title: formValue.title,
                            url: formValue.url,
                            order: formValue.order
                        },
                        individualPermissions
                    );
                }),
                take(1),
                finalize(() => {
                    console.log('=== publicado');
                    // this.loading = false;
                })
            )
            .subscribe(
                () => {
                    this.config.data?.onSave?.(this.form.value);
                    this.ref.close(false);
                    // this.asset = asset;
                    // this.propagateChange(this.asset.identifier);
                },
                (error: HttpErrorResponse) => {
                    this.httpErrorManagerService.handle(error);
                }
            );
    }

    /**
     * Handle cancel button
     *
     * @memberof DotTemplatePropsComponent
     */
    onCancel(): void {
        this.ref.close(true);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
