import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotContentDriveDropzoneComponent } from './dot-content-drive-dropzone.component';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

// Mock DragEvent since it's not available in Jest environment
class DragEventMock extends Event {
    override preventDefault = jest.fn();
    override stopPropagation = jest.fn();
    dataTransfer: { files?: FileList | null } | null = null;

    constructor(type: string) {
        super(type);
        this.dataTransfer = { files: null };
    }
}

// Override global DragEvent with our mock
(global as unknown as { DragEvent: typeof DragEventMock }).DragEvent = DragEventMock;

// Helper functions to create properly mocked drag events
function createDragEnterEvent(
    fromElement?: HTMLElement | null
): DragEvent & { fromElement: HTMLElement | null } {
    const event = new DragEvent('dragenter') as DragEvent & { fromElement: HTMLElement | null };
    (event as unknown as { fromElement: HTMLElement | null }).fromElement = fromElement ?? null;
    return event;
}

function createDragLeaveEvent(relatedTarget?: EventTarget | null): DragEvent {
    const event = new DragEvent('dragleave');
    (event as unknown as { relatedTarget: EventTarget | null }).relatedTarget =
        relatedTarget ?? null;
    return event;
}

function createDropEvent(files?: FileList | null): DragEvent {
    const event = new DragEvent('drop');
    if (event.dataTransfer) {
        (event.dataTransfer as unknown as { files: FileList | null }).files = files ?? null;
    }
    return event;
}

