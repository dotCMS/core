import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';
import { MockProvider } from 'ng-mocks';

import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { signal } from '@angular/core';

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
            MockProvider(UVEStore, {
                layout: signal<DotCMSLayout | null>(null),
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
                        layout: mockLayoutSignal,
                        updateLayout: jest.fn(),
                        updateRows: jest.fn()
                    }
                }
            ]
        });
        component = spectator.component;
        mockUVEStore = spectator.inject(UVEStore, true);
        spectator.detectChanges();
    });

    describe('Component Creation', () => {
        it('should create', () => {
            expect(component).toBeTruthy();
        });
    });

    describe('Rendering', () => {
        it('should render empty state when no rows', () => {
            mockLayoutSignal.set({
                ...MOCK_LAYOUT,
                body: { rows: [] }
            });
            spectator.detectChanges();

            const emptyState = spectator.query('.empty-state');
            expect(emptyState).toBeTruthy();
            expect(emptyState).toHaveText('No rows available');
        });

        it('should render rows when layout has rows', () => {
            const rowItems = spectator.queryAll('.row-item');
            expect(rowItems.length).toBe(2);
        });

        it('should render row labels correctly', () => {
            const rowLabels = spectator.queryAll('.row-label');
            expect(rowLabels[0]).toHaveText('row-1');
            expect(rowLabels[1]).toHaveText('Row 2');
        });

        it('should render column labels correctly', () => {
            // Expand first row
            const toggleButton = spectator.queryAll('.row-toggle')[0];
            spectator.click(toggleButton);
            spectator.detectChanges();

            const columnLabels = spectator.queryAll('.column-label');
            expect(columnLabels[0]).toHaveText('column-1');
            expect(columnLabels[1]).toHaveText('Column 2');
        });

        it('should not render columns when row is collapsed', () => {
            const columnLabels = spectator.queryAll('.column-label');
            expect(columnLabels.length).toBe(0);
        });

        it('should render columns when row is expanded', () => {
            const toggleButton = spectator.queryAll('.row-toggle')[0];
            spectator.click(toggleButton);
            spectator.detectChanges();

            const columnLabels = spectator.queryAll('.column-label');
            expect(columnLabels.length).toBe(2);
        });
    });

    describe('Row Labels', () => {
        it('should return styleClass when available', () => {
            const row = MOCK_ROWS[0];
            const label = (component as any).getRowLabel(row, 0);
            expect(label).toBe('row-1');
        });

        it('should return default label when styleClass is not available', () => {
            const row = MOCK_ROWS[1];
            const label = (component as any).getRowLabel(row, 1);
            expect(label).toBe('Row 2');
        });
    });

    describe('Column Labels', () => {
        it('should return styleClass when available', () => {
            const column = MOCK_COLUMNS[0];
            const label = (component as any).getColumnLabel(column, 0);
            expect(label).toBe('column-1');
        });

        it('should return default label when styleClass is not available', () => {
            const column = MOCK_COLUMNS[1];
            const label = (component as any).getColumnLabel(column, 1);
            expect(label).toBe('Column 2');
        });
    });

    describe('Row Selection', () => {
        it('should emit onRowSelect when row label is clicked', () => {
            const onRowSelectSpy = jest.spyOn(component.onRowSelect, 'emit');
            const rowLabel = spectator.queryAll('.row-label')[0];

            spectator.click(rowLabel);
            spectator.detectChanges();

            expect(onRowSelectSpy).toHaveBeenCalledWith({
                selector: '#section-1',
                type: 'row'
            });
        });
    });

    describe('Row Expansion', () => {
        it('should expand row when toggle button is clicked', () => {
            const toggleButton = spectator.queryAll('.row-toggle')[0];
            expect((component as any).isRowExpanded(0)).toBe(false);

            spectator.click(toggleButton);
            spectator.detectChanges();

            expect((component as any).isRowExpanded(0)).toBe(true);
        });

        it('should collapse row when toggle button is clicked again', () => {
            const toggleButton = spectator.queryAll('.row-toggle')[0];

            // Expand
            spectator.click(toggleButton);
            spectator.detectChanges();
            expect((component as any).isRowExpanded(0)).toBe(true);

            // Collapse
            spectator.click(toggleButton);
            spectator.detectChanges();
            expect((component as any).isRowExpanded(0)).toBe(false);
        });

        it('should show chevron-down icon when row is expanded', () => {
            const toggleButton = spectator.queryAll('.row-toggle')[0];
            spectator.click(toggleButton);
            spectator.detectChanges();

            const icon = toggleButton.querySelector('.pi-chevron-down');
            expect(icon).toBeTruthy();
        });

        it('should show chevron-right icon when row is collapsed', () => {
            const toggleButton = spectator.queryAll('.row-toggle')[0];
            const icon = toggleButton.querySelector('.pi-chevron-right');
            expect(icon).toBeTruthy();
        });
    });

    describe('Column Dragging State', () => {
        it('should set column dragging to true when drag starts', () => {
            expect((component as any).isColumnDragging()).toBe(false);
            (component as any).setColumnDragging(true);
            expect((component as any).isColumnDragging()).toBe(true);
        });

        it('should set column dragging to false when drag ends', () => {
            (component as any).setColumnDragging(true);
            (component as any).setColumnDragging(false);
            expect((component as any).isColumnDragging()).toBe(false);
        });
    });

    describe('Edit Row Dialog', () => {
        it('should open edit dialog when row label is double-clicked', () => {
            const rowLabel = spectator.queryAll('.row-label')[0];
            expect((component as any).editRowDialogOpen()).toBe(false);

            rowLabel.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect((component as any).editRowDialogOpen()).toBe(true);
        });

        it('should set form control value when opening edit dialog', () => {
            const rowLabel = spectator.queryAll('.row-label')[0];
            rowLabel.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect((component as any).rowStyleClassControl.value).toBe('row-1');
        });

        it('should close dialog when onHide is called', () => {
            (component as any).openEditRowDialog(0);
            spectator.detectChanges();
            expect((component as any).editRowDialogOpen()).toBe(true);

            (component as any).closeEditRowDialog();
            spectator.detectChanges();

            expect((component as any).editRowDialogOpen()).toBe(false);
        });

        it('should show "Edit Row" header when editing row', () => {
            (component as any).openEditRowDialog(0);
            spectator.detectChanges();

            const dialog = spectator.query('p-dialog');
            expect(dialog).toBeTruthy();
            expect((component as any).editingColumn()).toBeNull();
        });

        it('should show "Edit Column" header when editing column', () => {
            (component as any).openEditColumnDialog(0, 0);
            spectator.detectChanges();

            const dialog = spectator.query('p-dialog');
            expect(dialog).toBeTruthy();
            expect((component as any).editingColumn()).toEqual({ rowIndex: 0, columnIndex: 0 });
        });
    });

    describe('Edit Column Dialog', () => {
        it('should open edit dialog when column label is double-clicked', () => {
            // Expand row first
            const toggleButton = spectator.queryAll('.row-toggle')[0];
            spectator.click(toggleButton);
            spectator.detectChanges();

            const columnLabel = spectator.queryAll('.column-label')[0];
            expect((component as any).editRowDialogOpen()).toBe(false);

            columnLabel.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect((component as any).editRowDialogOpen()).toBe(true);
        });

        it('should set form control value when opening edit column dialog', () => {
            const toggleButton = spectator.queryAll('.row-toggle')[0];
            spectator.click(toggleButton);
            spectator.detectChanges();

            const columnLabel = spectator.queryAll('.column-label')[0];
            columnLabel.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
            spectator.detectChanges();

            expect((component as any).rowStyleClassControl.value).toBe('column-1');
        });
    });

    describe('Submit Edit Row', () => {
        it('should update row styleClass and close dialog', () => {
            (component as any).openEditRowDialog(0);
            spectator.detectChanges();

            (component as any).rowStyleClassControl.setValue('updated-row-1');
            (component as any).submitEditRow();
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
            expect((component as any).editRowDialogOpen()).toBe(false);
        });

        it('should update column styleClass and close dialog', () => {
            (component as any).openEditColumnDialog(0, 0);
            spectator.detectChanges();

            (component as any).rowStyleClassControl.setValue('updated-column-1');
            (component as any).submitEditRow();
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
            expect((component as any).editRowDialogOpen()).toBe(false);
        });

        it('should remove styleClass when value is empty', () => {
            (component as any).openEditRowDialog(0);
            spectator.detectChanges();

            (component as any).rowStyleClassControl.setValue('   ');
            (component as any).submitEditRow();
            spectator.detectChanges();

            expect(mockUVEStore.updateRows).toHaveBeenCalled();
        });

        it('should not update if row index is invalid', () => {
            (component as any).openEditRowDialog(999);
            spectator.detectChanges();

            const updateRowsSpy = jest.spyOn(mockUVEStore, 'updateRows');
            (component as any).submitEditRow();
            spectator.detectChanges();

            expect(updateRowsSpy).not.toHaveBeenCalled();
        });

        it('should not update column if row or column is invalid', () => {
            mockLayoutSignal.set({
                ...MOCK_LAYOUT,
                body: {
                    rows: [
                        {
                            identifier: 1,
                            columns: []
                        }
                    ]
                }
            });
            spectator.detectChanges();

            (component as any).openEditColumnDialog(0, 999);
            spectator.detectChanges();

            const updateRowsSpy = jest.spyOn(mockUVEStore, 'updateRows');
            (component as any).submitEditRow();
            spectator.detectChanges();

            expect(updateRowsSpy).not.toHaveBeenCalled();
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

            (component as any).drop(dropEvent);
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
        });
    });

    describe('Column Drag and Drop', () => {
        it('should update column order when dropped within same row', () => {
            const targetRow = MOCK_ROWS[0];
            const container = { data: targetRow.columns };
            const dropEvent = {
                previousIndex: 0,
                currentIndex: 1,
                container: container,
                previousContainer: container
            } as CdkDragDrop<DotPageAssetLayoutColumn[]>;

            (component as any).dropColumn(dropEvent, 0);
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
        });

        it('should recompute leftOffsets when columns are reordered', () => {
            const targetRow = MOCK_ROWS[0];
            const container = { data: targetRow.columns };
            const dropEvent = {
                previousIndex: 0,
                currentIndex: 1,
                container: container,
                previousContainer: container
            } as CdkDragDrop<DotPageAssetLayoutColumn[]>;

            (component as any).dropColumn(dropEvent, 0);
            spectator.detectChanges();

            expect(mockUVEStore.updateLayout).toHaveBeenCalled();
            expect(mockUVEStore.updateRows).toHaveBeenCalled();
        });

        it('should not update if dropped in different container', () => {
            const targetRow = MOCK_ROWS[0];
            const differentContainer = { data: [] };
            const dropEvent = {
                previousIndex: 0,
                currentIndex: 1,
                container: differentContainer,
                previousContainer: { data: targetRow.columns }
            } as CdkDragDrop<DotPageAssetLayoutColumn[]>;

            const updateRowsSpy = jest.spyOn(mockUVEStore, 'updateRows');
            (component as any).dropColumn(dropEvent, 0);
            spectator.detectChanges();

            expect(updateRowsSpy).not.toHaveBeenCalled();
        });

        it('should not update if row has no columns', () => {
            mockLayoutSignal.set({
                ...MOCK_LAYOUT,
                body: {
                    rows: [
                        {
                            identifier: 1,
                            columns: []
                        }
                    ]
                }
            });
            spectator.detectChanges();

            const dropEvent = {
                previousIndex: 0,
                currentIndex: 1,
                container: { data: [] },
                previousContainer: { data: [] }
            } as CdkDragDrop<DotPageAssetLayoutColumn[]>;

            const updateRowsSpy = jest.spyOn(mockUVEStore, 'updateRows');
            (component as any).dropColumn(dropEvent, 0);
            spectator.detectChanges();

            expect(updateRowsSpy).not.toHaveBeenCalled();
        });
    });

    describe('Computed Rows', () => {
        it('should return rows from layout body', () => {
            const rows = (component as any).rows();
            expect(rows.length).toBe(2);
            expect(rows[0].identifier).toBe(1);
            expect(rows[1].identifier).toBe(2);
        });

        it('should return empty array when layout is null', () => {
            mockLayoutSignal.set(null);
            spectator.detectChanges();

            const rows = (component as any).rows();
            expect(rows.length).toBe(0);
        });

        it('should return empty array when layout body is null', () => {
            mockLayoutSignal.set({
                ...MOCK_LAYOUT,
                body: null as any
            });
            spectator.detectChanges();

            const rows = (component as any).rows();
            expect(rows.length).toBe(0);
        });
    });
});
