import {
    Spectator,
    createComponentFactory,
    mockProvider,
    SpyObject,
    byTestId
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

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

        it('should be have a dot-contentlet-thumbnail', () => {
            spectator.detectChanges();
            expect(spectator.query('dot-contentlet-thumbnail')).toBeTruthy();
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
            const spyWindowOpen = jest.spyOn(window, 'open');
            spyWindowOpen.mockImplementation(jest.fn());

            const { inode } = NEW_FILE_MOCK.entity;

            const expectedUrl = `/contentAsset/raw-data/${inode}/asset?byInode=true&force_download=true`;

            spectator.detectChanges();

            const downloadBtnElement = spectator.query(byTestId('download-btn'));

            spectator.click(downloadBtnElement);
            expect(spyWindowOpen).toHaveBeenCalledWith(expectedUrl, '_self');
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
            // Clean up any existing spies and create a fresh one
            jest.restoreAllMocks();
            const spyWindowOpen = jest.spyOn(window, 'open').mockImplementation(() => null);

            // Try to trigger download through the component method indirectly
            const downloadBtnComponent = spectator.query(byTestId('download-btn'));
            const actualDownloadBtn = downloadBtnComponent.querySelector('button');

            // Since button is disabled, clicking should not trigger download
            spectator.click(actualDownloadBtn);

            expect(spyWindowOpen).not.toHaveBeenCalled();
            spyWindowOpen.mockRestore();
        });

        it('should not trigger download when clicking disabled button', () => {
            const spyWindowOpen = jest.spyOn(window, 'open').mockImplementation(() => null);

            // Get the actual button element inside the PrimeNG component
            const downloadBtnComponent = spectator.query(byTestId('download-btn'));
            const actualDownloadBtn = downloadBtnComponent.querySelector('button');

            // Verify button is actually disabled
            expect(actualDownloadBtn.disabled).toBe(true);

            // Try to click the disabled button - clicks on disabled buttons should not trigger handlers
            spectator.click(actualDownloadBtn);

            // Since button is disabled, download should not be triggered
            expect(spyWindowOpen).not.toHaveBeenCalled();

            spyWindowOpen.mockRestore();
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
