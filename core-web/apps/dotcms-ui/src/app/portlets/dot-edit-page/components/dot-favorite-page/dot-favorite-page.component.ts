import { Observable, Subject, throwError } from 'rxjs';
import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { finalize, map, switchMap, take, takeUntil } from 'rxjs/operators';
import { DomSanitizer } from '@angular/platform-browser';
import { DotCMSTempFile, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '@dotcms/app/api/services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotRolesService } from '@dotcms/app/api/services/dot-roles/dot-roles.service';
import { DotRole } from '@dotcms/app/shared/models/dot-role/dot-role.model';

export interface DotFavoritePage {
    isAdmin?: boolean;
    thumbnail?: Blob;
    title: string;
    url: string;
    order: number;
    deviceWidth?: number;
    pageRenderedHtml?: string;
    deviceId?: string;
    languageId?: string;
}

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
    imgHeight = (this.config.data.page.deviceWidth || 1024) / this.imgRatio43;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private ref: DynamicDialogRef,
        private config: DynamicDialogConfig,
        private fb: FormBuilder,
        private sanitizer: DomSanitizer,
        private dotTempFileUploadService: DotTempFileUploadService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotRolesService: DotRolesService
    ) {}

    ngOnInit(): void {
        const { page }: { page: DotFavoritePage } = this.config.data;
        this.pageRenderedHtml = page.pageRenderedHtml;
        this.isAdmin = page.isAdmin;

        console.log('+++imgWidth', this.imgWidth, this.imgHeight);
        console.log('______oage', page);

        this.dotRolesService
            .search()
            .pipe(take(1))
            .subscribe((roles: DotRole[]) => {
                console.log('**roles', roles);
                this.currentUserRole = roles.find((item: DotRole) => item.name === 'Current User');
                this.roleOptions = roles.filter((item: DotRole) => item.name !== 'Current User');

                // this.options = this.decorateLabels(languages);
                // this.disabled = this.options.length === 0;
            });

        const url =
            `${page.url}?language_id=${page.languageId}` +
            (page.deviceId ? `&device_id=${page.deviceId}` : '');
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
                return (
                    // JSON.stringify(this.form.value) !== JSON.stringify(template) &&
                    this.form.valid
                );
            })
            // startWith(false)
        );

        setTimeout(() => {
            const dotHtmlToImageElement = document.querySelector('dot-html-to-image');
            dotHtmlToImageElement.addEventListener('pageThumbnail', (event: CustomEvent) => {
                console.log('=====updateThumbnailForm', event.detail);
                console.log('=====form', this.form.getRawValue());

                this.form.setValue({
                    ...this.form.getRawValue(),
                    thumbnail: event.detail
                });
                console.log('=====form', this.form);
            });
        }, 0);
    }

    updateThumbnailForm(e: Event): void {
        console.log('=====updateThumbnailForm', e);
    }

    takeScreenshot(renderedWidth?: number): void {
        const ratio43 = 1.333;
        const renderedHeight = renderedWidth / ratio43;

        const iframe = document.querySelector('iframe');

        // TODO: get and send DEVICE param width
        const message = {
            message: 'html2canvas',
            height: renderedHeight,
            width: renderedWidth
        };

        const channel = new MessageChannel();

        // Listen for messages on port1
        channel.port1.onmessage = (e) => {
            // output.innerHTML = e.data;
            console.log('*** onMesssage', e);
            this.form.get('thumbnail').setValue(e.data.file);

            this.pageThumbnail = this.sanitizer.bypassSecurityTrustUrl(
                URL.createObjectURL(e.data.file)
            ) as string;
            console.log('*** onMesssage img', this.pageThumbnail);

            console.log('/====', this.pageThumbnail, this.form.get('thumbnail'));

            const imgTag: HTMLImageElement = document.querySelector('#previewImgHtmlTag');
            imgTag.src = this.pageThumbnail;

            // const fileURL = URL.createObjectURL(e.data.file);
            // window.open(fileURL);
        };

        // Transfer port2 to the iframe
        // iframe.contentWindow.postMessage(message, '*', [channel.port2]);

        // Handle messages received on port1
        // function onMessage(e) {
        //     // output.innerHTML = e.data;
        //     console.log('*** onMesssage', e);
        //     this.pageThumbnail = URL.createObjectURL(e.data.file);
        //     setTimeout(() => {
        //         const imgTag: HTMLImageElement = document.querySelector('#previewImgHtmlTag');
        //         imgTag.src = this.pageThumbnail;

        //     }, 1000);

        //     // const fileURL = URL.createObjectURL(e.data.file);
        //     // window.open(fileURL);
        // }
    }

    /**
     * Handle save button
     *
     * @memberof DotTemplatePropsComponent
     */
    onSave(): void {
        const value = this.form.value;
        const file = new File([value.thumbnail], 'image.png');
        const individualPermissions = {
            READ: []
        };
        individualPermissions.READ = value.permissions
            ? value.permissions.map((item) => item.id)
            : [];
        individualPermissions.READ.push(this.currentUserRole.id);

        // REQUEST BEGIN
        this.dotTempFileUploadService
            .upload(file)
            .pipe(
                switchMap(([{ id, image }]: DotCMSTempFile[]) => {
                    if (!image) {
                        return throwError(
                            'error uploading img'
                            // this.dotMessageService.get(
                            //     'templates.properties.form.thumbnail.error.invalid.url'
                            // )
                        );
                    }

                    return this.dotWorkflowActionsFireService.publishContentletAndWaitForIndex<DotCMSContentlet>(
                        'Screenshot',
                        {
                            screenshot: id,
                            title: value.title,
                            url: value.url,
                            order: value.order
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
                }
                // (err: HttpErrorResponse | string) => {
                //     const defaultError = this.dotMessageService.get(
                //         'templates.properties.form.thumbnail.error'
                //     );
                //     this.error = typeof err === 'string' ? err : defaultError;
                // }
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
