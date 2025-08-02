import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator/jest';

import { Table, TableModule } from 'primeng/table';

import { DotStateRestoreDirective } from './dot-state-restore.directive';

describe('DotStateRestoreDirective', () => {
    let spectator: SpectatorDirective<DotStateRestoreDirective>;

    const createDirective = createDirectiveFactory({
        directive: DotStateRestoreDirective,
        template: `<p-table stateStorage="session" stateKey="test-key"  dotStateRestore></p-table>`,
        imports: [TableModule]
    });

    const savedState = { sortField: 'name', sortOrder: 1 };

    it('should apply the saved state from localStorage', () => {
        jest.spyOn(localStorage, 'getItem').mockReturnValue(JSON.stringify(savedState));
        spectator = createDirective(
            `<p-table stateStorage="local" stateKey="test-key"  dotStateRestore></p-table>`
        );

        const table = spectator.query(Table);

        expect(table.sortField).toBe(savedState.sortField);
        expect(table.sortOrder).toBe(savedState.sortOrder);
    });

    it('should apply the saved state from sessionStorage', () => {
        jest.spyOn(sessionStorage, 'getItem').mockReturnValue(JSON.stringify(savedState));

        spectator = createDirective();

        const table = spectator.query(Table);

        expect(table.sortField).toBe(savedState.sortField);
        expect(table.sortOrder).toBe(savedState.sortOrder);
    });

    it('should not apply state if no state is saved', () => {
        // Clear any stored state
        sessionStorage.clear();
        localStorage.clear();

        // Mock storage getItem to explicitly return null
        jest.spyOn(sessionStorage, 'getItem').mockReturnValue(null);
        jest.spyOn(localStorage, 'getItem').mockReturnValue(null);

        spectator = createDirective(
            `<p-table stateStorage="session" stateKey="no-key"  dotStateRestore></p-table>`
        );

        const table = spectator.query(Table);

        // Check that no stored state is applied
        expect(table.sortField).toBeUndefined();
        expect(table.sortOrder).toBe(1); //default value from PrimeNG Table
    });

    it('should warn if the stateStorage or stateKey is not found', () => {
        const consoleSpy = jest.spyOn(console, 'warn');

        spectator = createDirective(`<div dotStateRestore></div>`, {
            providers: [{ provide: Table, useValue: null }]
        });

        expect(consoleSpy).toHaveBeenCalledWith(
            'DotStateRestoreDirective: stateStorage or stateKey not found'
        );
    });
});
