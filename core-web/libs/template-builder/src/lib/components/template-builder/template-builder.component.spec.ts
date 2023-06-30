import { expect, it } from '@jest/globals';
import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';
import { GridItemHTMLElement } from 'gridstack';

import { AsyncPipe, NgClass, NgFor, NgIf, NgStyle } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DividerModule } from 'primeng/divider';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';

import { pluck, take } from 'rxjs/operators';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';
import { containersMock, DotContainersServiceMock } from '@dotcms/utils-testing';

import { DotAddStyleClassesDialogStore } from './components/add-style-classes-dialog/store/add-style-classes-dialog.store';
import { TemplateBuilderComponentsModule } from './components/template-builder-components.module';
import { DotGridStackWidget } from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { parseFromDotObjectToGridStack } from './utils/gridstack-utils';
import { CONTAINER_MAP_MOCK, DOT_MESSAGE_SERVICE_TB_MOCK, FULL_DATA_MOCK } from './utils/mocks';

global.structuredClone = jest.fn((val) => {
    return JSON.parse(JSON.stringify(val));
});

describe('TemplateBuilderComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderComponent>;
    let store: DotTemplateBuilderStore;
    const mockContainer = containersMock[0];
    let dialog: DialogService;
    let openDialogMock: jest.SpyInstance;

    const createHost = createHostFactory({
        component: TemplateBuilderComponent,
        imports: [
            NgFor,
            NgIf,
            AsyncPipe,
            DotMessagePipeModule,
            DynamicDialogModule,
            NgStyle,
            NgClass,
            ToolbarModule,
            DividerModule,
            TemplateBuilderComponentsModule,
            HttpClientTestingModule
        ],
        providers: [
            DotTemplateBuilderStore,
            DialogService,
            DynamicDialogRef,
            DotAddStyleClassesDialogStore,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            }
        ]
    });
    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder [containerMap]="containerMap" [layout]="layout" [themeId]="themeId" ></dotcms-template-builder>`,
            {
                hostProps: {
                    layout: {
                        body: FULL_DATA_MOCK,
                        header: true,
                        footer: true,
                        sidebar: {
                            location: 'left',
                            width: 'small',
                            containers: []
                        },
                        width: 'Mobile',
                        title: 'Test Title'
                    },
                    themeId: '123',
                    containerMap: CONTAINER_MAP_MOCK
                }
            }
        );

        store = spectator.inject(DotTemplateBuilderStore);
        dialog = spectator.inject(DialogService);
        openDialogMock = jest.spyOn(dialog, 'open');
    });

    it('should have a Add Row Button', () => {
        expect.assertions(1);
        expect(spectator.query(byTestId('add-row'))).toBeTruthy();
    });

    it('should have a Add Box Button', () => {
        expect.assertions(1);
        expect(spectator.query(byTestId('add-box'))).toBeTruthy();
    });

    it('should have the same quantity of rows as mocked data', () => {
        expect.assertions(1);
        expect(spectator.queryAll(byTestId('row')).length).toBe(FULL_DATA_MOCK.rows.length);
    });

    it('should have the same quantity of boxes as mocked data', () => {
        expect.assertions(1);
        const totalBoxes = FULL_DATA_MOCK.rows.reduce((acc, row) => {
            return acc + row.columns.length;
        }, 0);

        expect(spectator.queryAll(byTestId('box')).length).toBe(totalBoxes);
    });

    it('should trigger removeColumn on store when triggering removeColumn', () => {
        expect.assertions(1);
        const removeColMock = jest.spyOn(store, 'removeColumn');

        let widgetToDelete: DotGridStackWidget;
        let rowId: string;
        let elementToDelete: GridItemHTMLElement;

        store.state$.pipe(take(1)).subscribe(({ items }) => {
            widgetToDelete = items[0].subGridOpts.children[0];
            rowId = items[0].id as string;
            elementToDelete = document.createElement('div');

            spectator.component.removeColumn(widgetToDelete, elementToDelete, rowId);

            expect(removeColMock).toHaveBeenCalledWith({ ...widgetToDelete, parentId: rowId });
        });
    });

    it('should call addContainer from store when triggering addContainer', () => {
        expect.assertions(1);
        const addContainerMock = jest.spyOn(store, 'addContainer');

        let widgetToAddContainer: DotGridStackWidget;
        let rowId: string;

        store.state$.pipe(take(1)).subscribe(({ items }) => {
            widgetToAddContainer = items[0].subGridOpts.children[0];
            rowId = items[0].id as string;

            spectator.component.addContainer(widgetToAddContainer, rowId, mockContainer);

            expect(addContainerMock).toHaveBeenCalled();
        });
    });

    it('should call deleteContainer from store when triggering deleteContainer', () => {
        expect.assertions(1);
        const deleteContainerMock = jest.spyOn(store, 'deleteContainer');

        let widgetToDeleteContainer: DotGridStackWidget;
        let rowId: string;

        store.state$.pipe(take(1)).subscribe(({ items }) => {
            widgetToDeleteContainer = items[0].subGridOpts.children[0];
            rowId = items[0].id as string;

            spectator.component.deleteContainer(widgetToDeleteContainer, rowId, 0);

            expect(deleteContainerMock).toHaveBeenCalled();
        });
    });

    it('should open a dialog when clicking on row-style-class-button ', () => {
        expect.assertions(1);
        const editRowStyleClassesButton = spectator.query(byTestId('row-style-class-button'));

        spectator.dispatchFakeEvent(editRowStyleClassesButton, 'onClick');

        expect(openDialogMock).toHaveBeenCalled();
    });

    it('should open a dialog when clicking on box-style-class-button', () => {
        expect.assertions(1);
        const editBoxStyleClassesButton = spectator.query(byTestId('box-style-class-button'));

        spectator.dispatchFakeEvent(editBoxStyleClassesButton, 'onClick');

        expect(openDialogMock).toHaveBeenCalled();
    });

    it('should open a panel when clicking on Layout button', () => {
        expect.assertions(1);
        const actionsButton = spectator.query(byTestId('btn-select-layout'));

        spectator.click(actionsButton);

        expect(spectator.query(byTestId('template-layout-properties-panel'))).toBeTruthy();
    });

    describe('layoutChange', () => {
        it('should emit layoutChange when the store changes', (done) => {
            const layoutChangeMock = jest.spyOn(spectator.component.templateChange, 'emit');

            spectator.detectChanges();

            expect.assertions(2);
            store.init({
                items: parseFromDotObjectToGridStack(FULL_DATA_MOCK),
                layoutProperties: {
                    header: true,
                    footer: true,
                    sidebar: {
                        containers: [],
                        location: 'left',
                        width: 'small'
                    }
                },
                resizingRowID: '',
                containerMap: {}
            });

            store.vm$.pipe(pluck('items'), take(1)).subscribe(() => {
                expect(true).toBeTruthy();
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
});
