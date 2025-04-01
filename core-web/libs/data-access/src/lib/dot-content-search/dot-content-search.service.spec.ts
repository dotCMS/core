import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { HttpErrorResponse } from '@angular/common/http';

import { createFakeContentlet } from '@dotcms/utils-testing';

import {
    DotContentSearchService,
    EsQueryParamsSearch,
    DotContentSearchParams,
    DotContentSearchResponse
} from './dot-content-search.service';

describe('DotContentSearchService', () => {
    let spectator: SpectatorHttp<DotContentSearchService>;
    const createHttp = createHttpFactory(DotContentSearchService);

    beforeEach(() => (spectator = createHttp()));

    it('should call the search method with the right EsQueryParamsSearch', (done) => {
        const params: EsQueryParamsSearch = {
            query: 'test',
            limit: 10,
            offset: 0
        };

        spectator.service.get(params).subscribe((resp) => {
            expect(resp).toEqual({ contentlets: [] });
            done();
        });

        const req = spectator.expectOne('/api/content/_search', HttpMethod.POST);
        expect(req.request.body).toEqual({
            query: 'test',
            sort: 'score,modDate desc',
            limit: 10,
            offset: 0
        });
        req.flush({
            entity: {
                contentlets: []
            }
        });
    });

    describe('search', () => {
        const mockData: DotContentSearchResponse['entity'] = {
            jsonObjectView: {
                contentlets: []
            },
            resultsSize: 0
        };

        beforeEach(() => {
            mockData.jsonObjectView.contentlets = [
                createFakeContentlet(),
                createFakeContentlet(),
                createFakeContentlet()
            ];
            mockData.resultsSize = mockData.jsonObjectView.contentlets.length;
        });

        it('should call the search endpoint with all provided parameters', (done) => {
            const params: DotContentSearchParams = {
                globalSearch: 'test query',
                systemSearchableFields: { languageId: 1 },
                searchableFieldsByContentType: { Blog: { title: 'test' } },
                page: 1,
                perPage: 10
            };

            spectator.service.search(params).subscribe((result) => {
                expect(result).toEqual(mockData);
                done();
            });

            const req = spectator.expectOne('/api/v1/content/search', HttpMethod.POST);
            expect(req.request.body).toEqual({
                globalSearch: 'test query',
                systemSearchableFields: { languageId: 1 },
                searchableFieldsByContentType: { Blog: { title: 'test' } },
                page: 1,
                perPage: 10
            });

            req.flush({
                entity: mockData
            });
        });

        it('should call the search endpoint with only the globalSearch parameter', (done) => {
            const params: DotContentSearchParams = {
                globalSearch: 'test query'
            };

            spectator.service.search(params).subscribe((result) => {
                expect(result).toEqual(mockData);
                done();
            });

            const req = spectator.expectOne('/api/v1/content/search', HttpMethod.POST);
            expect(req.request.body).toEqual({
                globalSearch: 'test query'
            });

            req.flush({
                entity: mockData
            });
        });

        it('should call the search endpoint with only systemSearchableFields parameter', () => {
            const params: DotContentSearchParams = {
                systemSearchableFields: { languageId: 1, contentType: 'Blog' }
            };

            spectator.service.search(params).subscribe((result) => {
                expect(result).toEqual(mockData);
            });

            const req = spectator.expectOne('/api/v1/content/search', HttpMethod.POST);
            expect(req.request.body).toEqual({
                systemSearchableFields: { languageId: 1, contentType: 'Blog' }
            });

            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: mockData
                    }
                }
            });
        });

        it('should call the search endpoint with only searchableFieldsByContentType parameter', (done) => {
            const params: DotContentSearchParams = {
                searchableFieldsByContentType: { Blog: { title: 'test' } }
            };

            spectator.service.search(params).subscribe((result) => {
                expect(result).toEqual(mockData);
                done();
            });

            const req = spectator.expectOne('/api/v1/content/search', HttpMethod.POST);
            expect(req.request.body).toEqual({
                searchableFieldsByContentType: { Blog: { title: 'test' } }
            });

            req.flush({
                entity: mockData
            });
        });

        it('should call the search endpoint with pagination parameters only', (done) => {
            const params: DotContentSearchParams = {
                page: 2,
                perPage: 20
            };

            spectator.service.search(params).subscribe((result) => {
                expect(result).toEqual(mockData);
                done();
            });

            const req = spectator.expectOne('/api/v1/content/search', HttpMethod.POST);
            expect(req.request.body).toEqual({
                page: 2,
                perPage: 20
            });

            req.flush({
                entity: mockData
            });
        });

        it('should call the search endpoint with an empty object when no parameters are provided', (done) => {
            const params: DotContentSearchParams = {};

            spectator.service.search(params).subscribe((result) => {
                expect(result).toEqual(mockData);
                done();
            });

            const req = spectator.expectOne('/api/v1/content/search', HttpMethod.POST);
            expect(req.request.body).toEqual({});

            req.flush({
                entity: mockData
            });
        });

        it('should handle empty contentlets array in response', () => {
            spectator.service.search({ globalSearch: 'nonexistent' }).subscribe((result) => {
                expect(result).toEqual({
                    jsonObjectView: {
                        contentlets: []
                    },
                    resultsSize: 0
                });
            });

            const req = spectator.expectOne('/api/v1/content/search', HttpMethod.POST);
            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: []
                    },
                    resultsSize: 0
                }
            });
        });

        it('should propagate error when the request fails', (done) => {
            const errorResponse = new HttpErrorResponse({
                error: 'test error',
                status: 500,
                statusText: 'Server Error'
            });

            spectator.service.search({ globalSearch: 'test' }).subscribe({
                next: () => fail('should have failed with the error'),
                error: (error) => {
                    expect(error.status).toBe(500);
                    done();
                }
            });

            const req = spectator.expectOne('/api/v1/content/search', HttpMethod.POST);
            req.flush('test error', errorResponse);
        });
    });
});
