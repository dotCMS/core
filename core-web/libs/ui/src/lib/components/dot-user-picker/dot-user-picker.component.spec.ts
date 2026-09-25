import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@openng/spectator/vitest';
import { firstValueFrom, of } from 'rxjs';
import { Mock, vi } from 'vitest';

import { DotMessageService, DotUserSearchRow, DotUserSearchService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DOT_USER_PICKER_ITEM_HEIGHT, DotUserPickerComponent } from './dot-user-picker.component';

import { DotLazyMultiselectComponent } from '../dot-filter-bar/chips/dot-field-filter/dot-lazy-multiselect/dot-lazy-multiselect.component';

const user = (userId: string, extra: Partial<DotUserSearchRow> = {}): DotUserSearchRow => ({
    userId,
    firstName: `First ${userId}`,
    lastName: `Last ${userId}`,
    emailAddress: `${userId}@dotcms.com`,
    ...extra
});

/** A directory page as `/v1/users/filter` returns it. */
const directoryPage = (rows: DotUserSearchRow[], totalEntries: number) =>
    of({ entity: rows, pagination: { currentPage: 1, perPage: 100, totalEntries } });

const ids = (count: number, from = 0) =>
    Array.from({ length: count }, (_, i) => user(`u${from + i}`));

describe('DotUserPickerComponent', () => {
    let spectator: SpectatorHost<DotUserPickerComponent>;
    let searchPage: Mock;

    const createHost = createHostFactory({
        component: DotUserPickerComponent,
        // Component-level: the picker provides the search itself, so a TestBed provider would be
        // shadowed and the real HTTP path would run.
        componentProviders: [
            mockProvider(DotUserSearchService, {
                searchPage: vi.fn(),
                resolveNames: vi.fn().mockReturnValue(of({}))
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ add: 'Add', search: 'Search' })
            }
        ],
        detectChanges: false
    });

    /** Hosts the picker behind a trigger that opens it, as a consumer would. */
    const build = (attributes = '', content = '', hostProps: Record<string, unknown> = {}) => {
        spectator = createHost(
            `
            <button data-testid="trigger" (click)="picker.toggle($event)">Add</button>
            <dot-user-picker #picker ${attributes}>${content}</dot-user-picker>
        `,
            { hostProps }
        );
        searchPage = spectator.inject(DotUserSearchService, true).searchPage as Mock;
        searchPage.mockReset().mockReturnValue(directoryPage(ids(2), 2));
        spectator.detectChanges();
    };

    const open = () => {
        spectator.click(byTestId('trigger'));
        spectator.detectChanges();
    };

    const list = () => spectator.query(DotLazyMultiselectComponent) as DotLazyMultiselectComponent;

    /** Pages the list the way it does while scrolling, returning the options handed back. */
    const loadPage = (page: number) =>
        firstValueFrom(spectator.component.loadPage({ page, perPage: 20, filter: '' }));

    it('should default to picking a single person', () => {
        build();

        expect(spectator.component.$multiple()).toBe(false);
    });

    describe('directory loading', () => {
        it('should map directory rows to options carrying the full row', async () => {
            build();
            const { options, hasMore } = await loadPage(1);

            expect(options).toEqual([
                { value: 'u0', label: 'First u0 Last u0', data: user('u0') },
                { value: 'u1', label: 'First u1 Last u1', data: user('u1') }
            ]);
            expect(hasMore).toBe(false);
        });

        it('should hide excluded people', async () => {
            build('[excludeIds]="excluded"', '', { excluded: ['u0'] });

            const { options } = await loadPage(1);

            expect(options.map((o) => o.value)).toEqual(['u1']);
        });

        it('should keep reading the directory until a thinned page is full again', async () => {
            // 150 people, the first 100 of whom are excluded: one directory page yields nothing,
            // and a short page would read as the end of the list and stop the scroll.
            build('[excludeIds]="excluded"', '', { excluded: ids(100).map((u) => u.userId) });
            searchPage.mockImplementation(({ page }: { page: number }) =>
                page === 1 ? directoryPage(ids(100), 150) : directoryPage(ids(50, 100), 150)
            );

            const { options, hasMore } = await loadPage(1);

            expect(searchPage).toHaveBeenCalledTimes(2);
            expect(options).toHaveLength(20);
            expect(options[0].value).toBe('u100');
            expect(hasMore).toBe(true);
        });

        it('should serve the next page from what it already read before asking again', async () => {
            build();
            searchPage.mockReturnValue(directoryPage(ids(100), 100));

            await loadPage(1);
            const second = await loadPage(2);

            expect(searchPage).toHaveBeenCalledTimes(1);
            expect(second.options[0].value).toBe('u20');
            expect(second.hasMore).toBe(true);
        });

        it('should start the walk over for a new search', async () => {
            build();
            searchPage.mockReturnValue(directoryPage(ids(100), 100));
            await loadPage(1);

            await loadPage(1);

            expect(searchPage).toHaveBeenLastCalledWith({ filter: '', page: 1, perPage: 100 });
        });
    });

    describe('picking', () => {
        it('should emit the chosen person and close in single mode', () => {
            build();
            open();
            const picked = vi.fn();
            spectator.output('picked').subscribe(picked);
            const hide = vi.spyOn(spectator.component, 'hide');

            // Through the real list, as a click on a row arrives: the list has to hand the
            // directory row back for there to be anyone to emit.
            spectator.triggerEventHandler('p-listbox', 'onChange', { value: 'u0' });

            expect(picked).toHaveBeenCalledWith([user('u0')]);
            expect(hide).toHaveBeenCalled();
        });

        it('should wait for the Add button in multiple mode', () => {
            build('[multiple]="true"');
            open();
            const picked = vi.fn();
            spectator.output('picked').subscribe(picked);

            spectator.triggerEventHandler('p-listbox', 'onChange', { value: ['u0', 'u1'] });
            spectator.detectChanges();
            expect(picked).not.toHaveBeenCalled();

            spectator.click(
                spectator.query(byTestId('user-picker-confirm'))?.querySelector('button') as Element
            );

            expect(picked).toHaveBeenCalledWith([user('u0'), user('u1')]);
        });

        it('should keep Add disabled until someone is ticked', () => {
            build('[multiple]="true"');
            open();

            const confirm = spectator
                .query(byTestId('user-picker-confirm'))
                ?.querySelector('button') as HTMLButtonElement;
            expect(confirm.disabled).toBe(true);
        });

        it('should hand the list checkboxes only in multiple mode', () => {
            build('[multiple]="true"');
            open();

            expect(list().$multiple()).toBe(true);
        });
    });

    describe('rows', () => {
        const option = { value: 'u0', label: 'First u0 Last u0', data: user('u0') };

        /**
         * Renders the row template the picker hands the list. The list itself cannot be asked:
         * its virtual scroller measures a zero-height viewport in jsdom and renders no rows.
         */
        const renderRow = (): string => {
            const template = list().$itemTemplate();
            expect(template).toBeTruthy();
            const view = template?.createEmbeddedView({ $implicit: option });
            view?.detectChanges();

            return (view?.rootNodes ?? []).map((node: Node) => node.textContent ?? '').join('');
        };

        it('should render avatar, name and email by default', () => {
            build();
            open();

            const text = renderRow();
            expect(text).toContain('First u0 Last u0');
            expect(text).toContain('u0@dotcms.com');
            expect(text).toContain('FL');
        });

        it('should render a consumer template with the directory row', () => {
            build(
                '',
                `<ng-template #item let-user>
                    <span data-testid="custom-row">custom {{ user.emailAddress }}</span>
                </ng-template>`
            );
            open();

            const text = renderRow();
            expect(text).toContain('custom u0@dotcms.com');
            expect(text).not.toContain('First u0 Last u0');
        });

        it('should tell the list the default row height', () => {
            build();
            open();

            expect(list().$itemSize()).toBe(DOT_USER_PICKER_ITEM_HEIGHT);
        });
    });
});
