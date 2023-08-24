import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import { DotDropZoneComponent, DropZoneError } from './dot-drop-zone.component';

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
        it('should emit fileDrop and set active to false', () => {
            const spy = spyOn(spectator.component.fileDrop, 'emit');
            spectator.component.active = true;

            const event = new DragEvent('drop', {
                dataTransfer: mockDataTransfer
            });

            spectator.component.onDrop(event);

            expect(spy).toHaveBeenCalledWith(mockFile);
            expect(spectator.component.active).toBeFalsy();
        });

        describe('when file is valid', () => {
            beforeEach(() => {
                spectator.component.accept = ['.html', 'text/html'];
                spectator.detectChanges();
            });

            it('should emit fileDrop and set active to false', () => {
                const spy = spyOn(spectator.component.fileDrop, 'emit');
                spectator.component.active = true;

                const event = new DragEvent('drop', {
                    dataTransfer: mockDataTransfer
                });

                spectator.component.onDrop(event);

                expect(spy).toHaveBeenCalledWith(mockFile);
                expect(spectator.component.active).toBeFalsy();
            });
        });

        describe('when file is invalid', () => {
            beforeEach(() => {
                spectator.component.accept = ['.png', 'image/'];
                spectator.detectChanges();
            });

            it('should set invalidFile to true if file is not valid', () => {
                const spy = spyOn(spectator.component.fileDrop, 'emit');
                const spyError = spyOn(spectator.component.dropZoneError, 'emit');

                const event = new DragEvent('drop', {
                    dataTransfer: mockDataTransfer
                });

                spectator.component.onDrop(event);
                expect(spectator.component.error).toBeTrue();
                expect(spyError).toHaveBeenCalledWith(DropZoneError.INVALID_FILE);
                expect(spy).not.toHaveBeenCalled();
            });
        });
    });

    describe('onDragEnter', () => {
        it('should set active to true and add drop-zone-active class', () => {
            const spy = spyOn(spectator.component.dragStart, 'emit');
            const event = new DragEvent('dragenter', {
                dataTransfer: mockDataTransfer
            });

            spectator.component.onDragEnter(event);

            spectator.detectChanges();

            expect(spectator.component.active).toBeTrue();
            expect(spectator.element.classList).toContain('drop-zone-active');
            expect(spy).toHaveBeenCalledWith(true);
        });

        describe('when multiple files are being dragged', () => {
            it('should set multiFileError to true if multiplefiles are being dragged', () => {
                const spyError = spyOn(spectator.component.dropZoneError, 'emit');
                const file1 = new File([''], 'filename', { type: 'text/html' });
                const file2 = new File([''], 'filename', { type: 'text/html' });
                mockDataTransfer.items.add(file1);
                mockDataTransfer.items.add(file2);

                const event = new DragEvent('dragenter', {
                    dataTransfer: mockDataTransfer
                });

                spectator.component.onDragEnter(event);
                expect(spyError).toHaveBeenCalledWith(DropZoneError.MULTIFILE_ERROR);
                expect(spectator.component.error).toBeTrue();
                expect(spectator.component.active).toBeFalsy();
            });
        });
    });

    describe('onDragLeave', () => {
        it('should set active & error to false', () => {
            const spy = spyOn(spectator.component.dragStop, 'emit');
            spectator.component.active = true;
            spectator.component.error = true;

            const event = new DragEvent('dragleave');

            spectator.component.onDragLeave(event);

            spectator.detectChanges();

            expect(spectator.component.active).toBeFalse();
            expect(spectator.component.error).toBeFalse();
            expect(spy).toHaveBeenCalledWith(true);
        });
    });
});
