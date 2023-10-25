import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { DotBinaryFieldPreviewComponent } from './dot-binary-field-preview.component';

import { BinaryFile } from '../../interfaces';

describe('DotBinaryFieldPreviewComponent', () => {
    let spectator: Spectator<DotBinaryFieldPreviewComponent>;
    const createComponent = createComponentFactory(DotBinaryFieldPreviewComponent);

    const binaryFile: BinaryFile = {
        mimeType: 'image/png',
        name: 'test.png',
        fileSize: 1234,
        content: 'data:image/png;base64,iVBORw0KGg...',
        url: 'http://example.com/test.png',
        inode: '123',
        titleImage: 'Test Image',
        width: '100',
        height: '100'
    };

    beforeEach(() => {
        spectator = createComponent({
            props: {
                file: binaryFile,
                variableName: 'test'
            }
        });
    });

    it('should set isEditable to true for image files', () => {
        expect(spectator.component.isEditable).toBe(true);
    });

    it('should emit editFile event when onEdit is called', () => {
        const spy = jest.spyOn(spectator.component.editFile, 'emit');
        spectator.component.onEdit();
        expect(spy).toHaveBeenCalledWith({ content: binaryFile.content });
    });

    it('should emit editFile event when edit button is clicked', () => {
        const spy = jest.spyOn(spectator.component.editFile, 'emit');
        const editButton = spectator.query(byTestId('edit-button'));
        spectator.click(editButton);
        expect(spy).toHaveBeenCalled();
    });

    it('should emit removeFile event when remove button is clicked', () => {
        const spy = jest.spyOn(spectator.component.removeFile, 'emit');
        const removeButton = spectator.query(byTestId('remove-button'));
        spectator.click(removeButton);
        expect(spy).toHaveBeenCalled();
    });
});
