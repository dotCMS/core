import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';

import { delay } from 'rxjs/operators';

import { DotResourceLinksService } from '@dotcms/data-access';

import { DotBinaryFieldPreviewComponent } from './dot-binary-field-preview.component';

import { BINARY_FIELD_CONTENTLET } from '../../../../utils/mocks';
import { TEMP_FILES_MOCK } from '../../utils/mock';

const CONTENTLET_MOCK = {
    ...BINARY_FIELD_CONTENTLET,
    baseType: 'FILEASSET',
    fieldVariable: 'Binary'
};

const CONTENTLET_HTMLPAGE_MOCK = {
    ...BINARY_FIELD_CONTENTLET,
    baseType: 'HTMLPAGE',
    fieldVariable: 'Binary'
};

const CONTENTLET_TEXT_MOCK = {
    ...BINARY_FIELD_CONTENTLET,
    BinaryMetaData: {
        ...BINARY_FIELD_CONTENTLET.binaryMetaData,
        editableAsText: true,
        contentType: 'text/plain'
    },
    fieldVariable: 'Binary',
    content: 'Data'
};

const clickOnInfoButton = (spectator: Spectator<DotBinaryFieldPreviewComponent>) => {
    const infoButton = spectator.query(byTestId('info-btn'));
    spectator.click(infoButton);
    spectator.detectChanges();
};

