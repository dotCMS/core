import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotBinaryFieldPreviewComponent } from './dot-binary-field-preview.component';

import { DotFilePreview } from '../../interfaces';
import { fileMetaData } from '../../utils/mock';

const fileImage: DotFilePreview = {
    ...fileMetaData,
    id: '123',
    inode: '123',
    titleImage: 'Assets',
    contentType: 'image/png',
    name: 'test.png',
    content: 'data:image/png;base64,iVBORw0KGg...'
};

const fileText: DotFilePreview = {
    ...fileMetaData,
    id: '123',
    inode: '123',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    contentType: 'text/plain',
    name: 'test.txt',
    content: 'This is a text file'
};

const fileUnknown: DotFilePreview = {
    ...fileMetaData,
    id: '123',
    inode: '123',
    contentType: 'unknown',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    name: 'test.txt',
    content: 'This is a text file but the mime type is unknown'
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
                file: fileImage,
                editableImage: true
            }
        });
    });

    it('should set isEditable to true for image files', () => {
        expect(spectator.component.isEditable).toBe(true);
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

        describe('when file is a text file', () => {
            beforeEach(() => {
                spectator.setInput('file', fileText);
                spectator.detectChanges();
            });

            it('should emit editFile event when edit button is clicked', () => {
                const spy = jest.spyOn(spectator.component.editFile, 'emit');
                const editButton = spectator.query(byTestId('edit-button'));
                spectator.click(editButton);
                expect(spy).toHaveBeenCalled();
            });
        });

        describe('when file is a unknown file', () => {
            beforeEach(() => {
                spectator.setInput('file', fileUnknown);
                spectator.detectChanges();
            });

            it('should emit editFile event when edit button is clicked', () => {
                const spy = jest.spyOn(spectator.component.editFile, 'emit');
                const editButton = spectator.query(byTestId('edit-button'));
                spectator.click(editButton);
                expect(spy).toHaveBeenCalled();
            });

            it('should have edit button if it has content', () => {
                const editButton = spectator.query(byTestId('edit-button'));
                expect(editButton).toBeTruthy();
            });

            it('should not have edit button if it does not have content', () => {
                spectator.setInput('file', {
                    ...fileUnknown,
                    content: ''
                });
                spectator.detectChanges();
                const editButton = spectator.query(byTestId('edit-button'));
                expect(editButton).not.toBeTruthy();
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
            describe('when file is an image', () => {
                it('should emit editImage event', () => {
                    const spy = jest.spyOn(spectator.component.editImage, 'emit');
                    const editButton = spectator.query(byTestId('edit-button-responsive'));
                    spectator.click(editButton);
                    expect(spy).toHaveBeenCalled();
                });
            });

            describe('when file is a text file', () => {
                beforeEach(() => {
                    spectator.setInput('file', fileText);
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
