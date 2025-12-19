import { expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { pluck, take } from 'rxjs/operators';


import { DotContainersService, DotEventsService, DotMessageService, DotSystemConfigService } from '@dotcms/data-access';
import { CoreWebService, LoginService, SiteService } from '@dotcms/dotcms-js';
import {
    containersMock,
    CoreWebServiceMock,
    DotContainersServiceMock,
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
        imports: [HttpClientTestingModule],
        providers: [
            DotTemplateBuilderStore,
            DialogService,
            DynamicDialogRef,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            },
            {
                provide: CoreWebService,
                useClass: CoreWebServiceMock
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

            store.vm$.pipe(pluck('items'), take(1)).subscribe(() => {
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

        store.vm$.pipe(pluck('layoutProperties'), take(1)).subscribe(() => {
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
