import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator';

import { Table, TableModule } from 'primeng/table';

import { DotStateRestoreDirective } from '@dotcms/ui';

describe('DotStateRestoreDirective', () => {
    let spectator: SpectatorDirective<DotStateRestoreDirective>;

    const createDirective = createDirectiveFactory({
        directive: DotStateRestoreDirective,
        template: `<p-table stateStorage="session" stateKey="test-key"  dotStateRestore></p-table>`,
        imports: [TableModule]
    });

    const savedState = { sortField: 'name', sortOrder: 1 };

    it('should apply the saved state from localStorage', () => {
        spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(savedState));
        spectator = createDirective(
            `<p-table stateStorage="local" stateKey="test-key"  dotStateRestore></p-table>`
        );

        const table = spectator.query(Table);

        expect(table.sortField).toBe(savedState.sortField);
        expect(table.sortOrder).toBe(savedState.sortOrder);
    });

    it('should apply the saved state from sessionStorage', () => {
        spyOn(sessionStorage, 'getItem').and.returnValue(JSON.stringify(savedState));

        spectator = createDirective();

        const table = spectator.query(Table);

        expect(table.sortField).toBe(savedState.sortField);
        expect(table.sortOrder).toBe(savedState.sortOrder);
    });

    it('should not apply state if no state is saved', () => {
        spectator = createDirective(
            `<p-table stateStorage="session" stateKey="no-key"  dotStateRestore></p-table>`
        );

        const table = spectator.query(Table);

        expect(table.sortField).toEqual(undefined);
        expect(table.sortOrder).toEqual(1); //default value
    });

    it('should warn if the stateStorage or stateKey is not found', () => {
        spyOn(console, 'warn');

        spectator = createDirective(`<div dotStateRestore></div>`, {
            providers: [{ provide: Table, useValue: null }]
        });

        expect(console.warn).toHaveBeenCalledWith(
            'DotStateRestoreDirective: stateStorage or stateKey not found'
        );
    });
});
