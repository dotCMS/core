import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';

import { AsyncPipe, NgFor } from '@angular/common';

import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { FULL_DATA_MOCK } from './utils/mocks';

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
            TemplateBuilderBackgroundColumnsComponent
        ],
        providers: [DotTemplateBuilderStore]
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

    it('should create', () => {
        expect(spectator).toBeTruthy();
    });

    it('should have a Add Row Button', () => {
        expect(spectator.query(byTestId('add-row'))).toBeTruthy();
    });

    it('should have a Add Box Button', () => {
        expect(spectator.query(byTestId('add-box'))).toBeTruthy();
    });

    it('should have a 12 columns as background', () => {
        expect(spectator.queryAll(byTestId('column')).length).toBe(12);
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

    it('should trigger removeColumn when clicking on delete column', () => {
        const deleteColumnButton = spectator.query(byTestId('btn-trash-column'));

        const mockRemoveColumn = jest.spyOn(spectator.component, 'removeColumn');

        spectator.dispatchFakeEvent(deleteColumnButton, 'onClick');

        expect(mockRemoveColumn).toHaveBeenCalled();
    });

    it('should call removeRow from store when triggering deleteRow', () => {
        const removeRowMock = jest.spyOn(store, 'removeRow');
        spectator.component.deleteRow('123');
        expect(removeRowMock).toHaveBeenCalledWith('123');
    });
});
