import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { EMPTY, of } from 'rxjs';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    BUNDLE_STATE,
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
                deploy: jest.fn().mockReturnValue(of({})),
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
        jest.clearAllMocks();
        spectator = createComponent();
        component = spectator.component;
    });

    describe('drag and drop', () => {
        it('should show drop overlay while dragging and hide it when drag leaves', () => {
            const event = { preventDefault: jest.fn(), dataTransfer: null } as unknown as DragEvent;
            component.onDragEnter(event);
            expect(component.isDragging()).toBe(true);

            spectator.detectChanges();
            expect(spectator.query('[data-testid="plugins-drop-overlay"]')).toBeTruthy();

            component.onDragLeave(event);
            expect(component.isDragging()).toBe(false);

            spectator.detectChanges();
            expect(spectator.query('[data-testid="plugins-drop-overlay"]')).toBeNull();
        });

        it('should keep isDragging true until all nested dragenter events have a matching dragleave', () => {
            const event = { preventDefault: jest.fn(), dataTransfer: null } as unknown as DragEvent;
            component.onDragEnter(event);
            component.onDragEnter(event);
            component.onDragLeave(event);
            expect(component.isDragging()).toBe(true);
            component.onDragLeave(event);
            expect(component.isDragging()).toBe(false);
        });

        it('should upload only jar files on drop and reset drag state', () => {
            const jarFile = makeFile('plugin.jar');
            const txtFile = makeFile('readme.txt');
            const event = makeDragEvent([jarFile, txtFile]);
            jest.spyOn(component.store, 'uploadBundles');
            component.isDragging.set(true);

            component.onDrop(event);

            expect(component.isDragging()).toBe(false);
            expect(component.store.uploadBundles).toHaveBeenCalledWith([jarFile]);
        });

        it('should show an error and not upload when dropped files contain no jars', () => {
            const event = makeDragEvent([makeFile('readme.txt')]);
            const pushSpy = jest.spyOn(component['dotMessageDisplayService'], 'push');
            jest.spyOn(component.store, 'uploadBundles');

            component.onDrop(event);

            expect(component.store.uploadBundles).not.toHaveBeenCalled();
            expect(pushSpy).toHaveBeenCalledWith(
                expect.objectContaining({ severity: expect.any(String) })
            );
        });

        it('should do nothing when drop event has no files', () => {
            const event = makeDragEvent([]);
            jest.spyOn(component.store, 'uploadBundles');
            const pushSpy = jest.spyOn(component['dotMessageDisplayService'], 'push');

            component.onDrop(event);

            expect(component.store.uploadBundles).not.toHaveBeenCalled();
            expect(pushSpy).not.toHaveBeenCalled();
        });
    });

    describe('context menu', () => {
        const mockShowSpy = jest.fn();

        const openContextMenu = (bundle: object) => {
            jest.spyOn(component, 'contextMenu').mockReturnValue({
                show: mockShowSpy
            } as never);
            component.onContextMenu(new MouseEvent('contextmenu'), bundle as never);
        };

        beforeEach(() => mockShowSpy.mockClear());

        it('should open the context menu on right-click', () => {
            openContextMenu({
                jarFile: 'test.jar',
                symbolicName: 'test',
                state: BUNDLE_STATE.ACTIVE
            });
            expect(mockShowSpy).toHaveBeenCalled();
        });

        describe('installed bundle actions', () => {
            it('should show Stop as first action when bundle is ACTIVE', () => {
                openContextMenu({
                    jarFile: 'test.jar',
                    symbolicName: 'test',
                    state: BUNDLE_STATE.ACTIVE
                });
                expect(component.contextMenuItems()[0].label).toBe('plugins.stop');
            });

            it('should show Start as first action when bundle is not ACTIVE', () => {
                openContextMenu({
                    jarFile: 'test.jar',
                    symbolicName: 'test',
                    state: BUNDLE_STATE.RESOLVED
                });
                expect(component.contextMenuItems()[0].label).toBe('plugins.start');
            });

            it('should include undeploy, separator, process exports and add-to-bundle actions', () => {
                openContextMenu({
                    jarFile: 'test.jar',
                    symbolicName: 'test',
                    state: BUNDLE_STATE.ACTIVE
                });
                const items = component.contextMenuItems();
                expect(items[1].label).toBe('plugins.undeploy');
                expect(items[2].separator).toBe(true);
                expect(items[3].label).toBe('plugins.process-exports');
                expect(items[4].label).toBe('plugins.add-to-bundle');
            });

            it('should call processExports with the bundle symbolic name', () => {
                jest.spyOn(component.store, 'processExports');
                openContextMenu({
                    jarFile: 'test.jar',
                    symbolicName: 'test-bundle',
                    state: BUNDLE_STATE.ACTIVE
                });
                component.contextMenuItems()[3].command!({} as never);
                expect(component.store.processExports).toHaveBeenCalledWith('test-bundle');
            });

            it('should set addToBundleIdentifier to the jar file name', () => {
                openContextMenu({
                    jarFile: 'test.jar',
                    symbolicName: 'test',
                    state: BUNDLE_STATE.ACTIVE
                });
                component.contextMenuItems()[4].command!({} as never);
                expect(component.addToBundleIdentifier()).toBe('test.jar');
            });
        });

        describe('undeployed bundle actions', () => {
            it('should show only the Deploy action for an undeployed jar', () => {
                openContextMenu({
                    jarFile: 'new-plugin.jar',
                    symbolicName: 'new-plugin.jar',
                    state: 'undeployed'
                });
                const items = component.contextMenuItems();
                expect(items).toHaveLength(1);
                expect(items[0].label).toBe('plugins.deploy');
            });

            it('should call store.deploy with the jar file name', () => {
                jest.spyOn(component.store, 'deploy');
                openContextMenu({
                    jarFile: 'new-plugin.jar',
                    symbolicName: 'new-plugin.jar',
                    state: 'undeployed'
                });
                component.contextMenuItems()[0].command!({} as never);
                expect(component.store.deploy).toHaveBeenCalledWith('new-plugin.jar');
            });
        });
    });
});