describe('DotContentDriveDropzoneComponent', () => {
    let spectator: Spectator<DotContentDriveDropzoneComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let elementRefSpy: ReturnType<typeof jest.spyOn>;
    let uploadFilesSpyEmitter: ReturnType<typeof jest.spyOn>;
    const createComponent = createComponentFactory({
        component: DotContentDriveDropzoneComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                resetContextMenu: jest.fn(),
                selectedNode: jest.fn().mockReturnValue({ data: { id: 'test-id' } })
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockReturnValue('Drag and drop files here')
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        uploadFilesSpyEmitter = jest.spyOn(spectator.component.uploadFiles, 'emit');
        // Spy on the component's elementRef nativeElement.contains method
        if (spectator.component.elementRef?.nativeElement) {
            elementRefSpy = jest
                .spyOn(spectator.component.elementRef.nativeElement, 'contains')
                .mockReturnValue(false);
        }

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Component Creation', () => {
        it('should initialize with inactive state', () => {
            expect(spectator.component.active).toBe(false);
        });

        it('should render the message content', () => {
            const messageElement = spectator.query('[data-testid="message-content"]');
            expect(messageElement).toBeTruthy();
        });

        it('should render the upload icon', () => {
            const iconElement = spectator.query('[data-testid="message-content-icon"]');
            expect(iconElement).toBeTruthy();
        });

        it('should render the message text', () => {
            const textElement = spectator.query('[data-testid="message-content-text"]');
            expect(textElement).toBeTruthy();
        });
    });

    describe('Host Binding', () => {
        it('should not have active class initially', () => {
            expect(spectator.element.classList.contains('active')).toBe(false);
        });

        it('should have active class when dropzone is active', () => {
            // Trigger dragenter to activate dropzone
            const dragEvent = createDragEnterEvent(null);

            spectator.component.onDragEnter(dragEvent);
            spectator.detectChanges();

            expect(spectator.element.classList.contains('active')).toBe(true);
        });
    });

    describe('Drag Events', () => {
        describe('dragenter', () => {
            it('should activate dropzone when drag enters without fromElement', () => {
                const dragEvent = createDragEnterEvent(null);

                spectator.component.onDragEnter(dragEvent);
                spectator.detectChanges();

                expect(spectator.component.active).toBe(true);
                expect(store.resetContextMenu).toHaveBeenCalled();
            });

            it('should not activate dropzone when drag enters with fromElement', () => {
                const mockFromElement = document.createElement('div');
                const dragEvent = createDragEnterEvent(mockFromElement);

                spectator.component.onDragEnter(dragEvent);
                spectator.detectChanges();

                expect(spectator.component.active).toBe(false);
                expect(store.resetContextMenu).not.toHaveBeenCalled();
            });

            it('should prevent default and stop propagation', () => {
                const dragEvent = createDragEnterEvent(null);

                spectator.component.onDragEnter(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
                expect(dragEvent.stopPropagation).toHaveBeenCalled();
            });
        });

        describe('dragover', () => {
            it('should prevent default and stop propagation', () => {
                const dragEvent = new DragEvent('dragover');

                spectator.component.onDragOver(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
                expect(dragEvent.stopPropagation).toHaveBeenCalled();
            });
        });

        describe('dragleave', () => {
            beforeEach(() => {
                // Activate dropzone first
                const dragEnterEvent = createDragEnterEvent(null);
                spectator.component.onDragEnter(dragEnterEvent);
                spectator.detectChanges();
            });

            it('should not deactivate dropzone when drag leaves but relatedTarget is within dropzone', () => {
                const mockRelatedTarget = document.createElement('div');
                elementRefSpy.mockReturnValue(true);

                const dragEvent = createDragLeaveEvent(mockRelatedTarget);

                spectator.component.onDragLeave(dragEvent);
                spectator.detectChanges();

                expect(spectator.component.active).toBe(true);
            });

            it('should deactivate dropzone when drag leaves and relatedTarget is not within dropzone', () => {
                const mockRelatedTarget = document.createElement('div');
                elementRefSpy.mockReturnValue(false);

                const dragEvent = createDragLeaveEvent(mockRelatedTarget);

                spectator.component.onDragLeave(dragEvent);
                spectator.detectChanges();

                expect(spectator.component.active).toBe(false);
            });

            it('should prevent default', () => {
                const dragEvent = createDragLeaveEvent(null);

                spectator.component.onDragLeave(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
            });
        });

        describe('dragend', () => {
            beforeEach(() => {
                // Activate dropzone first
                const dragEnterEvent = createDragEnterEvent(null);
                spectator.component.onDragEnter(dragEnterEvent);
                spectator.detectChanges();
            });

            it('should deactivate dropzone and clear files', () => {
                const dragEvent = new DragEvent('dragend');

                spectator.component.onDragEnd(dragEvent);
                spectator.detectChanges();

                expect(spectator.component.active).toBe(false);
            });

            it('should prevent default', () => {
                const dragEvent = new DragEvent('dragend');

                spectator.component.onDragEnd(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
            });
        });

        describe('drop', () => {
            let mockFiles: FileList;

            beforeEach(() => {
                // Create mock files
                const file1 = new File(['content1'], 'test1.txt', { type: 'text/plain' });
                const file2 = new File(['content2'], 'test2.txt', { type: 'text/plain' });

                mockFiles = {
                    length: 2,
                    item: (index: number) => [file1, file2][index] || null,
                    [0]: file1,
                    [1]: file2,
                    [Symbol.iterator]: function* () {
                        yield file1;
                        yield file2;
                    }
                } as FileList;

                // Activate dropzone first
                const dragEnterEvent = createDragEnterEvent(null);
                spectator.component.onDragEnter(dragEnterEvent);
                spectator.detectChanges();
            });

            it('should deactivate dropzone and set files when files are dropped', () => {
                const dragEvent = createDropEvent(mockFiles);

                spectator.component.onDrop(dragEvent);
                spectator.detectChanges();

                expect(spectator.component.active).toBe(false);
            });

            it('should prevent default and stop propagation', () => {
                const dragEvent = createDropEvent(mockFiles);

                spectator.component.onDrop(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
                expect(dragEvent.stopPropagation).toHaveBeenCalled();
            });

            it('should handle drop without files', () => {
                const dragEvent = createDropEvent(null);

                spectator.component.onDrop(dragEvent);
                spectator.detectChanges();

                expect(spectator.component.active).toBe(false);
            });
        });
    });

    describe('Upload Files Effect', () => {
        it('should trigger upload when files are set', async () => {
            const file = new File(['content'], 'test.txt', { type: 'text/plain' });
            const mockFiles = {
                length: 1,
                item: () => file,
                [0]: file,
                [Symbol.iterator]: function* () {
                    yield file;
                }
            } as FileList;

            // Simulate drop with files
            const dragEvent = createDropEvent(mockFiles);

            spectator.component.onDrop(dragEvent);
            spectator.detectChanges();

            // Wait for effect to run
            await spectator.fixture.whenStable();

            expect(uploadFilesSpyEmitter).toHaveBeenCalledWith({
                files: mockFiles,
                targetFolderId: 'test-id'
            });
        });

        it('should not trigger upload when no files are set', async () => {
            // Just activate and deactivate without files
            const dragEnterEvent = createDragEnterEvent(null);

            spectator.component.onDragEnter(dragEnterEvent);
            spectator.detectChanges();

            const dragLeaveEvent = createDragLeaveEvent(null);

            spectator.component.onDragLeave(dragLeaveEvent);
            spectator.detectChanges();

            await spectator.fixture.whenStable();

            expect(uploadFilesSpyEmitter).not.toHaveBeenCalled();
        });

        it('should not trigger upload when files list is empty', async () => {
            const emptyFiles = {
                length: 0,
                item: () => null,
                [Symbol.iterator]: function* () {
                    // Empty generator for empty files
                }
            } as FileList;

            const dragEvent = createDropEvent(emptyFiles);

            spectator.component.onDrop(dragEvent);
            spectator.detectChanges();

            await spectator.fixture.whenStable();

            expect(uploadFilesSpyEmitter).not.toHaveBeenCalled();
        });
    });

    describe('Integration Tests', () => {
        it('should complete full drag and drop cycle', async () => {
            // 1. Start drag
            const dragEnterEvent = createDragEnterEvent(null);

            spectator.component.onDragEnter(dragEnterEvent);
            spectator.detectChanges();

            expect(spectator.component.active).toBe(true);
            expect(spectator.element.classList.contains('active')).toBe(true);

            // 2. Drag over
            const dragOverEvent = new DragEvent('dragover');

            spectator.component.onDragOver(dragOverEvent);
            expect(spectator.component.active).toBe(true);

            // 3. Drop files
            const file = new File(['content'], 'test.txt', { type: 'text/plain' });
            const mockFiles = {
                length: 1,
                item: () => file,
                [0]: file,
                [Symbol.iterator]: function* () {
                    yield file;
                }
            } as FileList;

            const dropEvent = createDropEvent(mockFiles);

            spectator.component.onDrop(dropEvent);
            spectator.detectChanges();

            await spectator.fixture.whenStable();

            expect(uploadFilesSpyEmitter).toHaveBeenCalledWith({
                files: mockFiles,
                targetFolderId: 'test-id'
            });

            expect(spectator.component.active).toBe(false);
            expect(spectator.element.classList.contains('active')).toBe(false);
        });

        it('should handle drag leave without dropping', () => {
            // 1. Start drag
            const dragEnterEvent = createDragEnterEvent(null);

            spectator.component.onDragEnter(dragEnterEvent);
            spectator.detectChanges();

            expect(spectator.component.active).toBe(true);

            // 2. Leave dropzone
            const dragLeaveEvent = createDragLeaveEvent(null);

            spectator.component.onDragLeave(dragLeaveEvent);
            spectator.detectChanges();

            expect(spectator.component.active).toBe(false);
            expect(spectator.element.classList.contains('active')).toBe(false);
        });
    });
});
