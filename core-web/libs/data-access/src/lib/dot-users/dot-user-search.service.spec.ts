import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
    dotUserDisplayName,
    DotUserSearchRow,
    DotUserSearchService
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
