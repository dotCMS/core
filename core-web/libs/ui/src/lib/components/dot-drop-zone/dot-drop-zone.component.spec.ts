import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import { DotDropZoneComponent } from './dot-drop-zone.component';

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
                    isFileTypeMismatch: false,
                    isFileTooBig: false,
                    multipleFiles: false,
                    valid: true
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
                        isFileTypeMismatch: false,
                        isFileTooBig: false,
                        multipleFiles: false,
                        valid: true
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

                spectator.component.onDrop(event);

                expect(spy).toHaveBeenCalledWith({
                    file: null,
                    validity: {
                        isFileTypeMismatch: false,
                        isFileTooBig: false,
                        multipleFiles: true,
                        valid: false
                    }
                });
            });
        });

        describe('when file is invalid', () => {
            beforeEach(() => {
                spectator.component.accept = ['.png', 'image/'];
                spectator.detectChanges();
            });

            it('should emit fileDropped event with validity isFileTypeMismatch to true', () => {
                const spy = spyOn(spectator.component.fileDropped, 'emit');
                const event = new DragEvent('drop', {
                    dataTransfer: mockDataTransfer
                });

                spectator.component.onDrop(event);
                expect(spy).toHaveBeenCalledWith({
                    file: mockFile,
                    validity: {
                        isFileTypeMismatch: true,
                        isFileTooBig: false,
                        multipleFiles: false,
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
