import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersFilterByComponent } from './dot-users-filter-by.component';

import {
    DotUsersListStore,
    USERS_FILTER_ALL,
    USERS_FILTER_BACKEND,
    USERS_FILTER_CMS_ADMIN,
    USERS_FILTER_FRONTEND
} from '../../store/dot-users-list.store';

const MESSAGES = {
    'users.filter.by': 'Filter by',
    'users.filter.all-access': 'All access',
    'users.access.backend': 'Back-end',
    'users.access.frontend': 'Front-end',
    'users.access.cms-admin': 'CMS Administrator',
    search: 'Search',
    'dot.common.remove': 'Remove'
};

describe('DotUsersFilterByComponent', () => {
    let spectator: Spectator<DotUsersFilterByComponent>;
    const canFilterByRole = signal(true);

    const createComponent = createComponentFactory({
        component: DotUsersFilterByComponent,
        detectChanges: false,
        componentProviders: [
            mockProvider(DotUsersListStore, {
                roleFilter: vi.fn().mockReturnValue(USERS_FILTER_ALL),
                canFilterByRole,
                setRoleFilter: vi.fn()
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
        canFilterByRole.set(true);
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

    it('should offer the access options plus CMS Administrator', () => {
        const options = spectator.component['options'];

        expect(options).toEqual([
            { value: USERS_FILTER_ALL, label: 'All access' },
            { value: USERS_FILTER_BACKEND, label: 'Back-end' },
            { value: USERS_FILTER_FRONTEND, label: 'Front-end' },
            { value: USERS_FILTER_CMS_ADMIN, label: 'CMS Administrator' }
        ]);
    });

    it('should push the CMS Administrator role key to the store and label the chip with it', () => {
        const store = spectator.inject(DotUsersListStore, true);

        spectator.component['$selectedValue'].set(USERS_FILTER_CMS_ADMIN);
        spectator.component['onChange']();
        spectator.detectChanges();

        expect(store.setRoleFilter).toHaveBeenCalledWith(USERS_FILTER_CMS_ADMIN);
        expect(spectator.component['$selections']()).toEqual(['CMS Administrator']);
    });

    it('should hide the chip once the viewer is known not to be allowed to filter by role', () => {
        canFilterByRole.set(false);
        spectator.detectChanges();

        expect(spectator.query(byTestId('users-filter-by-chip'))).toBeNull();
    });
});
