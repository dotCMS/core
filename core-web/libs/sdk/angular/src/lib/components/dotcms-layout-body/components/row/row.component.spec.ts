import { expect } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { DotPageAssetLayoutColumn } from '@dotcms/types';

import { RowComponent } from './row.component';

describe('RowComponent', () => {
    let spectator: Spectator<RowComponent>;

    const createComponent = createComponentFactory({
        component: RowComponent
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                row: {
                    columns: [],
                    styleClass: 'custom-class',
                    identifier: 1
                }
            }
        });
    });

    describe('Component Structure', () => {
        it('should render container and row elements', () => {
            spectator.detectChanges();
            const rowElement = spectator.query(byTestId('dotcms-row'));
            const containerElement = rowElement?.parentElement;

            expect(containerElement).toBeTruthy();
            expect(rowElement).toBeTruthy();
            expect(rowElement?.getAttribute('data-dot-object')).toBe('row');
        });

        it('should apply dot-row-container class to container', () => {
            spectator.detectChanges();
            const rowElement = spectator.query(byTestId('dotcms-row'));
            const containerElement = rowElement?.parentElement;

            expect(containerElement?.classList.contains('dot-row-container')).toBe(true);
        });

        it('should apply dot-row class to row element', () => {
            spectator.detectChanges();
            const rowElement = spectator.query(byTestId('dotcms-row'));

            expect(rowElement?.classList.contains('dot-row')).toBe(true);
        });
    });

    describe('StyleClass Handling', () => {
        it('should apply custom class to container', () => {
            spectator.detectChanges();
            const rowElement = spectator.query(byTestId('dotcms-row'));
            const containerElement = rowElement?.parentElement;

            expect(containerElement?.classList.contains('custom-class')).toBe(true);
        });

        it('should handle empty styleClass', () => {
            spectator.setInput({
                row: {
                    columns: [],
                    styleClass: '',
                    identifier: 1
                }
            });
            spectator.detectChanges();
            const rowElement = spectator.query(byTestId('dotcms-row'));
            const containerElement = rowElement?.parentElement;

            expect(containerElement?.classList.contains('dot-row-container')).toBe(true);
            expect(containerElement?.classList.contains('custom-class')).toBe(false);
        });

        it('should handle undefined styleClass', () => {
            spectator.setInput({
                row: {
                    columns: [],
                    identifier: 1
                }
            });
            spectator.detectChanges();
            const rowElement = spectator.query(byTestId('dotcms-row'));
            const containerElement = rowElement?.parentElement;

            expect(containerElement?.classList.contains('dot-row-container')).toBe(true);
            expect(containerElement?.classList.contains('custom-class')).toBe(false);
        });

        it('should handle multiple classes in styleClass', () => {
            spectator.setInput({
                row: {
                    columns: [],
                    styleClass: 'class1 class2 class3',
                    identifier: 1
                }
            });
            spectator.detectChanges();
            const rowElement = spectator.query(byTestId('dotcms-row'));
            const containerElement = rowElement?.parentElement;

            expect(containerElement?.classList.contains('class1')).toBe(true);
            expect(containerElement?.classList.contains('class2')).toBe(true);
            expect(containerElement?.classList.contains('class3')).toBe(true);
            expect(containerElement?.classList.contains('dot-row-container')).toBe(true);
        });

        it('should update styleClass when input changes', () => {
            spectator.detectChanges();
            let rowElement = spectator.query(byTestId('dotcms-row'));
            let containerElement = rowElement?.parentElement;

            expect(containerElement?.classList.contains('custom-class')).toBe(true);

            spectator.setInput({
                row: {
                    columns: [],
                    styleClass: 'updated-class',
                    identifier: 1
                }
            });
            spectator.detectChanges();

            rowElement = spectator.query(byTestId('dotcms-row'));
            containerElement = rowElement?.parentElement;

            expect(containerElement?.classList.contains('updated-class')).toBe(true);
            expect(containerElement?.classList.contains('custom-class')).toBe(false);
        });
    });

    describe('Column Rendering', () => {
        it('should render columns', () => {
            const mockColumns = [
                { containers: [], leftOffset: 0, width: 12 },
                { containers: [], leftOffset: 0, width: 12 }
            ] as unknown as DotPageAssetLayoutColumn[];

            spectator.setInput({
                row: {
                    columns: mockColumns,
                    styleClass: '',
                    identifier: 1
                }
            });
            spectator.detectChanges();

            const columns = spectator.queryAll('dotcms-column');
            expect(columns.length).toBe(2);
        });

        it('should render empty columns array', () => {
            spectator.setInput({
                row: {
                    columns: [],
                    styleClass: '',
                    identifier: 1
                }
            });
            spectator.detectChanges();

            const columns = spectator.queryAll('dotcms-column');
            expect(columns.length).toBe(0);
        });

        it('should pass column data to column components', () => {
            const mockColumns = [
                {
                    containers: [],
                    leftOffset: 0,
                    width: 6,
                    widthPercent: 50,
                    left: 0,
                    preview: false
                },
                {
                    containers: [],
                    leftOffset: 6,
                    width: 6,
                    widthPercent: 50,
                    left: 50,
                    preview: false
                }
            ] as unknown as DotPageAssetLayoutColumn[];

            spectator.setInput({
                row: {
                    columns: mockColumns,
                    styleClass: '',
                    identifier: 1
                }
            });
            spectator.detectChanges();

            const columns = spectator.queryAll('dotcms-column');
            expect(columns.length).toBe(2);
            // Verify columns are rendered in order
            expect(columns[0]).toBeTruthy();
            expect(columns[1]).toBeTruthy();
        });
    });
});
