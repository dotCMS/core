import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotUsersFilterByComponent,
    USERS_FILTER_ALL,
    USERS_FILTER_BACKEND,
    USERS_FILTER_FRONTEND
} from './dot-users-filter-by.component';

import { DotUsersListStore } from '../../store/dot-users-list.store';

const MESSAGES = {
    'users.filter.by': 'Filter by',
    'users.filter.all-access': 'All access',
    'users.access.backend': 'Back-end',
    'users.access.frontend': 'Front-end',
    search: 'Search',
    'dot.common.remove': 'Remove'
};

describe('DotUsersFilterByComponent', () => {
    let spectator: Spectator<DotUsersFilterByComponent>;

    const createComponent = createComponentFactory({
        component: DotUsersFilterByComponent,
        detectChanges: false,
        componentProviders: [
            mockProvider(DotUsersListStore, {
                roleFilter: jest.fn().mockReturnValue(USERS_FILTER_ALL),
                setRoleFilter: jest.fn()
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(MESSAGES)
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should render the "Filter by" chip', () => {
        const chip = spectator.query(byTestId('users-filter-by-chip'));

        expect(chip).toBeTruthy();
        expect(chip?.textContent).toContain('Filter by');
    });

    it('should push the selected role key to the store on change', () => {
        const store = spectator.inject(DotUsersListStore, true);

        // Simulate the listbox emitting the value
        spectator.component['$selectedValue'].set(USERS_FILTER_BACKEND);
        spectator.component['onChange']();

        expect(store.setRoleFilter).toHaveBeenCalledWith(USERS_FILTER_BACKEND);
    });

    it('onRemove should clear the selection and call setRoleFilter with All access', () => {
        const store = spectator.inject(DotUsersListStore, true);
        spectator.component['$selectedValue'].set(USERS_FILTER_FRONTEND);

        spectator.component['onRemove']();

        expect(spectator.component['$selectedValue']()).toBe(USERS_FILTER_ALL);
        expect(store.setRoleFilter).toHaveBeenCalledWith(USERS_FILTER_ALL);
    });

    it('should expose the three filter options with labels', () => {
        const options = spectator.component['$options'];

        expect(options).toEqual([
            { value: USERS_FILTER_ALL, label: 'All access' },
            { value: USERS_FILTER_BACKEND, label: 'Back-end' },
            { value: USERS_FILTER_FRONTEND, label: 'Front-end' }
        ]);
    });
});
