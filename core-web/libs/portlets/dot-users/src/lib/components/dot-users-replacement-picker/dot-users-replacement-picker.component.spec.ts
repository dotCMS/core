import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersReplacementPickerComponent } from './dot-users-replacement-picker.component';

import { DotUserListItem, DotUsersService } from '../../services/dot-users.service';
import { createFakeUser } from '../../testing/dot-user.mock';

const MESSAGES = {
    'users.dialog.delete-confirm.replacement-placeholder': 'Select a replacement'
};

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
                createFakeUser({ userId: 'user-1' }),
                createFakeUser({ userId: 'user-2' }),
                createFakeUser({ userId: 'user-3' })
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

    describe('onSearch — loading, error, empty states', () => {
        it('flips $isLoading on before the response resolves', () => {
            spectator = createComponent();
            const service = spectator.inject(DotUsersService, true);
            const gate = new Subject<{
                entity: DotUserListItem[];
                errors: [];
                messages: [];
                permissions: [];
                i18nMessagesMap: Record<string, string>;
                pagination: { currentPage: number; perPage: number; totalEntries: number };
            }>();
            (service.getUsersPaginated as jest.Mock).mockReturnValue(gate);

            spectator.component['onSearch'](searchEvent('ada'));

            expect(spectator.component['$isLoading']()).toBe(true);
            expect(spectator.component['$hasError']()).toBe(false);

            gate.next({
                entity: [],
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {},
                pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
            });
            gate.complete();

            expect(spectator.component['$isLoading']()).toBe(false);
        });

        it('surfaces an error flag when the service fails and clears suggestions', () => {
            spectator = createComponent();
            const service = spectator.inject(DotUsersService, true);
            (service.getUsersPaginated as jest.Mock).mockReturnValue(
                throwError(() => new Error('boom'))
            );

            spectator.component['$suggestions'].set([createFakeUser()]);

            spectator.component['onSearch'](searchEvent('ada'));

            expect(spectator.component['$hasError']()).toBe(true);
            expect(spectator.component['$isLoading']()).toBe(false);
            expect(spectator.component['$suggestions']()).toEqual([]);
        });

        it('recovers the error flag when a subsequent query succeeds', () => {
            spectator = createComponent();
            const service = spectator.inject(DotUsersService, true);
            (service.getUsersPaginated as jest.Mock).mockReturnValueOnce(
                throwError(() => new Error('boom'))
            );

            spectator.component['onSearch'](searchEvent('ada'));
            expect(spectator.component['$hasError']()).toBe(true);

            (service.getUsersPaginated as jest.Mock).mockReturnValueOnce(
                of({
                    entity: [createFakeUser({ userId: 'user-1' })],
                    errors: [],
                    messages: [],
                    permissions: [],
                    i18nMessagesMap: {},
                    pagination: { currentPage: 1, perPage: 10, totalEntries: 1 }
                })
            );
            spectator.component['onSearch'](searchEvent('ada again'));

            expect(spectator.component['$hasError']()).toBe(false);
            expect(spectator.component['$suggestions']().map((u) => u.userId)).toEqual(['user-1']);
        });

        it('cancels an in-flight request when a newer query arrives (switchMap)', () => {
            spectator = createComponent();
            const service = spectator.inject(DotUsersService, true);
            const stale = new Subject<{
                entity: DotUserListItem[];
                errors: [];
                messages: [];
                permissions: [];
                i18nMessagesMap: Record<string, string>;
                pagination: { currentPage: number; perPage: number; totalEntries: number };
            }>();
            (service.getUsersPaginated as jest.Mock).mockReturnValueOnce(stale);

            spectator.component['onSearch'](searchEvent('slow'));

            const fresh = createFakeUser({ userId: 'fresh-1' });
            (service.getUsersPaginated as jest.Mock).mockReturnValueOnce(
                of({
                    entity: [fresh],
                    errors: [],
                    messages: [],
                    permissions: [],
                    i18nMessagesMap: {},
                    pagination: { currentPage: 1, perPage: 10, totalEntries: 1 }
                })
            );
            spectator.component['onSearch'](searchEvent('fast'));

            // Stale response resolves LATE — must be ignored, current
            // suggestions stay as the fresh set.
            stale.next({
                entity: [createFakeUser({ userId: 'stale-1' })],
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {},
                pagination: { currentPage: 1, perPage: 10, totalEntries: 1 }
            });
            stale.complete();

            expect(spectator.component['$suggestions']().map((u) => u.userId)).toEqual(['fresh-1']);
        });
    });

    describe('selectionChange output', () => {
        it('emits the selected user when onSelect is called', () => {
            spectator = createComponent();
            const emitted: (DotUserListItem | null)[] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));

            const user = createFakeUser({ userId: 'user-99' });
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
                createFakeUser({
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
                createFakeUser({ fullName: '', name: 'Nick Name' })
            );
            expect(value).toBe('Nick Name');
        });

        it('falls back to first+last when both fullName and name are blank', () => {
            const value = spectator.component['displayName'](
                createFakeUser({
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
                createFakeUser({
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
                createFakeUser({
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
                createFakeUser({ fullName: '   ', name: 'Nick Name' })
            );
            expect(value).toBe('Nick Name');
        });
    });
});
