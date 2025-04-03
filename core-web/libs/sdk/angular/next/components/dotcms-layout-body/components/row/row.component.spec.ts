import { expect } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

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
        ];

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
});
