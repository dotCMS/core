import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersReplacementPickerComponent } from './dot-users-replacement-picker.component';

import { DotUserListItem, DotUsersService } from '../../services/dot-users.service';

const MESSAGES = {
    'users.dialog.delete-confirm.replacement-placeholder': 'Select a replacement'
};

/** Baseline user shape — spec-level defaults kept exhaustive so
 * individual tests can override only the fields under test without
 * repeating the boilerplate. */
function fakeUser(overrides: Partial<DotUserListItem> = {}): DotUserListItem {
    return {
        userId: 'user-1',
        id: 'user-1',
        firstName: 'Jane',
        lastName: 'Doe',
        fullName: 'Jane Doe',
        name: 'Jane Doe',
        emailAddress: 'jane@dotcms.com',
        gravitar: '',
        active: true,
        admin: false,
        backendUser: true,
        frontendUser: false,
        hasConsoleAccess: true,
        lastLoginDate: null,
        lastLoginIP: null,
        failedLoginAttempts: 0,
        ...overrides
    };
}

/** Minimal search event — component only reads `.query`. */
function searchEvent(query: string): AutoCompleteCompleteEvent {
    return { originalEvent: new Event('input'), query } as AutoCompleteCompleteEvent;
}

describe('DotUsersReplacementPickerComponent', () => {
    let spectator: Spectator<DotUsersReplacementPickerComponent>;

    const createComponent = createComponentFactory({
        component: DotUsersReplacementPickerComponent,
        detectChanges: false,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(DotUsersService, {
                getUsersPaginated: jest.fn()
            })
        ]
    });

    describe('onSearch — exclusion filtering', () => {
        it('drops candidates whose userId appears in excludedUserIds', () => {
            const candidates = [
                fakeUser({ userId: 'user-1' }),
                fakeUser({ userId: 'user-2' }),
                fakeUser({ userId: 'user-3' })
            ];
            spectator = createComponent({
                props: { excludedUserIds: ['user-2'] }
            });
            const service = spectator.inject(DotUsersService, true);
            (service.getUsersPaginated as jest.Mock).mockReturnValue(
                of({
                    entity: candidates,
                    errors: [],
                    messages: [],
                    permissions: [],
                    i18nMessagesMap: {},
                    pagination: { currentPage: 1, perPage: 10, totalEntries: 3 }
                })
            );

            spectator.component['onSearch'](searchEvent('user'));

            const suggestions = spectator.component['$suggestions']();
            expect(suggestions.map((u) => u.userId)).toEqual(['user-1', 'user-3']);
        });

        it('forwards the query as the `filter` param and asks for the first page', () => {
            spectator = createComponent();
            const service = spectator.inject(DotUsersService, true);
            (service.getUsersPaginated as jest.Mock).mockReturnValue(
                of({
                    entity: [],
                    errors: [],
                    messages: [],
                    permissions: [],
                    i18nMessagesMap: {},
                    pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
                })
            );

            spectator.component['onSearch'](searchEvent('ada'));

            expect(service.getUsersPaginated).toHaveBeenCalledWith({
                filter: 'ada',
                page: 1,
                perPage: 10
            });
        });
    });

    describe('onSearch — error path', () => {
        it('swallows the error and empties the suggestion list', () => {
            spectator = createComponent();
            const service = spectator.inject(DotUsersService, true);
            (service.getUsersPaginated as jest.Mock).mockReturnValue(
                throwError(() => new Error('boom'))
            );

            spectator.component['$suggestions'].set([fakeUser()]);

            spectator.component['onSearch'](searchEvent('ada'));

            expect(spectator.component['$suggestions']()).toEqual([]);
        });
    });

    describe('selectionChange output', () => {
        it('emits the selected user when onSelect is called', () => {
            spectator = createComponent();
            const emitted: (DotUserListItem | null)[] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));

            const user = fakeUser({ userId: 'user-99' });
            spectator.component['onSelect'](user);

            expect(emitted).toEqual([user]);
        });

        it('emits null when the picker is cleared', () => {
            spectator = createComponent();
            const emitted: (DotUserListItem | null)[] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));

            spectator.component['onSelect'](null);

            expect(emitted).toEqual([null]);
        });
    });

    describe('displayName fallback chain', () => {
        beforeEach(() => {
            spectator = createComponent();
        });

        it('returns fullName when it is set', () => {
            const value = spectator.component['displayName'](
                fakeUser({
                    fullName: 'Jane Doe',
                    name: 'ignored',
                    firstName: 'ignored',
                    lastName: 'ignored',
                    emailAddress: 'ignored@x.com'
                })
            );
            expect(value).toBe('Jane Doe');
        });

        it('falls back to name when fullName is blank', () => {
            const value = spectator.component['displayName'](
                fakeUser({ fullName: '', name: 'Nick Name' })
            );
            expect(value).toBe('Nick Name');
        });

        it('falls back to first+last when both fullName and name are blank', () => {
            const value = spectator.component['displayName'](
                fakeUser({
                    fullName: '',
                    name: '',
                    firstName: 'Ada',
                    lastName: 'Lovelace'
                })
            );
            expect(value).toBe('Ada Lovelace');
        });

        it('falls back to emailAddress when every name field is blank', () => {
            const value = spectator.component['displayName'](
                fakeUser({
                    fullName: '',
                    name: '',
                    firstName: '',
                    lastName: '',
                    emailAddress: 'x@y.com'
                })
            );
            expect(value).toBe('x@y.com');
        });

        it('returns an empty string when every source field is missing', () => {
            const value = spectator.component['displayName'](
                fakeUser({
                    fullName: '',
                    name: '',
                    firstName: '',
                    lastName: '',
                    emailAddress: ''
                })
            );
            expect(value).toBe('');
        });

        it('treats whitespace-only fullName as blank and continues the fallback', () => {
            const value = spectator.component['displayName'](
                fakeUser({ fullName: '   ', name: 'Nick Name' })
            );
            expect(value).toBe('Nick Name');
        });
    });
});
