import { expect } from '@jest/globals';
import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';
import { GridItemHTMLElement } from 'gridstack';

import { AsyncPipe, NgFor } from '@angular/common';

import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { TemplateBuilderComponentsModule } from './components/template-builder-components.module';
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

    const createHost = createHostFactory({
        component: TemplateBuilderComponent,
        imports: [
            NgFor,
            AsyncPipe,
            DotMessagePipeModule,
            ToolbarModule,
            DividerModule,
            TemplateBuilderComponentsModule
        ],
        providers: [
            DotTemplateBuilderStore,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            }
        ]
    });
    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder [templateLayout]="templateLayout"></dotcms-template-builder>`,
            {
                hostProps: {
                    templateLayout: {
                        body: FULL_DATA_MOCK,
                        header: true,
                        footer: true,
                        sidebar: {
                            position: 'left',
                            width: 'small',
                            containers: []
                        }
                    }
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

    describe('layoutChange', () => {
        it('should emit layoutChange when the store changes', (done) => {
            const layoutChangeMock = jest.spyOn(spectator.component.layoutChange, 'emit');
            store.init({
                items: parseFromDotObjectToGridStack(FULL_DATA_MOCK),
                layoutProperties: {
                    header: true,
                    footer: true,
                    sidebar: {}
                }
            });
            store.items$.pipe(take(1)).subscribe(() => {
                // const body = parseFromGridStackToDotObject(items);
                expect(layoutChangeMock).toHaveBeenCalledWith({
                    body: FULL_DATA_MOCK,
                    header: true,
                    footer: true,
                    sidebar: {
                        position: 'left',
                        width: 'small',
                        containers: []
                    }
                });
                done();
            });
        });
    });
});
