import {
    Component,
    OnInit,
    Input,
    EventEmitter,
    Output,
    OnChanges,
    OnDestroy
} from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

// import * as htmlToImage from 'html-to-image';
// import { toPng, toJpeg, toBlob, toPixelData, toSvg } from 'html-to-image';

import { DialogService } from 'primeng/dynamicdialog';
import {
    DotFavoritePage,
    DotFavoritePageComponent
} from '../../../components/dot-favorite-page/dot-favorite-page.component';
// import { finalize, switchMap, take } from 'rxjs/operators';
// import { DotTemplateItem } from '@dotcms/app/portlets/dot-templates/dot-template-create-edit/store/dot-template.store';

// interface DotCMSStartPage {
//     contentType: string;
// }
@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges, OnDestroy {
    @Input() pageState: DotPageRenderState;

    @Output() cancel = new EventEmitter<boolean>();

    @Output() actionFired = new EventEmitter<DotCMSContentlet>();

    @Output() whatschange = new EventEmitter<boolean>();

    isEnterpriseLicense$: Observable<boolean>;
    showWhatsChanged: boolean;
    apiLink: string;
    // screenShot: string;
    pageRenderedHtml: string;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotLicenseService: DotLicenseService,
        private dialogService: DialogService
    ) {}

    ngOnInit() {
        console.log('***pagsetat', this.pageState);
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();

        this.apiLink = `api/v1/page/render${this.pageState.page.pageURI}?language_id=${this.pageState.page.languageId}`;
    }

    ngOnChanges(): void {
        console.log('**pagestate', this.pageState.params.viewAs.device?.cssWidth);
        console.log('**pagestate', this.pageState);

        this.updateRenderedHtml();

        this.showWhatsChanged =
            this.pageState.state.mode === DotPageMode.PREVIEW &&
            !('persona' in this.pageState.viewAs);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Hide what's change when state change
     *
     * @memberof DotEditPageToolbarComponent
     */
    stateChange(): void {
        if (this.showWhatsChanged) {
            this.showWhatsChanged = false;
            this.whatschange.emit(this.showWhatsChanged);
        }
    }

    bookmarkPage(): void {
        this.dialogService.open(DotFavoritePageComponent, {
            header: 'Add Start Page',
            width: '40rem',
            data: {
                page: {
                    isAdmin: this.pageState.user.admin || null,
                    title: this.pageState.params.page?.title || null,
                    url: this.pageState.params.page?.pageURI || null,
                    order: 1,
                    deviceWidth: this.pageState.params.viewAs.device?.cssWidth || null,
                    pageRenderedHtml: this.pageRenderedHtml || null,
                    deviceId: this.pageState.params.viewAs.device?.identifier || null,
                    languageId: this.pageState.params.viewAs.language.id || null
                },
                onSave: (value: DotFavoritePage) => {
                    console.log('**save', value);
                }
            }
        });

        /*
        const iframe = document.querySelector('iframe');
        console.log('**llego', iframe.contentWindow.document.body.innerHTML);

        // TODO: get and send DEVICE param width
        const message = {
            message: 'html2canvas',
        };

        // iframe.contentWindow.postMessage(message, '*');


        const channel = new MessageChannel();
        // var output = document.querySelector('.output');
        // var iframe = document.querySelector('iframe');

        // Wait for the iframe to load
        // iframe.addEventListener("load", onLoad);

        // function onLoad() {
        // Listen for messages on port1
        channel.port1.onmessage = onMessage;

        // Transfer port2 to the iframe
        iframe.contentWindow.postMessage(message, '*', [channel.port2]);
        // }

        // Handle messages received on port1
        function onMessage(e) {
            // output.innerHTML = e.data;
            console.log('*** onMesssage', e)

            // const iFrame = document.createElement("iframe");
            const fileURL = URL.createObjectURL(e.data.file);
            window.open(fileURL);
        }
*/
        // const fontEmbedCss = await htmlToImage.getFontEmbedCSS(iframe.contentWindow.document.body);
        // console.log(fontEmbedCss);

        // htmlToImage
        //     .toCanvas(iframe.contentWindow.document.body, { fontEmbedCSS: fontEmbedCss })
        //     .then( (canvas) => {
        //         document.body.appendChild(canvas);
        //         // console.log(canvas);
        //     }).catch(function (error) {
        //         console.error('oops, something went wrong!', error);
        //       });

        // const bodi = <HTMLElement>document.querySelector('dot-main-component');
        /*
        html2canvas(iframe.contentWindow.document.querySelector('html'), {
        // html2canvas(bodi, {
                windowWidth: 1280, // window size were html will be loaded
                imageTimeout: 15000,
            // removeContainer: false,
            // useCORS: true,
            // foreignObjectRendering: true,
            // windowHeight: 1400,
            height: 1024 // up to how many px you want to get
            // scale: 0.3
        }).then((canvas) => {
            this.screenShot = canvas.toDataURL();
            console.log('***canvas', this.screenShot);

            canvas.toBlob((blob) => {
                const file = new File([blob], 'image.png');
                console.log('***canvas file', file);

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
                                    title: 'test1',
                                    url: 'url test',
                                    order: 1
                                }
                            );
                        }),
                        take(1),
                        finalize(() => {
                            console.log('=== publicado');
                            // this.loading = false;
                        })
                    )
                    .subscribe
                    // (asset: DotCMSTemplateThumbnail) => {
                    //     this.asset = asset;
                    //     this.propagateChange(this.asset.identifier);
                    // },
                    // (err: HttpErrorResponse | string) => {
                    //     const defaultError = this.dotMessageService.get(
                    //         'templates.properties.form.thumbnail.error'
                    //     );
                    //     this.error = typeof err === 'string' ? err : defaultError;
                    // }
                    ();
            });

            // iframe.contentWindow.document.body.appendChild(canvas);
            document.body.appendChild(canvas);
        });
*/
    }
    private updateRenderedHtml(): void {
        this.pageRenderedHtml =
            this.pageState?.params.viewAs.mode === DotPageMode.PREVIEW
                ? this.pageState.params.page.rendered
                : this.pageRenderedHtml;
    }
}
