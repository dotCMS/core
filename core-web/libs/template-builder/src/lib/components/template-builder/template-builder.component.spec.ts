import { expect } from '@jest/globals';
import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';
import { GridItemHTMLElement } from 'gridstack';

import { AsyncPipe, NgFor } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DividerModule } from 'primeng/divider';
import { DialogService } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { TemplateBuilderActionsComponent } from './components/template-builder-actions/template-builder-actions.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { TemplateBuilderSectionComponent } from './components/template-builder-section/template-builder-section.component';
import { DotGridStackWidget } from './models/models';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { DOT_MESSAGE_SERVICE_TB_MOCK, FULL_DATA_MOCK } from './utils/mocks';

describe('TemplateBuilderComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderComponent>;
    let store: DotTemplateBuilderStore;

    const createHost = createHostFactory({
        component: TemplateBuilderComponent,
        imports: [
            NgFor,
            AsyncPipe,
            TemplateBuilderRowComponent,
            AddWidgetComponent,
            TemplateBuilderBoxComponent,
            TemplateBuilderBackgroundColumnsComponent,
            DotMessagePipeModule,
            HttpClientTestingModule,
            TemplateBuilderSectionComponent,
            TemplateBuilderActionsComponent,
            ToolbarModule,
            DividerModule
        ],
        providers: [
            DotTemplateBuilderStore,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            DialogService
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

    it('should call editRowStyleClasses from store when clicking on row-style-class-button', () => {
        const editRowStyleClassesMock = jest.spyOn(spectator.component, 'editRowStyleClasses');

        const editRowStyleClassesButton = spectator.query(byTestId('row-style-class-button'));

        spectator.dispatchFakeEvent(editRowStyleClassesButton, 'onClick');

        expect(editRowStyleClassesMock).toHaveBeenCalled();
    });

    it('should call editBoxStyleClasses from store when clicking on btn-palette', () => {
        const editBoxStyleClassesMock = jest.spyOn(spectator.component, 'editBoxStyleClasses');

        const editBoxStyleClassesButton = spectator.query(byTestId('btn-palette'));

        spectator.dispatchFakeEvent(editBoxStyleClassesButton, 'onClick');

        expect(editBoxStyleClassesMock).toHaveBeenCalled();
    });
});