describe('DotBinaryFieldPreviewComponent', () => {
    let spectator: Spectator<DotBinaryFieldPreviewComponent>;
    let dotResourceLinksService: DotResourceLinksService;

    const createComponent = createComponentFactory({
        component: DotBinaryFieldPreviewComponent,
        imports: [HttpClientTestingModule],
        providers: [
            {
                provide: DotResourceLinksService,
                useValue: {
                    getFileResourceLinks: () => of({})
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: CONTENTLET_MOCK,
                fieldVariable: 'Binary',
                tempFile: null,
                editableImage: true
            },
            detectChanges: false
        });

        dotResourceLinksService = spectator.inject(DotResourceLinksService, true);
    });

    it('should show contentlet thumbnail', () => {
        spectator.detectChanges();
        const thumbnail = spectator.query(byTestId('contentlet-thumbnail')) as Element;
        expect(thumbnail).toBeTruthy();
        expect(thumbnail['fieldVariable']).toBe(CONTENTLET_MOCK.fieldVariable);
        expect(thumbnail['contentlet']).toEqual(CONTENTLET_MOCK);
    });

    it('should show temp file thumbnail', () => {
        spectator.setInput('tempFile', TEMP_FILES_MOCK[0]);
        spectator.setInput('contentlet', null);

        spectator.detectChanges();
        expect(spectator.query(byTestId('temp-file-thumbnail'))).toBeTruthy();
    });

    it('should emit removeFile event when remove button is clicked', () => {
        const spy = jest.spyOn(spectator.component.removeFile, 'emit');
        const removeButton = spectator.query(byTestId('remove-button'));
        spectator.click(removeButton);
        expect(spy).toHaveBeenCalled();
    });

    it('should show download button', () => {
        spectator.detectChanges();
        const downloadButton = spectator.query(byTestId('download-btn'));
        const spyWindowOpen = jest.spyOn(window, 'open').mockImplementation(() => null);

        expect(downloadButton).toBeTruthy();

        spectator.click(downloadButton);
        spectator.detectChanges();

        expect(spyWindowOpen).toHaveBeenCalledWith(
            `/contentAsset/raw-data/${CONTENTLET_MOCK.inode}/${CONTENTLET_MOCK.fieldVariable}?byInode=true&force_download=true`,
            '_self'
        );
    });

    it("should doesn't show download button", () => {
        spectator.setInput('tempFile', TEMP_FILES_MOCK[0]);
        spectator.setInput('contentlet', null);
        spectator.detectChanges();
        const downloadButton = spectator.query(byTestId('download-btn'));

        expect(downloadButton).toBeNull();
    });

    it('should be editable', () => {
        spectator.detectChanges();
        const editButton = spectator.query(byTestId('edit-button'));
        expect(editButton).toBeTruthy();
    });

    it('should show download button responsive', () => {
        spectator.detectChanges();
        const downloadButtonResponsive = spectator.query(byTestId('download-btn-responsive'));
        const spyWindowOpen = jest.spyOn(window, 'open').mockImplementation(() => null);

        expect(downloadButtonResponsive).toBeTruthy();

        spectator.click(downloadButtonResponsive);
        spectator.detectChanges();

        expect(spyWindowOpen).toHaveBeenCalledWith(
            `/contentAsset/raw-data/${CONTENTLET_MOCK.inode}/${CONTENTLET_MOCK.fieldVariable}?byInode=true&force_download=true`,
            '_self'
        );
    });

    describe('onEdit', () => {
        describe('when file is an image', () => {
            it('should emit editImage event', () => {
                spectator.detectChanges();
                const spy = jest.spyOn(spectator.component.editImage, 'emit');
                const editButton = spectator.query(byTestId('edit-button'));
                spectator.click(editButton);
                expect(spy).toHaveBeenCalled();
            });
        });

        describe('when contentelt is a text file', () => {
            beforeEach(() => {
                spectator.setInput('contentlet', CONTENTLET_TEXT_MOCK);
                spectator.detectChanges();
            });

            it('should emit editFile event when edit button is clicked', () => {
                const spy = jest.spyOn(spectator.component.editFile, 'emit');
                const editButton = spectator.query(byTestId('edit-button'));
                spectator.click(editButton);
                expect(spy).toHaveBeenCalled();
            });

            it('should emit editFile event click on the code preview', () => {
                const spy = jest.spyOn(spectator.component.editFile, 'emit');
                const codePreview = spectator.query(byTestId('code-preview'));
                spectator.click(codePreview);
                expect(spy).toHaveBeenCalled();
            });
        });
    });

    describe('editableImage', () => {
        describe('when is true', () => {
            it('should set isEditable to true', () => {
                spectator.detectChanges();
                const editButton = spectator.query(byTestId('edit-button'));
                expect(editButton).toBeTruthy();
            });
        });

        describe('when is false', () => {
            beforeEach(async () => {
                spectator.setInput('editableImage', false);
                spectator.detectChanges();
                await spectator.fixture.whenStable();
            });

            it('should set isEditable to false', () => {
                const editButton = spectator.query(byTestId('edit-button'));
                expect(editButton).not.toBeTruthy();
            });
        });
    });

    describe('responsive', () => {
        it('should emit removeFile event when remove button is clicked', () => {
            const spy = jest.spyOn(spectator.component.removeFile, 'emit');
            const removeButton = spectator.query(byTestId('remove-button-responsive'));
            spectator.click(removeButton);
            expect(spy).toHaveBeenCalled();
        });

        describe('onEdit', () => {
            describe('when file is an image', () => {
                it('should emit editImage event', () => {
                    spectator.detectChanges();
                    const spy = jest.spyOn(spectator.component.editImage, 'emit');
                    const editButton = spectator.query(byTestId('edit-button-responsive'));
                    spectator.click(editButton);
                    expect(spy).toHaveBeenCalled();
                });
            });

            describe('when the contentlet is a text file', () => {
                beforeEach(() => {
                    spectator.setInput('contentlet', CONTENTLET_TEXT_MOCK);
                    spectator.detectChanges();
                });

                it('should emit editFile event when edit button is clicked', () => {
                    const spy = jest.spyOn(spectator.component.editFile, 'emit');
                    const editButton = spectator.query(byTestId('edit-button-responsive'));
                    spectator.click(editButton);
                    expect(spy).toHaveBeenCalled();
                });
            });
        });
    });

    describe('Resource Links', () => {
        const RESOURCE_LINKS = {
            configuredImageURL: '/configuredImageURL',
            text: '/text',
            versionPath: '/versionPath',
            idPath: '/idPath',
            mimeType: 'image/png'
        };

        it('should have the correct resource links', () => {
            const spyResourceLinks = jest
                .spyOn(dotResourceLinksService, 'getFileResourceLinks')
                .mockReturnValue(of(RESOURCE_LINKS));

            spectator.detectChanges();

            clickOnInfoButton(spectator);

            const fileLinkElement = spectator.query(byTestId('resource-link-FileLink'));
            const resourceLinkElement = spectator.query(byTestId('resource-link-Resource-Link'));
            const versionPathElement = spectator.query(byTestId('resource-link-VersionPath'));
            const idPathElement = spectator.query(byTestId('resource-link-IdPath'));

            expect(fileLinkElement).not.toBeNull();
            expect(resourceLinkElement).not.toBeNull();
            expect(versionPathElement).not.toBeNull();
            expect(idPathElement).not.toBeNull();

            expect(spyResourceLinks).toHaveBeenCalledWith({
                fieldVariable: 'Binary',
                inodeOrIdentifier: CONTENTLET_MOCK.identifier
            });
        });

        it('should not have the Resource-Link', () => {
            const spyResourceLinks = jest
                .spyOn(dotResourceLinksService, 'getFileResourceLinks')
                .mockReturnValue(of(RESOURCE_LINKS));
            spectator.setInput('contentlet', CONTENTLET_HTMLPAGE_MOCK);

            spectator.detectChanges();

            clickOnInfoButton(spectator);

            const resourceLinkElement = spectator.query(byTestId('resource-link-Resource-Link'));

            expect(resourceLinkElement).toBeNull();
            expect(spyResourceLinks).toHaveBeenCalledWith({
                fieldVariable: 'Binary',
                inodeOrIdentifier: CONTENTLET_MOCK.identifier
            });
        });

        it('should have the loading state', fakeAsync(() => {
            const spyResourceLinks = jest
                .spyOn(dotResourceLinksService, 'getFileResourceLinks')
                .mockReturnValue(of(RESOURCE_LINKS).pipe(delay(1000)));

            spectator.detectChanges();

            clickOnInfoButton(spectator);

            const loadingElements = spectator.queryAll('.file-info__loading');

            expect(loadingElements.length).toBe(4);

            tick(1000);

            expect(spyResourceLinks).toHaveBeenCalledWith({
                fieldVariable: 'Binary',
                inodeOrIdentifier: CONTENTLET_MOCK.identifier
            });
        }));

        it('should not show file resolution', () => {
            spectator.setInput('contentlet', {
                ...CONTENTLET_MOCK,
                BinaryMetaData: {
                    ...BINARY_FIELD_CONTENTLET.binaryMetaData,
                    height: 0,
                    width: 0
                }
            });

            spectator.detectChanges();

            clickOnInfoButton(spectator);

            const resolution = spectator.query(byTestId('file-resolution'));
            expect(resolution).toBeNull();
        });
    });
});
