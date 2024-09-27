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
                getFileResourceLinks: jest.fn().mockReturnValue(
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
            dotResourceLinksService.getFileResourceLinks.mockReturnValue(throwError('error'));
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
});
