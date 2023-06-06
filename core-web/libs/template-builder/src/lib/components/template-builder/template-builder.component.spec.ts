import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';

import { AsyncPipe, NgFor } from '@angular/common';

import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { FULL_DATA_MOCK } from './utils/mocks';

describe('TemplateBuilderComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderComponent,
        imports: [
            NgFor,
            AsyncPipe,
            TemplateBuilderRowComponent,
            AddWidgetComponent,
            TemplateBuilderBoxComponent
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

    it('should call deleteRow', () => {
        const deleteRowMock = jest.spyOn(spectator.component, 'deleteRow');
        spectator.component.deleteRow('123');
        expect(deleteRowMock).toHaveBeenCalledWith('123');
    });
});
