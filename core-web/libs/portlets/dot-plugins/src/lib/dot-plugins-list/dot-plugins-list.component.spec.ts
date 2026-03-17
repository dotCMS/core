import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { EMPTY, of } from 'rxjs';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotOsgiService
} from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';

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
                uploadBundles: jest.fn().mockReturnValue(of({})),
                processExports: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ConfirmationService),
            mockProvider(DotMessageDisplayService, { push: jest.fn() }),
            mockProvider(DotcmsEventsService, { subscribeTo: jest.fn().mockReturnValue(EMPTY) })
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
            expect(component.store.uploadBundles).toHaveBeenCalledWith([jarFile]);
        });

        it('should show error via DotMessageDisplayService and not upload when no jar files are dropped', () => {
            const txtFile = makeFile('readme.txt');
            const event = makeDragEvent([txtFile]);
            const pushSpy = jest.spyOn(component['dotMessageDisplayService'], 'push');
            jest.spyOn(component.store, 'uploadBundles');

            component.onDrop(event);

            expect(component.store.uploadBundles).not.toHaveBeenCalled();
            expect(pushSpy).toHaveBeenCalledWith(
                expect.objectContaining({ severity: expect.any(String) })
            );
        });

        it('should do nothing when no files are dropped', () => {
            const event = makeDragEvent([]);
            jest.spyOn(component.store, 'uploadBundles');
            const pushSpy = jest.spyOn(component['dotMessageDisplayService'], 'push');
            pushSpy.mockClear();

            component.onDrop(event);

            expect(component.store.uploadBundles).not.toHaveBeenCalled();
            expect(pushSpy).not.toHaveBeenCalled();
        });

        it('should filter only jar files when dropping mixed files', () => {
            const jarFile = makeFile('plugin.jar');
            const txtFile = makeFile('readme.txt');
            const event = makeDragEvent([jarFile, txtFile]);
            jest.spyOn(component.store, 'uploadBundles');

            component.onDrop(event);

            expect(component.store.uploadBundles).toHaveBeenCalledWith([jarFile]);
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

    describe('context menu', () => {
        const mockBundle = {
            bundleId: 1,
            symbolicName: 'test-bundle',
            jarFile: 'test.jar',
            state: 32,
            isSystem: false
        };
        const systemBundle = { ...mockBundle, isSystem: true };

        it('should set selectedBundle and show context menu for non-system bundle', () => {
            const event = new MouseEvent('contextmenu');
            const showSpy = jest.fn();
            jest.spyOn(component, 'contextMenu').mockReturnValue({ show: showSpy } as never);

            component.onContextMenu(event, mockBundle);

            expect(showSpy).toHaveBeenCalledWith(event);
        });

        it('should not show context menu for system bundle', () => {
            const event = new MouseEvent('contextmenu');
            const showSpy = jest.fn();
            jest.spyOn(component, 'contextMenu').mockReturnValue({ show: showSpy } as never);

            component.onContextMenu(event, systemBundle);

            expect(showSpy).not.toHaveBeenCalled();
        });

        it('should return menu items for a selected bundle', () => {
            const event = new MouseEvent('contextmenu');
            jest.spyOn(component, 'contextMenu').mockReturnValue({ show: jest.fn() } as never);
            component.onContextMenu(event, mockBundle);

            const items = component.contextMenuItems();
            expect(items).toHaveLength(2);
            expect(items[0].icon).toBe('pi pi-cog');
            expect(items[1].icon).toBe('pi pi-box');
        });

        it('should call processExports when Process Exports is clicked', () => {
            const event = new MouseEvent('contextmenu');
            jest.spyOn(component, 'contextMenu').mockReturnValue({ show: jest.fn() } as never);
            jest.spyOn(component.store, 'processExports');
            component.onContextMenu(event, mockBundle);

            component.contextMenuItems()[0].command!({} as never);

            expect(component.store.processExports).toHaveBeenCalledWith('test-bundle');
        });

        it('should set addToBundleIdentifier when Add to Bundle is clicked', () => {
            const event = new MouseEvent('contextmenu');
            jest.spyOn(component, 'contextMenu').mockReturnValue({ show: jest.fn() } as never);
            component.onContextMenu(event, mockBundle);

            component.contextMenuItems()[1].command!({} as never);

            expect(component.addToBundleIdentifier()).toBe('test.jar');
        });

        it('should clear addToBundleIdentifier when set to null', () => {
            component.addToBundleIdentifier.set('test.jar');
            component.addToBundleIdentifier.set(null);
            expect(component.addToBundleIdentifier()).toBeNull();
        });
    });
});
