import {
    Spectator,
    createComponentFactory,
    mockProvider,
    SpyObject,
    byTestId
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { ConfirmationService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import { DotResourceLinksService } from '@dotcms/data-access';
import { DotCopyButtonComponent } from '@dotcms/ui';

import { DotFileFieldPreviewComponent } from './dot-file-field-preview.component';

import { NEW_FILE_MOCK, TEMP_FILE_MOCK, NEW_FILE_EDITABLE_MOCK } from '../../../../utils/mocks';

describe('DotFileFieldPreviewComponent', () => {
    let spectator: Spectator<DotFileFieldPreviewComponent>;
    let dotResourceLinksService: SpyObject<DotResourceLinksService>;

    const createComponent = createComponentFactory({
        component: DotFileFieldPreviewComponent,
        detectChanges: false,
        providers: [provideHttpClient()],
        componentProviders: [
            ConfirmationService,
            mockProvider(DotResourceLinksService, {
                getFileResourceLinksByInode: jest.fn().mockReturnValue(
                    of({
                        configuredImageURL: 'testConfiguredImageURL',
                        idPath: 'testIdPath',
                        mimeType: 'testMimeType',
                        text: 'testText',
                        versionPath: 'testVersionPath'
                    })
                )
            })
        ]
    });

    describe('temp preview file', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    previewFile: {
                        source: 'temp',
                        file: TEMP_FILE_MOCK
                    }
                } as unknown
            });
            dotResourceLinksService = spectator.inject(DotResourceLinksService, true);
        });

        it('should be created', () => {
            spectator.detectChanges();
            expect(spectator.component).toBeTruthy();
        });

        it('should show download button for temp preview file', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('download-btn'))).toBeTruthy();
        });

        it('should render the unified thumbnail with the temp file pdf src', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('contentlet-thumbnail'))).toBeTruthy();
            expect(
                spectator.query(byTestId('dot-content-thumbnail-image')).getAttribute('src')
            ).toBe(TEMP_FILE_MOCK.thumbnailUrl);
        });

        it('should call downloadAsset when click on the download btn', () => {
            const downloadSpy = jest
                .spyOn(spectator.component, 'downloadAsset')
                .mockImplementation(jest.fn());

            const expectedUrl = `${TEMP_FILE_MOCK.referenceUrl}?force_download=true`;

            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('download-btn')));
            expect(downloadSpy).toHaveBeenCalledWith(expectedUrl);
        });

        it('should render with fallback metadata when the temp file has none (image editor save)', () => {
            spectator.setInput('previewFile', {
                source: 'temp',
                file: { ...TEMP_FILE_MOCK, metadata: undefined }
            } as unknown);
            spectator.detectChanges();

            expect(spectator.component).toBeTruthy();
            expect(spectator.query(byTestId('metadata-title'))).toHaveText(TEMP_FILE_MOCK.fileName);
        });

        it('should not show download button when referenceUrl is missing', () => {
            spectator.setInput('previewFile', {
                source: 'temp',
                file: { ...TEMP_FILE_MOCK, referenceUrl: '' }
            } as unknown);
            spectator.detectChanges();

            expect(spectator.query(byTestId('download-btn'))).toBeFalsy();
        });

        it('renders without crashing when the temp file has no metadata', () => {
            // Regression: the image-editor Save servlet can return a temp file with
            // `metadata: null`. The file-info dialog header bound `metadata.title`
            // unguarded and threw "Cannot read properties of null (reading 'title')".
            spectator.setInput('previewFile', {
                source: 'temp',
                file: { ...TEMP_FILE_MOCK, image: false, mimeType: 'unknown', metadata: null }
            } as unknown);

            expect(() => spectator.detectChanges()).not.toThrow();
            expect(spectator.component).toBeTruthy();
        });
    });

    describe('contentlet without content preview file', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    previewFile: {
                        source: 'contentlet',
                        file: NEW_FILE_MOCK.entity
                    }
                } as unknown
            });
            dotResourceLinksService = spectator.inject(DotResourceLinksService, true);
        });

        it('should be created', () => {
            spectator.detectChanges();
            expect(spectator.component).toBeTruthy();
        });

        it('should render the unified thumbnail with the contentlet image src', () => {
            spectator.detectChanges();

            const { inode, modDate } = NEW_FILE_MOCK.entity;

            expect(spectator.query(byTestId('contentlet-thumbnail'))).toBeTruthy();
            expect(
                spectator.query(byTestId('dot-content-thumbnail-image')).getAttribute('src')
            ).toBe(`/dA/${inode}/asset/500w/50q?r=${modDate}`);
        });

        it('should show proper metadata', () => {
            spectator.detectChanges();

            const { title, width, height } = NEW_FILE_MOCK.entity.assetMetaData;

            const metadataTitleElement = spectator.query(byTestId('metadata-title'));
            const metadataDimensionsElement = spectator.query(byTestId('metadata-dimensions'));
            const metadataFileSizeElement = spectator.query(byTestId('metadata-file-size'));

            expect(metadataTitleElement).toHaveText(title);
            expect(metadataDimensionsElement).toHaveText(`${width} x ${height}`);
            expect(metadataFileSizeElement).toHaveText('3.70 MB');
        });

        it('should show a dialog when click on the proper btn', async () => {
            spectator.detectChanges();

            const infoBtnElement = spectator.query(byTestId('info-btn'));

            const dialogComponent = spectator.query(Dialog);

            spectator.click(infoBtnElement);
            expect(dialogComponent.visible).toBeTruthy();
        });

        it('should show a dialog when click on the proper responsive btn', async () => {
            spectator.detectChanges();

            const infoBtnElement = spectator.query(byTestId('info-btn-responsive'));

            const dialogComponent = spectator.query(Dialog);

            spectator.click(infoBtnElement);
            expect(dialogComponent.visible).toBeTruthy();
        });

        it('should call downloadAsset when click on the proper btn', async () => {
            const downloadSpy = jest
                .spyOn(spectator.component, 'downloadAsset')
                .mockImplementation(jest.fn());

            const { inode } = NEW_FILE_MOCK.entity;

            const expectedUrl = `/contentAsset/raw-data/${inode}/asset?byInode=true&force_download=true`;

            spectator.detectChanges();

            const downloadBtnElement = spectator.query(byTestId('download-btn'));

            spectator.click(downloadBtnElement);
            expect(downloadSpy).toHaveBeenCalledWith(expectedUrl);
        });

        it('should handle a error in fetchResourceLinks', async () => {
            dotResourceLinksService.getFileResourceLinksByInode.mockReturnValue(
                throwError(() => 'error')
            );
            spectator.detectChanges();
        });

        it('should there are the proper resources links', async () => {
            spectator.detectChanges();

            const infoBtnElement = spectator.query(byTestId('info-btn'));

            spectator.click(infoBtnElement);
            spectator.detectChanges();

            const links = spectator.queryAll('.file-info__item');
            const copyBtns = spectator.queryAll(DotCopyButtonComponent);

            expect(links.length).toBe(4);
            expect(copyBtns.length).toBe(3);
        });
    });

    describe('contentlet with content preview file', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    previewFile: {
                        source: 'contentlet',
                        file: NEW_FILE_EDITABLE_MOCK.entity
                    }
                } as unknown
            });
            dotResourceLinksService = spectator.inject(DotResourceLinksService, true);
        });

        it('should be created', () => {
            spectator.detectChanges();
            expect(spectator.component).toBeTruthy();
        });
    });

    describe('Disabled State Management', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    previewFile: {
                        source: 'contentlet',
                        file: NEW_FILE_MOCK.entity
                    },
                    disabled: true
                } as unknown
            });
            dotResourceLinksService = spectator.inject(DotResourceLinksService, true);
            spectator.detectChanges();
        });

        it('should disable info button when disabled', () => {
            const infoBtnComponent = spectator.query(byTestId('info-btn'));
            const actualButton = infoBtnComponent.querySelector('button') as HTMLButtonElement;
            expect(infoBtnComponent).toBeTruthy();
            expect(actualButton.disabled).toBe(true);
        });

        it('should disable download button when disabled', () => {
            const downloadBtnComponent = spectator.query(byTestId('download-btn'));
            const actualButton = downloadBtnComponent.querySelector('button') as HTMLButtonElement;
            expect(downloadBtnComponent).toBeTruthy();
            expect(actualButton.disabled).toBe(true);
        });

        it('should disable responsive buttons when disabled', () => {
            const infoResponsiveBtnComponent = spectator.query(byTestId('info-btn-responsive'));
            const downloadResponsiveBtnComponent = spectator.query(
                byTestId('download-btn-responsive')
            );

            expect(infoResponsiveBtnComponent?.querySelector('button').disabled).toBe(true);
            expect(downloadResponsiveBtnComponent?.querySelector('button').disabled).toBe(true);
        });

        it('should prevent download action when disabled', () => {
            const downloadSpy = jest
                .spyOn(spectator.component, 'downloadAsset')
                .mockImplementation(jest.fn());

            const downloadBtnComponent = spectator.query(byTestId('download-btn'));
            const actualDownloadBtn = downloadBtnComponent.querySelector('button');

            // Since button is disabled, clicking should not trigger download
            spectator.click(actualDownloadBtn);

            expect(downloadSpy).not.toHaveBeenCalled();
        });

        it('should not trigger download when clicking disabled button', () => {
            const downloadSpy = jest
                .spyOn(spectator.component, 'downloadAsset')
                .mockImplementation(jest.fn());

            const downloadBtnComponent = spectator.query(byTestId('download-btn'));
            const actualDownloadBtn = downloadBtnComponent.querySelector('button');

            expect(actualDownloadBtn.disabled).toBe(true);

            spectator.click(actualDownloadBtn);

            expect(downloadSpy).not.toHaveBeenCalled();
        });

        it('should not open dialog when clicking disabled info button', () => {
            const infoBtnComponent = spectator.query(byTestId('info-btn'));
            const actualButton = infoBtnComponent.querySelector('button') as HTMLButtonElement;
            const dialogComponent = spectator.query(Dialog);

            // Verify button is actually disabled
            expect(actualButton.disabled).toBe(true);

            // Try to click the disabled button
            spectator.click(actualButton);

            // Dialog should not open when button is disabled
            expect(dialogComponent.visible).toBeFalsy();
        });

        it('should not open dialog when clicking disabled responsive info button', () => {
            const infoResponsiveBtnComponent = spectator.query(byTestId('info-btn-responsive'));
            const actualButton = infoResponsiveBtnComponent?.querySelector(
                'button'
            ) as HTMLButtonElement;
            const dialogComponent = spectator.query(Dialog);

            // Verify button is actually disabled
            expect(actualButton?.disabled).toBe(true);

            // Try to click the disabled button
            if (actualButton) {
                spectator.click(actualButton);
            }

            // Dialog should not open when button is disabled
            expect(dialogComponent.visible).toBeFalsy();
        });

        describe('with temp file', () => {
            let tempFileSpectator: Spectator<DotFileFieldPreviewComponent>;

            beforeEach(() => {
                // Create a fresh component instance with temp file
                tempFileSpectator = createComponent({
                    props: {
                        previewFile: {
                            source: 'temp',
                            file: TEMP_FILE_MOCK
                        },
                        disabled: true
                    } as unknown
                });
                tempFileSpectator.detectChanges();
            });

            it('should disable info button for temp files', () => {
                const infoBtnComponent = tempFileSpectator.query(byTestId('info-btn'));
                const actualButton = infoBtnComponent?.querySelector('button') as HTMLButtonElement;
                expect(actualButton?.disabled).toBe(true);
            });

            it('should disable responsive info button for temp files', () => {
                const infoResponsiveBtnComponent = tempFileSpectator.query(
                    byTestId('info-btn-responsive')
                );
                const actualButton = infoResponsiveBtnComponent?.querySelector(
                    'button'
                ) as HTMLButtonElement;
                expect(actualButton?.disabled).toBe(true);
            });
        });

        describe('responsive actions when disabled', () => {
            // Note: disabled state is already set in parent beforeEach

            it('should show responsive info button as disabled', () => {
                const infoBtnComponent = spectator.query(byTestId('info-btn-responsive'));
                const actualButton = infoBtnComponent?.querySelector('button') as HTMLButtonElement;
                expect(actualButton?.disabled).toBe(true);
            });

            it('should show responsive download button as disabled', () => {
                const downloadBtnComponent = spectator.query(byTestId('download-btn-responsive'));
                const actualButton = downloadBtnComponent?.querySelector(
                    'button'
                ) as HTMLButtonElement;
                expect(actualButton?.disabled).toBe(true);
            });
        });
    });
});
