import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import {
    DotDropZoneComponent,
    DropZoneErrorType,
    DropZoneFileValidity
} from './dot-drop-zone.component';

const MOCK_VALIDITY: DropZoneFileValidity = {
    fileTypeMismatch: false,
    maxFileSizeExceeded: false,
    multipleFilesDropped: false,
    errorsType: [],
    valid: true
};

describe('DotDropZoneComponent', () => {
    let spectator: SpectatorHost<DotDropZoneComponent>;
    let mockFile: File;
    let mockDataTransfer: DataTransfer;

    const createHost = createHostFactory({
        component: DotDropZoneComponent,
        imports: [CommonModule]
    });

    beforeEach(async () => {
        spectator = createHost(`
            <dot-drop-zone>
                <div id="dot-drop-zone__content" class="dot-drop-zone__content">
                    Content
                </div>
            </dot-drop-zone>
        `);

        spectator.detectChanges();
    });

    beforeEach(() => {
        mockFile = new File([''], 'filename', { type: 'text/html' });
        mockDataTransfer = new DataTransfer();
        mockDataTransfer.items.add(mockFile);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should have content', () => {
        expect(spectator.query('#dot-drop-zone__content')).toBeTruthy();
    });

    describe('onDrop', () => {
        it('should emit fileDrop', () => {
            const spy = spyOn(spectator.component.fileDropped, 'emit');
            const event = new DragEvent('drop', {
                dataTransfer: mockDataTransfer
            });

            spectator.component.onDrop(event);

            expect(spy).toHaveBeenCalledWith({
                file: mockFile,
                validity: {
                    ...MOCK_VALIDITY
                }
            });
        });

        it('should prevent default', () => {
            const event = new DragEvent('drop', {
                dataTransfer: mockDataTransfer
            });
            const spyEventPrevent = spyOn(event, 'preventDefault');
            const spyEventStop = spyOn(event, 'stopPropagation');

            spectator.component.onDrop(event);

            expect(spyEventPrevent).toHaveBeenCalled();
            expect(spyEventStop).toHaveBeenCalled();
        });

        describe('when file is valid', () => {
            beforeEach(() => {
                spectator.component.accept = ['.html', 'text/html'];
                spectator.detectChanges();
            });

            it('should emit fileDrop', () => {
                const spy = spyOn(spectator.component.fileDropped, 'emit');
                const event = new DragEvent('drop', {
                    dataTransfer: mockDataTransfer
                });

                spectator.component.onDrop(event);

                expect(spy).toHaveBeenCalledWith({
                    file: mockFile,
                    validity: {
                        ...MOCK_VALIDITY
                    }
                });
            });
        });

        describe('when multiple files are being dragged', () => {
            it('should set multiFileError to true if multiplefiles are being dragged', () => {
                const spy = spyOn(spectator.component.fileDropped, 'emit');

                const file1 = new File([''], 'filename', { type: 'text/html' });
                const file2 = new File([''], 'filename', { type: 'text/html' });
                mockDataTransfer.items.add(file1);
                mockDataTransfer.items.add(file2);

                const event = new DragEvent('drop', {
                    dataTransfer: mockDataTransfer
                });

                // FF does not support clearData:
                // ERROR DOMException: Modifications are not allowed for this document on FireFox
                const spyClearData = spyOn(mockDataTransfer, 'clearData'); // It gets cleared automatically

                spectator.component.onDrop(event);

                expect(spy).toHaveBeenCalledWith({
                    file: null,
                    validity: {
                        ...MOCK_VALIDITY,
                        errorsType: [DropZoneErrorType.MULTIPLE_FILES_DROPPED],
                        multipleFilesDropped: true,
                        valid: false
                    }
                });
                expect(spyClearData).not.toHaveBeenCalled();
            });
        });

        describe('when file is invalid', () => {
            beforeEach(() => {
                spectator.component.accept = ['.png', 'image/'];
                spectator.component.maxFileSize = 10;
                spectator.detectChanges();
            });

            it('should emit fileDropped event with validity  to true', () => {
                const spy = spyOn(spectator.component.fileDropped, 'emit');
                const event = new DragEvent('drop', {
                    dataTransfer: mockDataTransfer
                });

                spectator.component.onDrop(event);
                expect(spy).toHaveBeenCalledWith({
                    file: mockFile,
                    validity: {
                        ...MOCK_VALIDITY,
                        errorsType: [DropZoneErrorType.FILE_TYPE_MISMATCH],
                        fileTypeMismatch: true,
                        valid: false
                    }
                });
            });

            it('should emit fileDropped event with validity maxFileSizeExceeded to true', () => {
                const file = new File([''], 'mockfile.png', { type: 'image/png' });
                Object.defineProperty(file, 'size', { value: 2000000 });
                const mockDataTransfer = new DataTransfer();
                mockDataTransfer.items.add(file);

                const spy = spyOn(spectator.component.fileDropped, 'emit');
                const event = new DragEvent('drop', {
                    dataTransfer: mockDataTransfer
                });

                spectator.component.onDrop(event);
                expect(spy).toHaveBeenCalledWith({
                    file: mockFile,
                    validity: {
                        ...MOCK_VALIDITY,
                        errorsType: [DropZoneErrorType.MAX_FILE_SIZE_EXCEEDED],
                        maxFileSizeExceeded: true,
                        valid: false
                    }
                });
            });
        });
    });

    describe('onDragEnter', () => {
        it('should emit fileDragEnter event', () => {
            const spy = spyOn(spectator.component.fileDragEnter, 'emit');
            const event = new DragEvent('dragenter');

            spectator.component.onDragEnter(event);
            spectator.detectChanges();

            expect(spy).toHaveBeenCalledWith(true);
        });

        it('should prevent default', () => {
            const event = new DragEvent('dragenter');
            const spyEventPrevent = spyOn(event, 'preventDefault');
            const spyEventStop = spyOn(event, 'stopPropagation');

            spectator.component.onDragEnter(event);

            expect(spyEventPrevent).toHaveBeenCalled();
            expect(spyEventStop).toHaveBeenCalled();
        });
    });

    describe('onDragOver', () => {
        it('should emit fileDragOver event', () => {
            const spy = spyOn(spectator.component.fileDragOver, 'emit');
            const event = new DragEvent('dragover');

            spectator.component.onDragOver(event);
            spectator.detectChanges();

            expect(spy).toHaveBeenCalledWith(true);
        });

        it('should prevent default', () => {
            const event = new DragEvent('dragover');
            const spyEventPrevent = spyOn(event, 'preventDefault');
            const spyEventStop = spyOn(event, 'stopPropagation');

            spectator.component.onDragOver(event);

            expect(spyEventPrevent).toHaveBeenCalled();
            expect(spyEventStop).toHaveBeenCalled();
        });
    });

    describe('onDragLeave', () => {
        it('should emit fileDragLeave event', () => {
            const spy = spyOn(spectator.component.fileDragLeave, 'emit');
            const event = new DragEvent('dragleave');

            spectator.component.onDragLeave(event);
            spectator.detectChanges();

            expect(spy).toHaveBeenCalledWith(true);
        });

        it('should prevent default', () => {
            const event = new DragEvent('dragleave');
            const spyEventPrevent = spyOn(event, 'preventDefault');
            const spyEventStop = spyOn(event, 'stopPropagation');

            spectator.component.onDragLeave(event);

            expect(spyEventPrevent).toHaveBeenCalled();
            expect(spyEventStop).toHaveBeenCalled();
        });
    });
});
