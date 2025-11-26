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
                .spyOn(dotResourceLinksService, 'getFileResourceLinksByInode')
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
                inode: CONTENTLET_MOCK.inode
            });
        });

        it('should not have the Resource-Link', () => {
            const spyResourceLinks = jest
                .spyOn(dotResourceLinksService, 'getFileResourceLinksByInode')
                .mockReturnValue(of(RESOURCE_LINKS));
            spectator.setInput('contentlet', CONTENTLET_HTMLPAGE_MOCK);

            spectator.detectChanges();

            clickOnInfoButton(spectator);

            const resourceLinkElement = spectator.query(byTestId('resource-link-Resource-Link'));

            expect(resourceLinkElement).toBeNull();
            expect(spyResourceLinks).toHaveBeenCalledWith({
                fieldVariable: 'Binary',
                inode: CONTENTLET_MOCK.inode
            });
        });

        it('should have the loading state', fakeAsync(() => {
            const spyResourceLinks = jest
                .spyOn(dotResourceLinksService, 'getFileResourceLinksByInode')
                .mockReturnValue(of(RESOURCE_LINKS).pipe(delay(1000)));

            spectator.detectChanges();

            clickOnInfoButton(spectator);

            const loadingElements = spectator.queryAll('.file-info__loading');

            expect(loadingElements.length).toBe(4);

            tick(1000);

            expect(spyResourceLinks).toHaveBeenCalledWith({
                fieldVariable: 'Binary',
                inode: CONTENTLET_MOCK.inode
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

    describe('Disabled State Management', () => {
        beforeEach(() => {
            spectator.setInput('disabled', true);
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

        it('should disable remove button when disabled', () => {
            const removeBtnComponent = spectator.query(byTestId('remove-button'));
            const actualButton = removeBtnComponent.querySelector('button') as HTMLButtonElement;
            expect(removeBtnComponent).toBeTruthy();
            expect(actualButton.disabled).toBe(true);
        });

        it('should disable edit button when disabled', () => {
            const editBtnComponent = spectator.query(byTestId('edit-button'));
            const actualButton = editBtnComponent.querySelector('button') as HTMLButtonElement;
            expect(editBtnComponent).toBeTruthy();
            expect(actualButton.disabled).toBe(true);
        });

        it('should disable responsive buttons when disabled', () => {
            const infoResponsiveBtnComponent = spectator.query(byTestId('infor-button-responsive'));
            const downloadResponsiveBtnComponent = spectator.query(
                byTestId('download-btn-responsive')
            );
            const removeResponsiveBtnComponent = spectator.query(
                byTestId('remove-button-responsive')
            );
            const editResponsiveBtnComponent = spectator.query(byTestId('edit-button-responsive'));

            expect(infoResponsiveBtnComponent.querySelector('button').disabled).toBe(true);
            expect(downloadResponsiveBtnComponent.querySelector('button').disabled).toBe(true);
            expect(removeResponsiveBtnComponent.querySelector('button').disabled).toBe(true);
            expect(editResponsiveBtnComponent?.querySelector('button').disabled).toBe(true);
        });

        it('should prevent download action when disabled', () => {
            // Clean up any existing spies and create a fresh one
            jest.restoreAllMocks();
            const spyWindowOpen = jest.spyOn(window, 'open').mockImplementation(() => null);

            spectator.component.downloadAsset();

            expect(spyWindowOpen).not.toHaveBeenCalled();
            spyWindowOpen.mockRestore();
        });

        it('should prevent edit action when disabled', () => {
            const editImageSpy = jest.spyOn(spectator.component.editImage, 'emit');
            const editFileSpy = jest.spyOn(spectator.component.editFile, 'emit');

            spectator.component.onEdit();

            expect(editImageSpy).not.toHaveBeenCalled();
            expect(editFileSpy).not.toHaveBeenCalled();
        });

        it('should not trigger actions when clicking disabled buttons', () => {
            const editImageSpy = jest.spyOn(spectator.component.editImage, 'emit');
            const removeFileSpy = jest.spyOn(spectator.component.removeFile, 'emit');

            // Get the actual button elements inside the PrimeNG components
            const editBtnComponent = spectator.query(byTestId('edit-button'));
            const removeBtnComponent = spectator.query(byTestId('remove-button'));

            const actualEditBtn = editBtnComponent.querySelector('button');
            const actualRemoveBtn = removeBtnComponent.querySelector('button');

            // Verify buttons are actually disabled
            expect(actualEditBtn.disabled).toBe(true);
            expect(actualRemoveBtn.disabled).toBe(true);

            // Try to click the disabled buttons - clicks on disabled buttons should not trigger handlers
            spectator.click(actualEditBtn);
            spectator.click(actualRemoveBtn);

            // Since buttons are disabled, events should not be emitted
            expect(editImageSpy).not.toHaveBeenCalled();
            expect(removeFileSpy).not.toHaveBeenCalled();
        });

        it('should show remove button as disabled when component is disabled', () => {
            const removeBtnComponent = spectator.query(byTestId('remove-button'));
            const actualButton = removeBtnComponent.querySelector('button') as HTMLButtonElement;
            expect(actualButton.disabled).toBe(true);
        });

        it('should show edit button as disabled when component is disabled', () => {
            const editBtnComponent = spectator.query(byTestId('edit-button'));
            const actualButton = editBtnComponent.querySelector('button') as HTMLButtonElement;
            expect(actualButton.disabled).toBe(true);
        });

        it('should show download button as disabled when component is disabled', () => {
            const downloadBtnComponent = spectator.query(byTestId('download-btn'));
            const actualButton = downloadBtnComponent.querySelector('button') as HTMLButtonElement;
            expect(actualButton.disabled).toBe(true);
        });

        describe('with text file', () => {
            let textFileSpectator: Spectator<DotBinaryFieldPreviewComponent>;

            beforeEach(() => {
                // Create a fresh component instance with text file contentlet
                textFileSpectator = createComponent({
                    props: {
                        contentlet: CONTENTLET_TEXT_MOCK,
                        fieldVariable: 'Binary',
                        tempFile: null,
                        editableImage: true,
                        disabled: true
                    },
                    detectChanges: true
                });
            });

            it('should show edit button as disabled for text files', () => {
                const editBtnComponent = textFileSpectator.query(byTestId('edit-button'));
                const actualButton = editBtnComponent.querySelector('button') as HTMLButtonElement;
                expect(actualButton.disabled).toBe(true);
            });

            it('should show code preview for text files when disabled', () => {
                // Code preview should still be visible but interactions would be handled by disabled state
                const codePreview = textFileSpectator.query(byTestId('code-preview'));
                expect(codePreview).toBeTruthy();
                expect(codePreview.textContent.trim()).toContain('Data');
            });
        });

        describe('responsive actions when disabled', () => {
            // Note: disabled state is already set in parent beforeEach

            it('should show responsive remove button as disabled', () => {
                const removeBtnComponent = spectator.query(byTestId('remove-button-responsive'));
                const actualButton = removeBtnComponent.querySelector(
                    'button'
                ) as HTMLButtonElement;
                expect(actualButton.disabled).toBe(true);
            });

            it('should show responsive edit button as disabled', () => {
                const editBtnComponent = spectator.query(byTestId('edit-button-responsive'));
                const actualButton = editBtnComponent?.querySelector('button') as HTMLButtonElement;
                expect(actualButton?.disabled).toBe(true);
            });

            it('should show responsive download button as disabled', () => {
                const downloadBtnComponent = spectator.query(byTestId('download-btn-responsive'));
                const actualButton = downloadBtnComponent.querySelector(
                    'button'
                ) as HTMLButtonElement;
                expect(actualButton.disabled).toBe(true);
            });

            it('should show responsive info button as disabled', () => {
                const infoBtnComponent = spectator.query(byTestId('infor-button-responsive'));
                const actualButton = infoBtnComponent.querySelector('button') as HTMLButtonElement;
                expect(actualButton.disabled).toBe(true);
            });
        });
    });
});
