import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
    dotUserDisplayName,
    DotUserSearchRow,
    DotUserSearchService,
    MAX_RESOLVE_PAGES
} from './dot-user-search.service';

/**
 * One page of the directory as `GET /api/v1/users/filter` returns it: the envelope, not a bare
 * array. `pagination.totalEntries` is what lets a caller know whether more pages remain, so a
 * service that mapped it away would make infinite scroll impossible to stop (FR-011).
 */
const pageResponse = (userIds: string[], totalEntries: number) => ({
    entity: userIds.map((userId) => ({
        userId,
        firstName: 'First',
        lastName: userId,
        fullName: `First ${userId}`,
        emailAddress: `${userId}@dotcms.com`
    })),
    errors: [],
    messages: [],
    permissions: [],
    i18nMessagesMap: {},
    pagination: { currentPage: 1, perPage: 20, totalEntries }
});

describe('DotUserSearchService', () => {
    let service: DotUserSearchService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotUserSearchService, provideHttpClient(), provideHttpClientTesting()]
        });

        service = TestBed.inject(DotUserSearchService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    describe('searchPage', () => {
        it('should request the directory with query, page and per_page', () => {
            service.searchPage({ filter: 'jane', page: 2, perPage: 20 }).subscribe();

            const req = httpMock.expectOne(
                (request) => request.url === '/api/v1/users/filter' && request.method === 'GET'
            );

            expect(req.request.params.get('query')).toBe('jane');
            expect(req.request.params.get('page')).toBe('2');
            expect(req.request.params.get('per_page')).toBe('20');
        });

        it('should send page 1 for the first page, not 0', () => {
            // The endpoint declares a default of 0 but coerces page <= 0 to the first page, so it
            // is 1-based in practice and the loader contract is 1-based too.
            service.searchPage({ filter: '', page: 1, perPage: 20 }).subscribe();

            const req = httpMock.expectOne((request) => request.url === '/api/v1/users/filter');

            expect(req.request.params.get('page')).toBe('1');
        });

        it('should return the envelope so the caller can read pagination.totalEntries', () => {
            let received: unknown;
            service
                .searchPage({ filter: '', page: 1, perPage: 20 })
                .subscribe((response) => (received = response));

            httpMock
                .expectOne((request) => request.url === '/api/v1/users/filter')
                .flush(pageResponse(['dotcms.org.1'], 57));

            expect(received).toMatchObject({ pagination: { totalEntries: 57 } });
        });

        it('should propagate a failure rather than swallowing it', () => {
            let errored = false;
            service
                .searchPage({ filter: '', page: 1, perPage: 20 })
                .subscribe({ error: () => (errored = true) });

            httpMock
                .expectOne((request) => request.url === '/api/v1/users/filter')
                .flush('nope', { status: 500, statusText: 'Server Error' });

            expect(errored).toBe(true);
        });
    });

    describe('an exact id that is not on the first page', () => {
        /**
         * `query` matches ids as a **substring**, so asking for `dotcms.org.1` also returns
         * `dotcms.org.10`, `.11`, `.100` and so on. One page of twenty was the original mitigation,
         * and it is not a guarantee: past twenty near-misses the exact id falls off the page and a
         * previously-selected real person renders as a raw id after a reload — the failure routing
         * through the directory search exists to avoid, reached another way.
         */
        it('should keep paging until it finds the exact id', () => {
            let resolved: Record<string, string> | undefined;
            service.resolveNames(['dotcms.org.1']).subscribe((names) => (resolved = names));

            // Page one: twenty ids that merely contain the searched one.
            const first = httpMock.expectOne((r) => r.url === '/api/v1/users/filter');
            first.flush({
                entity: Array.from({ length: 20 }, (_, i) => ({
                    userId: `dotcms.org.1${i}`,
                    fullName: `Near miss ${i}`
                })),
                pagination: { currentPage: 1, perPage: 20, totalEntries: 21 }
            });

            const second = httpMock.expectOne((r) => r.url === '/api/v1/users/filter');
            expect(second.request.params.get('page')).toBe('2');
            second.flush({
                entity: [{ userId: 'dotcms.org.1', fullName: 'Jane Doe' }],
                pagination: { currentPage: 2, perPage: 20, totalEntries: 21 }
            });

            expect(resolved).toEqual({ 'dotcms.org.1': 'Jane Doe' });
        });

        it('should stop once the directory is exhausted rather than paging for ever', () => {
            let resolved: Record<string, string> | undefined;
            service.resolveNames(['nobody']).subscribe((names) => (resolved = names));

            const only = httpMock.expectOne((r) => r.url === '/api/v1/users/filter');
            only.flush({
                entity: [{ userId: 'nobody-else', fullName: 'Someone' }],
                pagination: { currentPage: 1, perPage: 20, totalEntries: 1 }
            });

            // One page held everything there was, so there is no second request to make.
            httpMock.verify();
            expect(resolved).toEqual({ nobody: 'nobody' });
        });

        it('should give up after a bounded number of pages', () => {
            // A pathological query could otherwise walk a directory of any size. The id is worth
            // one bounded search, not an unbounded one — it still renders, just as itself.
            let resolved: Record<string, string> | undefined;
            service.resolveNames(['haystack']).subscribe((names) => (resolved = names));

            for (let page = 1; page <= MAX_RESOLVE_PAGES; page++) {
                const req = httpMock.expectOne((r) => r.url === '/api/v1/users/filter');
                expect(req.request.params.get('page')).toBe(String(page));
                req.flush({
                    entity: Array.from({ length: 20 }, (_, i) => ({
                        userId: `haystack-${page}-${i}`,
                        fullName: 'Near miss'
                    })),
                    pagination: { currentPage: page, perPage: 20, totalEntries: 10_000 }
                });
            }

            httpMock.verify();
            expect(resolved).toEqual({ haystack: 'haystack' });
        });

        it('should order the search so paging is stable', () => {
            // Without an explicit order the endpoint's is unspecified, and a set that shifts
            // between two requests can show a row twice and never show another at all.
            service.resolveNames(['dotcms.org.1']).subscribe();

            const req = httpMock.expectOne((r) => r.url === '/api/v1/users/filter');

            expect(req.request.params.get('orderby')).toBe('userId');
            expect(req.request.params.get('direction')).toBe('ASC');
            req.flush({ entity: [], pagination: { currentPage: 1, perPage: 20, totalEntries: 0 } });
        });
    });

    describe('resolveNames', () => {
        it('should query the directory once per id', () => {
            service.resolveNames(['dotcms.org.1', 'dotcms.org.2']).subscribe();

            const requests = httpMock.match((request) => request.url === '/api/v1/users/filter');

            // There is no bulk-by-ids parameter on the endpoint, so N ids is N calls (FR-009d).
            expect(requests.length).toBe(2);
            requests.forEach((req) => req.flush(pageResponse([], 0)));
        });

        it('should pick the EXACT id match, not the first result', () => {
            // `query` matches ids as a substring, so asking for a short id also returns every
            // longer id containing it. Taking the first result would label the wrong person.
            let resolved: Record<string, string> | undefined;
            service.resolveNames(['dotcms.org.1']).subscribe((names) => (resolved = names));

            httpMock
                .expectOne((request) => request.url === '/api/v1/users/filter')
                .flush(pageResponse(['dotcms.org.10', 'dotcms.org.1', 'dotcms.org.11'], 3));

            expect(resolved).toEqual({ 'dotcms.org.1': 'First dotcms.org.1' });
        });

        it('should fall back to the id when the directory returns no exact match', () => {
            let resolved: Record<string, string> | undefined;
            service.resolveNames(['ghost.user']).subscribe((names) => (resolved = names));

            httpMock
                .expectOne((request) => request.url === '/api/v1/users/filter')
                .flush(pageResponse([], 0));

            expect(resolved).toEqual({ 'ghost.user': 'ghost.user' });
        });

        it('should fall back to the id when the lookup itself fails', () => {
            // A failed resolution must never drop the selection: the filter matches on ids and
            // does not depend on the label (FR-009f).
            let resolved: Record<string, string> | undefined;
            service.resolveNames(['dotcms.org.1']).subscribe((names) => (resolved = names));

            httpMock
                .expectOne((request) => request.url === '/api/v1/users/filter')
                .flush('nope', { status: 500, statusText: 'Server Error' });

            expect(resolved).toEqual({ 'dotcms.org.1': 'dotcms.org.1' });
        });

        it('should issue no request at all for an empty selection', () => {
            service.resolveNames([]).subscribe();

            httpMock.expectNone((request) => request.url === '/api/v1/users/filter');
        });
    });
});

