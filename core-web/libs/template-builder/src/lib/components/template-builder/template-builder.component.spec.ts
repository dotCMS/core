import { expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { map, take } from 'rxjs/operators';

import {
    DotContainersService,
    DotCurrentUserService,
    DotEventsService,
    DotMessageService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { LoginService, SiteService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import {
    containersMock,
    DotContainersServiceMock,
    DotCurrentUserServiceMock,
    LoginServiceMock,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotGridStackWidget, SCROLL_DIRECTION } from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { parseFromDotObjectToGridStack } from './utils/gridstack-utils';
import {
    CONTAINER_MAP_MOCK,
    DOT_MESSAGE_SERVICE_TB_MOCK,
    FULL_DATA_MOCK,
    INITIAL_STATE_MOCK,
    ROWS_MOCK
} from './utils/mocks';

global.structuredClone = jest.fn((val) => {
    return JSON.parse(JSON.stringify(val));
});

const mockRect = {
    top: 120,
    bottom: 100,
    x: 146,
    y: 50,
    width: 440,
    height: 240,
    right: 586,
    left: 146,
    toJSON: jest.fn()
};

describe('TemplateBuilderComponent', () => {
    let spectator: Spectator<TemplateBuilderComponent>;
    let store: DotTemplateBuilderStore;
    let dialog: DialogService;
    let dotContainersService: DotContainersService;
    let openDialogMock: jest.SpyInstance;
    let defaultContainerSpy: jest.SpyInstance;
    const mockContainer = containersMock[0];

    const createComponent = createComponentFactory({
        component: TemplateBuilderComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            DotTemplateBuilderStore,
            DialogService,
            DynamicDialogRef,
            {
                provide: DotCurrentUserService,
                useClass: DotCurrentUserServiceMock
            },
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            },
            {
                provide: SiteService,
                useClass: SiteServiceMock
            },
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            {
                provide: DotSystemConfigService,
                useValue: { getSystemConfig: () => of({}) }
            },
            {
                provide: GlobalStore,
                useValue: { currentSiteId: () => null }
            },
            DotEventsService
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                layout: {
                    body: FULL_DATA_MOCK,
                    header: true,
                    footer: true,
                    sidebar: null,
                    width: 'Mobile',
                    title: 'Test Title'
                },
                template: {
                    identifier: '111',
                    themeId: '123'
                },
                containerMap: CONTAINER_MAP_MOCK
            }
        });

        store = spectator.inject(DotTemplateBuilderStore, true);
        dialog = spectator.inject(DialogService);
        openDialogMock = jest.spyOn(dialog, 'open');
        dotContainersService = spectator.inject(DotContainersService, true);
        defaultContainerSpy = jest.spyOn(dotContainersService.defaultContainer$, 'pipe');
        spectator.detectChanges();
    });

    it('should not trigger a template change when store is initialized', () => {
        // Store init is called on init
        const changeMock = jest.spyOn(spectator.component.templateChange, 'emit');
        expect(changeMock).not.toHaveBeenCalled();
    });

    describe('ngOnInit and defaultContainer$ subscription', () => {
        it('should subscribe to defaultContainer$ on ngOnInit', () => {
            expect(defaultContainerSpy).toHaveBeenCalled();
        });
    });

    it("should call updateOldRows from the store when the layout changes and it's not the first time", () => {
        const updateOldRowsMock = jest.spyOn(store, 'updateOldRows');
        const templateUpdateMock = jest.spyOn(spectator.component.templateChange, 'emit');

        spectator.setInput('layout', {
            body: FULL_DATA_MOCK,
            header: true,
            footer: true,
            sidebar: null,
            width: 'Mobile',
            title: 'Test Title'
        });

        spectator.detectChanges();

        expect(updateOldRowsMock).toHaveBeenCalled();

        expect(templateUpdateMock).not.toHaveBeenCalled();
    });

    it('should have a Add Row Button', () => {
        expect(spectator.query(byTestId('add-row'))).toBeTruthy();
    });

    it('should have a Add Box Button', () => {
        expect(spectator.query(byTestId('add-box'))).toBeTruthy();
    });

    it('should have the same quantity of rows as mocked data', () => {
        expect(spectator.queryAll(byTestId('row')).length).toBe(FULL_DATA_MOCK.rows.length);
    });

    it('should have the same quantity of boxes as mocked data', () => {
        const totalBoxes = FULL_DATA_MOCK.rows.reduce((acc, row) => {
            return acc + row.columns.length;
        }, 0);
        expect(spectator.queryAll(byTestId(/builder-box-\d+/)).length).toBe(totalBoxes);
    });

    it('should trigger removeColumn on store when triggering removeColumn', (done) => {
        jest.spyOn(store, 'removeColumn');
        jest.spyOn(spectator.component, 'removeColumn');

        const builderBox1 = spectator.debugElement.query(By.css('[data-testId="builder-box-1"]'));

        spectator.triggerEventHandler(builderBox1, 'deleteColumn', undefined);
        expect(spectator.component.removeColumn).toHaveBeenCalled();

        // Wait for GridStack to be initialized via requestAnimationFrame
        requestAnimationFrame(() => {
            const box1 = spectator.debugElement.query(By.css('[data-testId="box-1"]'));
            const rowId = box1.nativeElement
                .closest('dotcms-template-builder-row')
                .getAttribute('gs-id');

            const box1Id = box1.nativeElement.getAttribute('gs-id');

            spectator.component.removeColumn(
                { id: box1Id, parentId: rowId },
                box1.nativeElement,
                rowId
            );
            expect(store.removeColumn).toHaveBeenCalledWith({
                ...{ id: box1Id, parentId: rowId },
                parentId: rowId
            });
            done();
        });
    });

    it('should call addContainer from store when triggering addContainer', (done) => {
        const addContainerMock = jest.spyOn(store, 'addContainer');

        let widgetToAddContainer: DotGridStackWidget;
        let rowId: string;

        store.state$.pipe(take(1)).subscribe(({ rows: items }) => {
            widgetToAddContainer = items[0].subGridOpts.children[0];
            rowId = items[0].id as string;

            spectator.component.addContainer(widgetToAddContainer, rowId, mockContainer);

            expect(addContainerMock).toHaveBeenCalled();
            done();
        });
    });

    it('should call deleteContainer from store when triggering deleteContainer', (done) => {
        const deleteContainerMock = jest.spyOn(store, 'deleteContainer');

        let widgetToDeleteContainer: DotGridStackWidget;
        let rowId: string;

        store.state$.pipe(take(1)).subscribe(({ rows: items }) => {
            widgetToDeleteContainer = items[0].subGridOpts.children[0];
            rowId = items[0].id as string;

            spectator.component.deleteContainer(widgetToDeleteContainer, rowId, 0);

            expect(deleteContainerMock).toHaveBeenCalled();
            done();
        });
    });

    it('should open a dialog when clicking on row-style-class-button ', () => {
        const editRowStyleClassesButton = spectator.query(byTestId('row-style-class-button'));

        spectator.dispatchFakeEvent(editRowStyleClassesButton, 'onClick');

        expect(openDialogMock).toHaveBeenCalled();
    });

    it('should open a dialog when clicking on box-style-class-button', () => {
        const editBoxStyleClassesButton = spectator.query(byTestId('box-style-class-button'));

        spectator.dispatchFakeEvent(editBoxStyleClassesButton, 'onClick');

        expect(openDialogMock).toHaveBeenCalled();
    });

    it('should open a panel when clicking on Layout button', () => {
        const actionsButton = spectator.query(byTestId('btn-select-layout'));

        spectator.click(actionsButton);

        expect(spectator.query(byTestId('template-layout-properties-panel'))).toBeTruthy();
    });

    it('should have a row with class "template-builder-row--wont-fit" when a box wont fit in the row and the Add Box button is dragging', () => {
        spectator.component.addBoxIsDragging = true;

        store.setState((state) => ({
            ...state,
            rows: ROWS_MOCK
        }));

        spectator.detectChanges();

        expect(spectator.queryAll('.template-builder-row--wont-fit').length).toBe(1);
    });

    it('should trigger fixGridStackNodeOptions when triggering mousemove on main div', () => {
        const fixGridStackNodeOptionsMock = jest.spyOn(
            spectator.component,
            'fixGridStackNodeOptions'
        );
        const mainDiv = spectator.query(byTestId('template-builder-main'));

        mainDiv.dispatchEvent(new MouseEvent('mousemove'));

        expect(fixGridStackNodeOptionsMock).toHaveBeenCalled();
    });

    it('should set layoutProperties to default values if sidebar null', () => {
        expect(spectator.component.layoutProperties).toEqual({
            header: true,
            footer: true,
            sidebar: { location: '', width: 'medium', containers: [] }
        });
    });

    it("should trigger deleteSection on header when clicking on 'Delete Section' button", () => {
        const deleteSectionMock = jest.spyOn(spectator.component, 'deleteSection');
        const headerComponent = spectator.query(byTestId('template-builder-header'));
        const deleteSectionButton = headerComponent.querySelector(
            '[data-testId="delete-section-button"]'
        );

        // `p-button` emits through its internal <button>, clicking the host element won't trigger `(onClick)`
        spectator.click(deleteSectionButton.querySelector('button'));

        expect(deleteSectionMock).toHaveBeenCalledWith('header');
    });

    it("should trigger deleteSection on footer when clicking on 'Delete Section' button", () => {
        const deleteSectionMock = jest.spyOn(spectator.component, 'deleteSection');
        const footerComponent = spectator.query(byTestId('template-builder-footer'));
        const deleteSectionButton = footerComponent.querySelector(
            '[data-testId="delete-section-button"]'
        );

        // `p-button` emits through its internal <button>, clicking the host element won't trigger `(onClick)`
        spectator.click(deleteSectionButton.querySelector('button'));

        expect(deleteSectionMock).toHaveBeenCalledWith('footer');
    });

    it("should emit changes with a not null layout when the theme is changed and layoutProperties or rows weren't touched", () => {
        const layoutChangeMock = jest.spyOn(spectator.component.templateChange, 'emit');

        // Theme changes are routed through TemplateBuilderActions -> TemplateBuilderComponent.updateTheme()
        spectator.component.updateTheme('test-123');

        expect(layoutChangeMock).toHaveBeenCalledWith({
            layout: {
                body: FULL_DATA_MOCK,
                header: true,
                footer: true,
                sidebar: null,
                width: 'Mobile',
                title: 'Test Title'
            },
            themeId: 'test-123'
        });
    });

    describe('layoutChange', () => {
        it('should emit layoutChange when the store changes', (done) => {
            const layoutChangeMock = jest.spyOn(spectator.component.templateChange, 'emit');

            spectator.detectChanges();

            store.setState({
                ...INITIAL_STATE_MOCK,
                rows: parseFromDotObjectToGridStack(FULL_DATA_MOCK),
                layoutProperties: {
                    header: true,
                    footer: true,
                    sidebar: {
                        containers: [],
                        location: 'left',
                        width: 'small'
                    }
                }
            });

            store.vm$
                .pipe(
                    map((x) => x?.items),
                    take(1)
                )
                .subscribe(() => {
                    expect(layoutChangeMock).toHaveBeenCalledWith({
                        layout: {
                            body: FULL_DATA_MOCK,
                            header: true,
                            footer: true,
                            sidebar: {
                                containers: [],
                                location: 'left',
                                width: 'small'
                            },
                            width: 'Mobile',
                            title: 'Test Title'
                        },
                        themeId: '123'
                    });
                    done();
                });
        });
    });

    it('should emit layoutChange when the layoutProperties changes', (done) => {
        const LAYOUT_PROPERTIES_MOCK = {
            header: false,
            footer: true,
            sidebar: {
                containers: [],
                location: 'right',
                width: 'medium'
            }
        };

        const layoutChangeMock = jest.spyOn(spectator.component.templateChange, 'emit');

        store.updateLayoutProperties(LAYOUT_PROPERTIES_MOCK);

        spectator.detectChanges();

        store.vm$
            .pipe(
                map((x) => x?.layoutProperties),
                take(1)
            )
            .subscribe(() => {
                expect(layoutChangeMock).toHaveBeenCalledWith({
                    layout: {
                        ...LAYOUT_PROPERTIES_MOCK,
                        body: FULL_DATA_MOCK,
                        width: 'Mobile',
                        title: 'Test Title'
                    },
                    themeId: '123'
                });
                done();
            });
    });

    describe('disabled input', () => {
        it('should not render the disabled overlay by default', () => {
            expect(spectator.query(byTestId('template-builder-disabled-overlay'))).toBeFalsy();
        });

        it('should render the disabled overlay when disabled is set to true', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();
            expect(spectator.query(byTestId('template-builder-disabled-overlay'))).toBeTruthy();
        });

        it('should hide the disabled overlay when disabled is set back to false', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();
            spectator.setInput('disabled', false);
            spectator.detectChanges();
            expect(spectator.query(byTestId('template-builder-disabled-overlay'))).toBeFalsy();
        });

        describe('grid interactions', () => {
            let mockGrid: {
                disable: jest.Mock;
                enable: jest.Mock;
                el: { querySelectorAll: jest.Mock };
            };

            beforeEach(() => {
                mockGrid = {
                    disable: jest.fn(),
                    enable: jest.fn(),
                    load: jest.fn(),
                    save: jest.fn().mockReturnValue([{ id: 'row-1', x: 0, y: 0, w: 12, h: 1 }]),
                    el: { querySelectorAll: jest.fn().mockReturnValue([]) }
                };
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                spectator.component.grid = mockGrid as any;
            });

            it('should call grid.disable() when disabled becomes true', () => {
                spectator.setInput('disabled', true);
                expect(mockGrid.disable).toHaveBeenCalled();
            });

            it('should dispatch a synthetic mouseup to terminate an in-progress drag before locking the grid', () => {
                const dispatchSpy = jest.spyOn(document, 'dispatchEvent');
                spectator.component.draggingElement = document.createElement('div');

                spectator.setInput('disabled', true);

                expect(dispatchSpy).toHaveBeenCalledWith(
                    expect.objectContaining({ type: 'mouseup' })
                );
                expect(mockGrid.disable).toHaveBeenCalled();

                dispatchSpy.mockRestore();
            });

            it('should restore the pre-drag layout with grid.load() to undo the committed drag position', () => {
                const savedState = [{ id: 'row-1', x: 0, y: 0, w: 12, h: 1 }];
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                (spectator.component as any).preDragState = savedState;
                spectator.component.draggingElement = document.createElement('div');

                spectator.setInput('disabled', true);

                expect(mockGrid.load).toHaveBeenCalledWith(savedState);
            });

            it('should NOT call grid.load() when there is no pre-drag state (external drag-in)', () => {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                (spectator.component as any).preDragState = null;
                spectator.component.draggingElement = document.createElement('div');

                spectator.setInput('disabled', true);

                expect(mockGrid.load).not.toHaveBeenCalled();
            });

            it('should call load() on preDragGrid (not main grid) when cancelling a column drag', () => {
                const savedState = [{ id: 'col-1', x: 0, y: 0, w: 6, h: 1 }];
                const subGridLoad = jest.fn();
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                (spectator.component as any).preDragState = savedState;
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                (spectator.component as any).preDragGrid = { load: subGridLoad };
                spectator.component.draggingElement = document.createElement('div');

                spectator.setInput('disabled', true);

                expect(subGridLoad).toHaveBeenCalledWith(savedState);
                expect(mockGrid.load).not.toHaveBeenCalled();
            });

            it('should reset suppressStoreUpdates to false after cancelling an in-progress drag', () => {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                (spectator.component as any).preDragState = [
                    { id: 'row-1', x: 0, y: 0, w: 12, h: 1 }
                ];
                spectator.component.draggingElement = document.createElement('div');

                spectator.setInput('disabled', true);

                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                expect((spectator.component as any).suppressStoreUpdates).toBe(false);
            });

            it('should NOT dispatch mouseup when disabled becomes true with no active drag', () => {
                const dispatchSpy = jest.spyOn(document, 'dispatchEvent');
                spectator.component.draggingElement = null;

                spectator.setInput('disabled', true);

                const mouseupCalls = dispatchSpy.mock.calls.filter(
                    ([event]) => (event as Event).type === 'mouseup'
                );
                expect(mouseupCalls).toHaveLength(0);
                expect(mockGrid.disable).toHaveBeenCalled();

                dispatchSpy.mockRestore();
            });

            it('should dispatch Escape to close open PrimeNG panels when disabled becomes true', () => {
                const dispatchSpy = jest.spyOn(document, 'dispatchEvent');

                spectator.setInput('disabled', true);

                expect(dispatchSpy).toHaveBeenCalledWith(
                    expect.objectContaining({ type: 'keydown', key: 'Escape' })
                );

                dispatchSpy.mockRestore();
            });

            it('should call grid.enable() when disabled becomes false', () => {
                spectator.setInput('disabled', true);
                spectator.setInput('disabled', false);
                expect(mockGrid.enable).toHaveBeenCalled();
            });

            it('should disable all subgrids when disabled becomes true', () => {
                const subGridDisable = jest.fn();
                const subGridEnable = jest.fn();
                mockGrid.el.querySelectorAll.mockReturnValue([
                    { gridstack: { disable: subGridDisable, enable: subGridEnable } }
                ]);

                spectator.setInput('disabled', true);
                expect(subGridDisable).toHaveBeenCalled();
            });

            it('should enable all subgrids when disabled becomes false', () => {
                const subGridDisable = jest.fn();
                const subGridEnable = jest.fn();
                mockGrid.el.querySelectorAll.mockReturnValue([
                    { gridstack: { disable: subGridDisable, enable: subGridEnable } }
                ]);

                spectator.setInput('disabled', true);
                spectator.setInput('disabled', false);
                expect(subGridEnable).toHaveBeenCalled();
            });

            it('should not call grid methods if grid is not yet initialized', () => {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                spectator.component.grid = undefined as any;
                expect(() => spectator.setInput('disabled', true)).not.toThrow();
            });
        });
    });

    describe('setSubGridEvent — dropped handler', () => {
        let handlers: Record<string, (...args: unknown[]) => void>;
        let mockSubGrid: {
            on: jest.Mock;
            removeWidget: jest.Mock;
        };
        let store: DotTemplateBuilderStore;

        beforeEach(() => {
            store = spectator.inject(DotTemplateBuilderStore);
            handlers = {};
            mockSubGrid = {
                removeWidget: jest.fn(),
                on: jest.fn().mockImplementation(function (this: unknown, event, cb) {
                    handlers[event as string] = cb;

                    return this; // fluent chain
                })
            };

            spectator.component.setSubGridEvent(mockSubGrid as never);
        });

        it('should call store.subGridOnDropped and onDragStop when not suppressed', () => {
            const subGridOnDroppedSpy = jest.spyOn(store, 'subGridOnDropped');
            const onDragStopSpy = jest.spyOn(spectator.component, 'onDragStop');
            const el = document.createElement('div');
            const newNode = { el, grid: mockSubGrid };

            handlers['dropped']({}, {}, newNode);

            expect(subGridOnDroppedSpy).toHaveBeenCalled();
            expect(onDragStopSpy).toHaveBeenCalled();
            expect(mockSubGrid.removeWidget).not.toHaveBeenCalled();
        });

        it('should removeWidget and skip store.subGridOnDropped when suppressStoreUpdates is true', () => {
            const subGridOnDroppedSpy = jest.spyOn(store, 'subGridOnDropped');
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (spectator.component as any).suppressStoreUpdates = true;

            const el = document.createElement('div');
            const newNode = { el, grid: mockSubGrid };

            handlers['dropped']({}, {}, newNode);

            expect(mockSubGrid.removeWidget).toHaveBeenCalledWith(el, true, false);
            expect(subGridOnDroppedSpy).not.toHaveBeenCalled();
        });

        it('should not throw when newNode.el is null during cancel window', () => {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (spectator.component as any).suppressStoreUpdates = true;
            const newNode = { el: null, grid: mockSubGrid };

            expect(() => handlers['dropped']({}, {}, newNode)).not.toThrow();
        });
    });

    describe('Scroll on Drag', () => {
        beforeEach(() => {
            spectator.component.templateContainerRef = {
                nativeElement: document.createElement('div')
            };

            spectator.detectChanges();
        });

        it('should not scroll if draggingElement is null', () => {
            spectator.component.draggingElement = null;
            spectator.component.onMouseMove();
            expect(spectator.component.scrollDirection).toBe(SCROLL_DIRECTION.NONE);
        });

        it('should scroll up if the element is close to the top of the container', () => {
            const spy = jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => 1);
            spectator.component.draggingElement = document.createElement('div');
            jest.spyOn(
                spectator.component.draggingElement,
                'getBoundingClientRect'
            ).mockReturnValue({
                ...mockRect,
                top: 0
            });
            jest.spyOn(
                spectator.component.templateContaniner,
                'getBoundingClientRect'
            ).mockReturnValue({
                ...mockRect,
                top: 0
            });

            spectator.component.onMouseMove();
            expect(spectator.component.scrollDirection).toBe(SCROLL_DIRECTION.UP);
            expect(spy).toHaveBeenCalled();
        });

        it('should scroll down if the element is close to the bottom of the container', () => {
            const spy = jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => 1);
            spectator.component.draggingElement = document.createElement('div');
            jest.spyOn(
                spectator.component.draggingElement,
                'getBoundingClientRect'
            ).mockReturnValue({
                ...mockRect,
                top: 500,
                bottom: 0
            });
            jest.spyOn(
                spectator.component.templateContaniner,
                'getBoundingClientRect'
            ).mockReturnValue({
                ...mockRect,
                top: 100,
                bottom: 0
            });

            spectator.component.onMouseMove();

            expect(spectator.component.scrollDirection).toBe(SCROLL_DIRECTION.DOWN);
            expect(spy).toHaveBeenCalled();
        });
    });
});
