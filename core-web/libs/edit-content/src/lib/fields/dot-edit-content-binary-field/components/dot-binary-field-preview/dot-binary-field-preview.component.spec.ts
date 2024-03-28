import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotBinaryFieldPreviewComponent } from './dot-binary-field-preview.component';

import { BINARY_FIELD_CONTENTLET } from '../../../../utils/mocks';
import { TEMP_FILES_MOCK } from '../../utils/mock';

const CONTENTLET_MOCK = {
    ...BINARY_FIELD_CONTENTLET,
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

describe('DotBinaryFieldPreviewComponent', () => {
    let spectator: Spectator<DotBinaryFieldPreviewComponent>;
    const createComponent = createComponentFactory({
        component: DotBinaryFieldPreviewComponent,
        imports: [HttpClientTestingModule]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: CONTENTLET_MOCK,
                tempFile: null,
                editableImage: true
            }
        });
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

    describe('onEdit', () => {
        describe('when file is an image', () => {
            it('should emit editImage event', () => {
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
            it('should set isEditable to true for image files', () => {
                expect(spectator.component.isEditable).toBe(true);
            });

            describe('when file is an image', () => {
                it('should emit editImage event', () => {
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
});
