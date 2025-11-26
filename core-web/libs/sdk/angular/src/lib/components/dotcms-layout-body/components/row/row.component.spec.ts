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

    it('should apply custom class', () => {
        spectator.detectChanges();
        const rowElement = spectator.query(byTestId('dotcms-row'));

        expect(rowElement?.classList.contains('custom-class')).toBe(true);
    });

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

        const columns = spectator.queryAll('dotcms-column');
        expect(columns.length).toBe(2);
    });

    it('should render row with styleClass', () => {
        spectator.setInput({
            row: {
                columns: [],
                styleClass: 'custom-class',
                identifier: 1
            }
        });
        spectator.detectChanges();
        const rowElement = spectator.query(byTestId('dotcms-row'));

        expect(rowElement?.classList.contains('custom-class')).toBe(true);
        expect(rowElement?.classList.contains('dot-row')).toBe(true);
    });
});
