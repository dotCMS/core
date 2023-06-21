import { expect, it } from '@jest/globals';
import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';
import { GridItemHTMLElement } from 'gridstack';

import { AsyncPipe, NgFor } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DividerModule } from 'primeng/divider';
import { DialogService } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';
import { containersMock, DotContainersServiceMock } from '@dotcms/utils-testing';

import { DotAddStyleClassesDialogStore } from './components/add-style-classes-dialog/store/add-style-classes-dialog.store';
import { TemplateBuilderActionsComponent } from './components/template-builder-actions/template-builder-actions.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderComponentsModule } from './components/template-builder-components.module';
import { TemplateBuilderSectionComponent } from './components/template-builder-section/template-builder-section.component';
import { DotGridStackWidget } from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { parseFromDotObjectToGridStack } from './utils/gridstack-utils';
import { DOT_MESSAGE_SERVICE_TB_MOCK, FULL_DATA_MOCK } from './utils/mocks';

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
            AsyncPipe,
            TemplateBuilderBoxComponent,
            TemplateBuilderBackgroundColumnsComponent,
            TemplateBuilderSectionComponent,
            DotMessagePipeModule,
            TemplateBuilderActionsComponent,
            DotMessagePipeModule,
            HttpClientTestingModule,
            ToolbarModule,
            DividerModule,
            TemplateBuilderComponentsModule
        ],
        providers: [
            DotTemplateBuilderStore,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            },
            DialogService,
            DotAddStyleClassesDialogStore
        ]
    });
    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder [templateLayout]="templateLayout"></dotcms-template-builder>`,
            {
                hostProps: {
                    templateLayout: { body: FULL_DATA_MOCK }
                }
            }
        );

        store = spectator.inject(DotTemplateBuilderStore);
        dialog = spectator.inject(DialogService);
        openDialogMock = jest.spyOn(dialog, 'open');
    });

    it('should have a Add Row Button', () => {
        expect(spectator.query(byTestId('add-row'))).toBeTruthy();
    });

    it('should have a Add Box Button', () => {
        expect(spectator.query(byTestId('add-box'))).toBeTruthy();
    });

    it('should have a background', () => {
        expect(spectator.query('dotcms-template-builder-background-columns')).toBeTruthy();
    });

    it('should have the same quantity of rows as mocked data', () => {
        expect(spectator.queryAll(byTestId('row')).length).toBe(FULL_DATA_MOCK.rows.length);
    });

    it('should have the same quantity of boxes as mocked data', () => {
        const totalBoxes = FULL_DATA_MOCK.rows.reduce((acc, row) => {
            return acc + row.columns.length;
        }, 0);

        expect(spectator.queryAll(byTestId('box')).length).toBe(totalBoxes);
    });

    it('should trigger removeColumn on store when triggering removeColumn', () => {
        const removeColMock = jest.spyOn(store, 'removeColumn');

        let widgetToDelete: DotGridStackWidget;
        let rowId: string;
        let elementToDelete: GridItemHTMLElement;

        expect.assertions(1);

        store.state$.pipe(take(1)).subscribe(({ items }) => {
            widgetToDelete = items[0].subGridOpts.children[0];
            rowId = items[0].id as string;
            elementToDelete = document.createElement('div');

            spectator.component.removeColumn(widgetToDelete, elementToDelete, rowId);

            expect(removeColMock).toHaveBeenCalledWith({ ...widgetToDelete, parentId: rowId });
        });
    });

    it('should call removeRow from store when triggering deleteRow', () => {
        const removeRowMock = jest.spyOn(store, 'removeRow');
        spectator.component.deleteRow('123');
        expect(removeRowMock).toHaveBeenCalledWith('123');
    });

    it('should call addContainer from store when triggering addContainer', () => {
        const addContainerMock = jest.spyOn(store, 'addContainer');

        let widgetToAddContainer: DotGridStackWidget;
        let rowId: string;

        expect.assertions(1);

        store.state$.pipe(take(1)).subscribe(({ items }) => {
            widgetToAddContainer = items[0].subGridOpts.children[0];
            rowId = items[0].id as string;

            spectator.component.addContainer(widgetToAddContainer, rowId, mockContainer);

            expect(addContainerMock).toHaveBeenCalled();
        });
    });

    it('should call deleteContainer from store when triggering deleteContainer', () => {
        const deleteContainerMock = jest.spyOn(store, 'deleteContainer');

        let widgetToDeleteContainer: DotGridStackWidget;
        let rowId: string;

        expect.assertions(1);

        store.state$.pipe(take(1)).subscribe(({ items }) => {
            widgetToDeleteContainer = items[0].subGridOpts.children[0];
            rowId = items[0].id as string;

            spectator.component.deleteContainer(widgetToDeleteContainer, rowId, 0);

            expect(deleteContainerMock).toHaveBeenCalled();
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

    describe('layoutChange', () => {
        it('should emit layoutChange when the store changes', (done) => {
            const layoutChangeMock = jest.spyOn(spectator.component.layoutChange, 'emit');
            store.init(parseFromDotObjectToGridStack(FULL_DATA_MOCK));
            store.items$.pipe(take(1)).subscribe(() => {
                // const body = parseFromGridStackToDotObject(items);
                expect(layoutChangeMock).toHaveBeenCalledWith({ body: FULL_DATA_MOCK });
                done();
            });
        });
    });
});
