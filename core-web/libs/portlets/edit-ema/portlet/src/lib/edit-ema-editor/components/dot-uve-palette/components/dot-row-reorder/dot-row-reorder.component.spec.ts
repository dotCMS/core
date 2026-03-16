import { createComponentFactory, mockProvider, Spectator, byTestId } from '@ngneat/spectator/jest';

import { CdkDrag, CdkDragDrop } from '@angular/cdk/drag-drop';
import { computed, signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { DotPageAssetLayoutRow, DotPageAssetLayoutColumn, DotCMSLayout } from '@dotcms/types';

import { DotRowReorderComponent } from './dot-row-reorder.component';

import { UVEStore } from '../../../../../store/dot-uve.store';

const MOCK_COLUMNS: DotPageAssetLayoutColumn[] = [
    {
        preview: false,
        containers: [{ identifier: 'container-1', uuid: 'uuid-1', historyUUIDs: [] }],
        widthPercent: 50,
        width: 6,
        leftOffset: 1,
        left: 0,
        styleClass: 'column-1'
    },
    {
        preview: false,
        containers: [{ identifier: 'container-2', uuid: 'uuid-2', historyUUIDs: [] }],
        widthPercent: 50,
        width: 6,
        leftOffset: 7,
        left: 6
    }
];

const MOCK_ROWS: DotPageAssetLayoutRow[] = [
    {
        identifier: 1,
        columns: MOCK_COLUMNS,
        styleClass: 'row-1'
    },
    {
        identifier: 2,
        columns: [
            {
                preview: false,
                containers: [{ identifier: 'container-3', uuid: 'uuid-3', historyUUIDs: [] }],
                widthPercent: 100,
                width: 12,
                leftOffset: 1,
                left: 0
            }
        ]
    }
];

const MOCK_LAYOUT: DotCMSLayout = {
    pageWidth: '100%',
    width: '100%',
    layout: 'test-layout',
    title: 'Test Layout',
    header: false,
    footer: false,
    sidebar: {
        preview: false,
        containers: [],
        location: '',
        widthPercent: 0,
        width: '0'
    },
    body: {
        rows: MOCK_ROWS
    }
};

describe('DotRowReorderComponent', () => {
    let spectator: Spectator<DotRowReorderComponent>;
    let component: DotRowReorderComponent;
    let mockUVEStore: InstanceType<typeof UVEStore>;
    let mockLayoutSignal: ReturnType<typeof signal<DotCMSLayout | null>>;

    const createComponent = createComponentFactory({
        component: DotRowReorderComponent,
        providers: [
            mockProvider(UVEStore, {
                pageAsset: signal(null),
                updateLayout: jest.fn(),
                updateRows: jest.fn()
            })
        ]
    });

    beforeEach(() => {
        mockLayoutSignal = signal<DotCMSLayout | null>(MOCK_LAYOUT);
        spectator = createComponent({
            providers: [
                {
                    provide: UVEStore,
                    useValue: {
                        pageAsset: computed(() => {
                            const layout = mockLayoutSignal();
                            return layout ? { layout } : null;
                        }),
                        updateLayout: jest.fn(),
                        updateRows: jest.fn()
                    }
                }
            ]
        });
        component = spectator.component;
        mockUVEStore = spectator.inject(UVEStore, true) as InstanceType<typeof UVEStore> & {
            updateLayout: jest.Mock;
            updateRows: jest.Mock;
        };
        spectator.detectChanges();
    });

    describe('Rendering', () => {
        it('should render empty state when no rows', () => {
            mockLayoutSignal.set({ ...MOCK_LAYOUT, body: { rows: [] } });
            spectator.detectChanges();

            const emptyState = spectator.query(byTestId('empty-state'));
            expect(emptyState).toBeTruthy();
            expect(emptyState).toHaveText('No rows available');
        });

        it('should render rows when layout has rows', () => {
            expect(spectator.queryAll(byTestId('row-item')).length).toBe(2);
        });

        it('should render row labels correctly', () => {
            const rowLabels = spectator.queryAll(byTestId('row-label'));
            expect(rowLabels[0]).toHaveText('row-1');
            expect(rowLabels[1]).toHaveText('Row 2');
        });

        it('should render column labels correctly', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            const columnLabels = spectator.queryAll(byTestId('column-label'));
            expect(columnLabels[0]).toHaveText('column-1');
            expect(columnLabels[1]).toHaveText('Column 2');
        });

        it('should not render columns when row is collapsed', () => {
            expect(spectator.queryAll(byTestId('column-label')).length).toBe(0);
        });

        it('should render columns when row is expanded', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('column-label')).length).toBe(2);
        });
    });

    describe('Row Labels', () => {
        it('should return styleClass when available', () => {
            expect(component.getRowLabel(MOCK_ROWS[0], 0)).toBe('row-1');
        });

        it('should return default label when styleClass is not available', () => {
            expect(component.getRowLabel(MOCK_ROWS[1], 1)).toBe('Row 2');
        });
    });

    describe('Column Labels', () => {
        it('should return styleClass when available', () => {
            expect(component.getColumnLabel(MOCK_COLUMNS[0], 0)).toBe('column-1');
        });

        it('should return default label when styleClass is not available', () => {
            expect(component.getColumnLabel(MOCK_COLUMNS[1], 1)).toBe('Column 2');
        });
    });

    describe('Row Selection', () => {
        it('should emit onRowSelect when row label is clicked', () => {
            const onRowSelectSpy = jest.spyOn(component.onRowSelect, 'emit');
            spectator.click(spectator.queryAll(byTestId('row-label'))[0]);
            spectator.detectChanges();

            expect(onRowSelectSpy).toHaveBeenCalledWith({ selector: '#section-1', type: 'row' });
        });
    });

    describe('Row Expansion', () => {
        it('should expand row when toggle button is clicked', () => {
            const toggleButton = spectator.queryAll(byTestId('row-toggle'))[0];
            expect(toggleButton.getAttribute('aria-expanded')).toBe('false');

            spectator.click(toggleButton);
            spectator.detectChanges();

            expect(toggleButton.getAttribute('aria-expanded')).toBe('true');
        });

        it('should collapse row when toggle button is clicked again', () => {
            const toggleButton = spectator.queryAll(byTestId('row-toggle'))[0];

            spectator.click(toggleButton);
            spectator.detectChanges();
            expect(toggleButton.getAttribute('aria-expanded')).toBe('true');

            spectator.click(toggleButton);
            spectator.detectChanges();
            expect(toggleButton.getAttribute('aria-expanded')).toBe('false');
        });

        it('should show chevron-down icon when row is expanded', () => {
            const toggleButton = spectator.queryAll(byTestId('row-toggle'))[0];
            spectator.click(toggleButton);
            spectator.detectChanges();

            expect(toggleButton.querySelector('.pi-chevron-down')).toBeTruthy();
        });

        it('should show chevron-right icon when row is collapsed', () => {
            const toggleButton = spectator.queryAll(byTestId('row-toggle'))[0];
            expect(toggleButton.querySelector('.pi-chevron-right')).toBeTruthy();
        });
    });

    describe('Column Dragging State', () => {
        it('should disable row dragging when column drag starts', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            const rowDrag = spectator.fixture.debugElement
                .query(By.css('[data-testid="row-item"]'))
                ?.injector.get(CdkDrag);
            expect(rowDrag?.disabled).toBe(false);

            spectator.triggerEventHandler('[cdkDrag].row-column', 'cdkDragStarted', {});
            spectator.detectChanges();

            expect(rowDrag?.disabled).toBe(true);
        });

        it('should re-enable row dragging when column drag ends', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            const rowDrag = spectator.fixture.debugElement
                .query(By.css('[data-testid="row-item"]'))
                ?.injector.get(CdkDrag);

            spectator.triggerEventHandler('[cdkDrag].row-column', 'cdkDragStarted', {});
            spectator.detectChanges();
            expect(rowDrag?.disabled).toBe(true);

            spectator.triggerEventHandler('[cdkDrag].row-column', 'cdkDragEnded', {});
            spectator.detectChanges();
            expect(rowDrag?.disabled).toBe(false);
        });
    });

    describe('Edit Row Dialog', () => {
        it('should open edit dialog when row label is double-clicked', () => {
            expect(component.editRowDialogOpen()).toBe(false);

            spectator
                .queryAll(byTestId('row-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect(component.editRowDialogOpen()).toBe(true);
        });

        it('should set form control value when opening edit dialog', () => {
            spectator
                .queryAll(byTestId('row-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect(component.rowStyleClassControl.value).toBe('row-1');
        });

        it('should close dialog when onHide fires', () => {
            spectator
                .queryAll(byTestId('row-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();
            expect(component.editRowDialogOpen()).toBe(true);

            spectator.triggerEventHandler('p-dialog', 'onHide', undefined);
            spectator.detectChanges();

            expect(component.editRowDialogOpen()).toBe(false);
        });

        it('should track row editing when row label is double-clicked', () => {
            spectator
                .queryAll(byTestId('row-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect(spectator.query('p-dialog')).toBeTruthy();
            expect(component.editingColumn()).toBeNull();
        });

        it('should track column editing when column label is double-clicked', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            spectator
                .queryAll(byTestId('column-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect(spectator.query('p-dialog')).toBeTruthy();
            expect(component.editingColumn()).toEqual({ rowIndex: 0, columnIndex: 0 });
        });
    });

    describe('Edit Column Dialog', () => {
        it('should open edit dialog when column label is double-clicked', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            expect(component.editRowDialogOpen()).toBe(false);

            spectator
                .queryAll(byTestId('column-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect(component.editRowDialogOpen()).toBe(true);
        });

        it('should set form control value when opening edit column dialog', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            spectator
                .queryAll(byTestId('column-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect(component.rowStyleClassControl.value).toBe('column-1');
        });
    });

    describe('Submit Edit Row', () => {
        it('should update row styleClass and close dialog', () => {
            spectator
                .queryAll(byTestId('row-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            component.rowStyleClassControl.setValue('updated-row-1');
            spectator.click(spectator.query<HTMLButtonElement>('button[type="submit"]')!);
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
            expect(component.editRowDialogOpen()).toBe(false);
        });

        it('should update column styleClass and close dialog', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            spectator
                .queryAll(byTestId('column-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            component.rowStyleClassControl.setValue('updated-column-1');
            spectator.click(spectator.query<HTMLButtonElement>('button[type="submit"]')!);
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
            expect(component.editRowDialogOpen()).toBe(false);
        });

        it('should remove styleClass when value is empty', () => {
            spectator
                .queryAll(byTestId('row-label'))[0]
                .dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            component.rowStyleClassControl.setValue('   ');
            spectator.click(spectator.query<HTMLButtonElement>('button[type="submit"]')!);
            spectator.detectChanges();

            expect(mockUVEStore.updateRows).toHaveBeenCalled();
        });
    });

    describe('Row Drag and Drop', () => {
        it('should update rows order when dropped', () => {
            const dropEvent = {
                previousIndex: 0,
                currentIndex: 1,
                container: { data: MOCK_ROWS },
                previousContainer: { data: MOCK_ROWS }
            } as CdkDragDrop<DotPageAssetLayoutRow[]>;

            spectator.triggerEventHandler('[cdkDropList]', 'cdkDropListDropped', dropEvent);
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
        });
    });

    describe('Column Drag and Drop', () => {
        it('should update column order when dropped within same row', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            const targetRow = component.rows()[0];
            const container = { data: targetRow.columns };
            const dropEvent = {
                previousIndex: 0,
                currentIndex: 1,
                container,
                previousContainer: container
            } as CdkDragDrop<DotPageAssetLayoutColumn[]>;

            spectator.triggerEventHandler(
                '[data-testid="row-columns"]',
                'cdkDropListDropped',
                dropEvent
            );
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
        });

        it('should not update if dropped in different container', () => {
            spectator.click(spectator.queryAll(byTestId('row-toggle'))[0]);
            spectator.detectChanges();

            const targetRow = component.rows()[0];
            const dropEvent = {
                previousIndex: 0,
                currentIndex: 1,
                container: { data: [] },
                previousContainer: { data: targetRow.columns }
            } as CdkDragDrop<DotPageAssetLayoutColumn[]>;

            const updateRowsSpy = jest.spyOn(mockUVEStore, 'updateRows');
            spectator.triggerEventHandler(
                '[data-testid="row-columns"]',
                'cdkDropListDropped',
                dropEvent
            );
            spectator.detectChanges();

            expect(updateRowsSpy).not.toHaveBeenCalled();
        });
    });

    describe('Computed Rows', () => {
        it('should return rows from layout body', () => {
            const rows = component.rows();
            expect(rows.length).toBe(2);
            expect(rows[0].identifier).toBe(1);
            expect(rows[1].identifier).toBe(2);
        });

        it('should return empty array when layout is null', () => {
            mockLayoutSignal.set(null);
            spectator.detectChanges();

            expect(component.rows().length).toBe(0);
        });

        it('should return empty array when layout body is null', () => {
            mockLayoutSignal.set({
                ...MOCK_LAYOUT,
                body: null as unknown as typeof MOCK_LAYOUT.body
            });
            spectator.detectChanges();

            expect(component.rows().length).toBe(0);
        });
    });
});