describe('dotUserDisplayName', () => {
    const row = (fields: Partial<DotUserSearchRow>): DotUserSearchRow => ({
        userId: 'dotcms.org.1',
        ...fields
    });

    it('should prefer the full name', () => {
        expect(
            dotUserDisplayName(
                row({ fullName: 'Jane Doe', firstName: 'Jane', emailAddress: 'jane@dotcms.com' })
            )
        ).toBe('Jane Doe');
    });

    it('should fall back to first and last name when the full name is blank', () => {
        // Blank rather than absent: this is the shape a legacy or partially-imported account
        // actually arrives in, and the reason the chain exists at all.
        expect(
            dotUserDisplayName(row({ fullName: '   ', firstName: 'Jane', lastName: 'Doe' }))
        ).toBe('Jane Doe');
    });

    it('should use whichever half of the name is present', () => {
        expect(dotUserDisplayName(row({ lastName: 'Doe' }))).toBe('Doe');
    });

    it('should fall back to the email when there is no name at all', () => {
        expect(dotUserDisplayName(row({ emailAddress: 'jane@dotcms.com' }))).toBe(
            'jane@dotcms.com'
        );
    });

    it('should fall back to the id last, so the result is never blank', () => {
        expect(dotUserDisplayName(row({ fullName: '', emailAddress: '  ' }))).toBe('dotcms.org.1');
    });

    it('should trim what it returns', () => {
        expect(dotUserDisplayName(row({ fullName: '  Jane Doe  ' }))).toBe('Jane Doe');
    });
});
