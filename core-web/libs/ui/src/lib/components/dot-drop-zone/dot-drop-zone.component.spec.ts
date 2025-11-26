/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/ban-ts-comment */

import { SpectatorHost, createHostFactory } from '@ngneat/spectator/jest';

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

const createMockDataTransfer = (files: File[]) => {
    const items = files.map((file) => ({
        kind: 'file',
        type: file.type,
        getAsFile: () => file
    }));

    return {
        items,
        files,
        clearData: jest.fn(),
        getData: jest.fn(),
        setData: jest.fn(),
        setDragImage: jest.fn()
    };
};

describe('DotDropZoneComponent', () => {
    let spectator: SpectatorHost<DotDropZoneComponent>;
    let mockFile: File;

    const createHost = createHostFactory({
        component: DotDropZoneComponent,
        imports: [CommonModule]
    });

    beforeEach(async () => {
        spectator = createHost(
            `<dot-drop-zone [disabled]="disabled">
                <div id="dot-drop-zone__content" class="dot-drop-zone__content">
                    Content
                </div>
            </dot-drop-zone>`,
            {
                hostProps: {
                    disabled: false
                }
            }
        );

        spectator.detectChanges();
    });

    beforeEach(() => {
        mockFile = new File([''], 'filename', { type: 'text/html' });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should have content', () => {
        expect(spectator.query('#dot-drop-zone__content')).toBeTruthy();
    });

    describe('onDrop', () => {
        it('should emit fileDropped event', () => {
            const spy = jest.spyOn(spectator.component.fileDropped, 'emit');
            const dataTransfer = createMockDataTransfer([mockFile]);

            const dropEvent = {
                preventDefault: jest.fn(),
                stopPropagation: jest.fn(),
                dataTransfer
            };

            spectator.component.onDrop(dropEvent as any);

            expect(spy).toHaveBeenCalledWith({
                file: mockFile,
                validity: {
                    ...MOCK_VALIDITY
                }
            });
        });

        it('should prevent default and stop propagation', () => {
            const dataTransfer = createMockDataTransfer([mockFile]);

            const dropEvent = {
                preventDefault: jest.fn(),
                stopPropagation: jest.fn(),
                dataTransfer
            };

            spectator.component.onDrop(dropEvent as any);

            expect(dropEvent.preventDefault).toHaveBeenCalled();
            expect(dropEvent.stopPropagation).toHaveBeenCalled();
        });

        describe('when file is valid', () => {
            beforeEach(() => {
                spectator.component.accept = ['.html', 'text/html'];
                spectator.detectChanges();
            });

            it('should emit fileDropped event', () => {
                const spy = jest.spyOn(spectator.component.fileDropped, 'emit');
                const dataTransfer = createMockDataTransfer([mockFile]);

                const dropEvent = {
                    preventDefault: jest.fn(),
                    stopPropagation: jest.fn(),
                    dataTransfer
                };

                spectator.component.onDrop(dropEvent as any);

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
                const spy = jest.spyOn(spectator.component.fileDropped, 'emit');
                const file1 = new File([''], 'filename1', { type: 'text/html' });
                const file2 = new File([''], 'filename2', { type: 'text/html' });
                const dataTransfer = createMockDataTransfer([file1, file2]);

                const dropEvent = {
                    preventDefault: jest.fn(),
                    stopPropagation: jest.fn(),
                    dataTransfer
                };

                spectator.component.onDrop(dropEvent as any);

                expect(spy).toHaveBeenCalledWith({
                    file: null,
                    validity: {
                        ...MOCK_VALIDITY,
                        errorsType: [DropZoneErrorType.MULTIPLE_FILES_DROPPED],
                        multipleFilesDropped: true,
                        valid: false
                    }
                });
            });
        });

        describe('when file is invalid', () => {
            beforeEach(() => {
                spectator.component.accept = ['.png', 'image/'];
                spectator.component.maxFileSize = 10;
                spectator.detectChanges();
            });

            it('should emit fileDropped event with validity fileTypeMismatch to true', () => {
                const spy = jest.spyOn(spectator.component.fileDropped, 'emit');
                const dataTransfer = createMockDataTransfer([mockFile]);

                const dropEvent = {
                    preventDefault: jest.fn(),
                    stopPropagation: jest.fn(),
                    dataTransfer
                };

                spectator.component.onDrop(dropEvent as any);

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
                const dataTransfer = createMockDataTransfer([file]);
                const spy = jest.spyOn(spectator.component.fileDropped, 'emit');

                const dropEvent = {
                    preventDefault: jest.fn(),
                    stopPropagation: jest.fn(),
                    dataTransfer
                };

                spectator.component.onDrop(dropEvent as any);

                expect(spy).toHaveBeenCalledWith({
                    file,
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
            const spy = jest.spyOn(spectator.component.fileDragEnter, 'emit');
            const event = new DragEvent('dragenter');

            spectator.component.onDragEnter(event);
            spectator.detectChanges();

            expect(spy).toHaveBeenCalledWith(true);
        });

        it('should prevent default', () => {
            const event = new DragEvent('dragenter');
            const spyEventPrevent = jest.spyOn(event, 'preventDefault');
            const spyEventStop = jest.spyOn(event, 'stopPropagation');

            spectator.component.onDragEnter(event);

            expect(spyEventPrevent).toHaveBeenCalled();
            expect(spyEventStop).toHaveBeenCalled();
        });
    });

    describe('onDragOver', () => {
        it('should emit fileDragOver event', () => {
            const spy = jest.spyOn(spectator.component.fileDragOver, 'emit');
            const event = new DragEvent('dragover');

            spectator.component.onDragOver(event);
            spectator.detectChanges();

            expect(spy).toHaveBeenCalledWith(true);
        });

        it('should prevent default', () => {
            const event = new DragEvent('dragover');
            const spyEventPrevent = jest.spyOn(event, 'preventDefault');
            const spyEventStop = jest.spyOn(event, 'stopPropagation');

            spectator.component.onDragOver(event);

            expect(spyEventPrevent).toHaveBeenCalled();
            expect(spyEventStop).toHaveBeenCalled();
        });
    });

    describe('onDragLeave', () => {
        it('should emit fileDragLeave event', () => {
            const spy = jest.spyOn(spectator.component.fileDragLeave, 'emit');
            const event = new DragEvent('dragleave');

            spectator.component.onDragLeave(event);
            spectator.detectChanges();

            expect(spy).toHaveBeenCalledWith(true);
        });

        it('should prevent default', () => {
            const event = new DragEvent('dragleave');
            const spyEventPrevent = jest.spyOn(event, 'preventDefault');
            const spyEventStop = jest.spyOn(event, 'stopPropagation');

            spectator.component.onDragLeave(event);

            expect(spyEventPrevent).toHaveBeenCalled();
            expect(spyEventStop).toHaveBeenCalled();
        });
    });

    describe('when disabled', () => {
        beforeEach(() => {
            spectator.setHostInput('disabled', true);
            spectator.detectChanges();
        });

        it('should add disabled class to host element', () => {
            expect(spectator.element).toHaveClass('disabled');
        });

        it('should not emit events when onDrop is called while disabled', () => {
            const spy = jest.spyOn(spectator.component.fileDropped, 'emit');
            const dataTransfer = createMockDataTransfer([mockFile]);

            const dropEvent = {
                preventDefault: jest.fn(),
                stopPropagation: jest.fn(),
                dataTransfer
            };

            spectator.component.onDrop(dropEvent as any);

            expect(spy).not.toHaveBeenCalled();
        });

        it('should not emit events when onDragEnter is called while disabled', () => {
            const spy = jest.spyOn(spectator.component.fileDragEnter, 'emit');
            const event = new DragEvent('dragenter');

            spectator.component.onDragEnter(event);

            expect(spy).not.toHaveBeenCalled();
        });

        it('should not emit events when onDragOver is called while disabled', () => {
            const spy = jest.spyOn(spectator.component.fileDragOver, 'emit');
            const event = new DragEvent('dragover');

            spectator.component.onDragOver(event);

            expect(spy).not.toHaveBeenCalled();
        });

        it('should not emit events when onDragLeave is called while disabled', () => {
            const spy = jest.spyOn(spectator.component.fileDragLeave, 'emit');
            const event = new DragEvent('dragleave');

            spectator.component.onDragLeave(event);

            expect(spy).not.toHaveBeenCalled();
        });

        it('should return early from onDrop without processing files when disabled', () => {
            const preventDefaultSpy = jest.fn();
            const stopPropagationSpy = jest.fn();
            const dataTransfer = createMockDataTransfer([mockFile]);

            const dropEvent = {
                preventDefault: preventDefaultSpy,
                stopPropagation: stopPropagationSpy,
                dataTransfer
            };

            spectator.component.onDrop(dropEvent as any);

            // Event methods should not be called since function returns early
            expect(preventDefaultSpy).not.toHaveBeenCalled();
            expect(stopPropagationSpy).not.toHaveBeenCalled();
        });

        it('should return early from drag events without calling event methods when disabled', () => {
            const preventDefaultSpy = jest.fn();
            const stopPropagationSpy = jest.fn();

            const dragEvent = {
                preventDefault: preventDefaultSpy,
                stopPropagation: stopPropagationSpy
            };

            spectator.component.onDragEnter(dragEvent as any);
            spectator.component.onDragOver(dragEvent as any);
            spectator.component.onDragLeave(dragEvent as any);

            // Event methods should not be called since functions return early
            expect(preventDefaultSpy).not.toHaveBeenCalled();
            expect(stopPropagationSpy).not.toHaveBeenCalled();
        });
    });
});
