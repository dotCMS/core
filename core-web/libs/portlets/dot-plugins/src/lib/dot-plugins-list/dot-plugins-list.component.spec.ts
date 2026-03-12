import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService, DotOsgiService } from '@dotcms/data-access';

import { DotPluginsListComponent } from './dot-plugins-list.component';
import { DotPluginsListStore } from './store/dot-plugins-list.store';

const makeFile = (name: string): File => new File(['content'], name);

const makeDragEvent = (files: File[]): DragEvent => {
    const dataTransfer = { files: files as unknown as FileList };
    return { preventDefault: jest.fn(), dataTransfer } as unknown as DragEvent;
};

describe('DotPluginsListComponent', () => {
    let spectator: Spectator<DotPluginsListComponent>;
    let component: DotPluginsListComponent;

    const createComponent = createComponentFactory({
        component: DotPluginsListComponent,
        providers: [
            DotPluginsListStore,
            mockProvider(DialogService),
            mockProvider(DotMessageService, { get: (key: string) => key }),
            mockProvider(DotOsgiService, {
                getInstalledBundles: jest.fn().mockReturnValue(of({ entity: [] })),
                getAvailablePlugins: jest.fn().mockReturnValue(of({ entity: [] })),
                uploadBundles: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ConfirmationService),
            mockProvider(MessageService, { add: jest.fn() })
        ],
        shallow: true
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have toolbar with upload, refresh and restart buttons', () => {
        expect(spectator.query('[data-testid="plugins-upload-btn"]')).toBeTruthy();
        expect(spectator.query('[data-testid="plugins-refresh-btn"]')).toBeTruthy();
        expect(spectator.query('[data-testid="plugins-restart-btn"]')).toBeTruthy();
    });

    it('should have plugins table', () => {
        expect(spectator.query('[data-testid="plugins-table"]')).toBeTruthy();
    });

    it('should show drag-and-drop hint text', () => {
        expect(spectator.query('[data-testid="plugins-drag-hint"]')).toBeTruthy();
    });

    describe('drag and drop', () => {
        it('should set isDragging to true on dragenter', () => {
            const event = { preventDefault: jest.fn(), dataTransfer: null } as unknown as DragEvent;
            component.onDragEnter(event);
            expect(component.isDragging()).toBe(true);
            expect(event.preventDefault).toHaveBeenCalled();
        });

        it('should set isDragging to false when all drag enters have left', () => {
            const event = { preventDefault: jest.fn(), dataTransfer: null } as unknown as DragEvent;
            component.onDragEnter(event);
            component.onDragEnter(event);
            component.onDragLeave(event);
            expect(component.isDragging()).toBe(true);
            component.onDragLeave(event);
            expect(component.isDragging()).toBe(false);
        });

        it('should call preventDefault on dragover', () => {
            const event = { preventDefault: jest.fn() } as unknown as DragEvent;
            component.onDragOver(event);
            expect(event.preventDefault).toHaveBeenCalled();
        });

        it('should upload jar files on drop and reset drag state', () => {
            const jarFile = makeFile('plugin.jar');
            const event = makeDragEvent([jarFile]);
            jest.spyOn(component.store, 'uploadBundles');
            component.isDragging.set(true);

            component.onDrop(event);

            expect(component.isDragging()).toBe(false);
            expect(component.store.uploadBundles).toHaveBeenCalledWith(
                [jarFile],
                expect.any(Function)
            );
        });

        it('should show error toast and not upload when no jar files are dropped', () => {
            const txtFile = makeFile('readme.txt');
            const event = makeDragEvent([txtFile]);
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const addSpy = jest.spyOn(component['messageService'], 'add');
            jest.spyOn(component.store, 'uploadBundles');

            component.onDrop(event);

            expect(component.store.uploadBundles).not.toHaveBeenCalled();
            expect(addSpy).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
        });

        it('should do nothing when no files are dropped', () => {
            const event = makeDragEvent([]);
            jest.spyOn(component.store, 'uploadBundles');
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const addSpy = jest.spyOn(component['messageService'], 'add');

            component.onDrop(event);

            expect(component.store.uploadBundles).not.toHaveBeenCalled();
            expect(addSpy).not.toHaveBeenCalled();
        });

        it('should filter only jar files when dropping mixed files', () => {
            const jarFile = makeFile('plugin.jar');
            const txtFile = makeFile('readme.txt');
            const event = makeDragEvent([jarFile, txtFile]);
            jest.spyOn(component.store, 'uploadBundles');

            component.onDrop(event);

            expect(component.store.uploadBundles).toHaveBeenCalledWith(
                [jarFile],
                expect.any(Function)
            );
        });

        it('should show drop overlay when isDragging is true', () => {
            component.isDragging.set(true);
            spectator.detectChanges();
            expect(spectator.query('[data-testid="plugins-drop-overlay"]')).toBeTruthy();
        });

        it('should hide drop overlay when isDragging is false', () => {
            component.isDragging.set(false);
            spectator.detectChanges();
            expect(spectator.query('[data-testid="plugins-drop-overlay"]')).toBeNull();
        });
    });
});
