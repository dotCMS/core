import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotAiSearchResponse } from '@dotcms/dotcms-models';

import { DotAiIndexNotFoundError, DotAiSearchService } from './dot-ai-search.service';
import { AI_API_ENDPOINT } from './dot-ai.constants';

describe('DotAiSearchService', () => {
    let spectator: SpectatorHttp<DotAiSearchService>;

    const createHttp = createHttpFactory(DotAiSearchService);

    const SEARCH_URL = `${AI_API_ENDPOINT}/search`;

    const form = {
        prompt: 'what is dotCMS',
        indexName: 'default',
        threshold: 0.25,
        operator: 'innerProduct' as const,
        searchLimit: 50,
        searchOffset: 0
    };

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should POST a CompletionsForm body rather than query params', () => {
        spectator.service.semanticSearch(form).subscribe();

        const req = spectator.expectOne(SEARCH_URL, HttpMethod.POST);
        // The SDK's AISearch uses GET + query params; the portlet must not drift into that.
        expect(req.request.params.keys()).toHaveLength(0);
        expect(JSON.parse(req.request.body)).toMatchObject({
            prompt: 'what is dotCMS',
            operator: 'innerProduct'
        });
    });

    it('should map dotCMSResults into typed results with their matches', () => {
        let result: DotAiSearchResponse;
        spectator.service.semanticSearch(form).subscribe((r) => (result = r));

        spectator.expectOne(SEARCH_URL, HttpMethod.POST).flush({
            timeToEmbeddings: '120ms',
            total: 1,
            count: 1,
            query: 'what is dotCMS',
            threshold: 0.25,
            operator: '<#>',
            offset: 0,
            limit: 50,
            dotCMSResults: [
                {
                    identifier: 'id-1',
                    inode: 'inode-1',
                    title: 'About dotCMS',
                    contentType: 'Blog',
                    modDate: '2026-01-01',
                    matches: [{ distance: 0.12, extractedText: 'dotCMS is a CMS' }]
                }
            ]
        });

        expect(result.count).toBe(1);
        expect(result.timeToEmbeddings).toBe('120ms');
        expect(result.results[0]).toMatchObject({
            identifier: 'id-1',
            title: 'About dotCMS',
            contentType: 'Blog',
            modDate: '2026-01-01'
        });
        expect(result.results[0].matches[0].distance).toBe(0.12);
    });

    it('should map a result that carries no modDate without inventing one', () => {
        // One server fallback path emits {inode, identifier, title, language, index, contentType}
        // with no modDate at all. The row has to render without it.
        let result: DotAiSearchResponse;
        spectator.service.semanticSearch(form).subscribe((r) => (result = r));

        spectator.expectOne(SEARCH_URL, HttpMethod.POST).flush({
            count: 1,
            dotCMSResults: [
                {
                    identifier: 'id-1',
                    inode: 'inode-1',
                    title: 'No date',
                    contentType: 'Blog',
                    matches: []
                }
            ]
        });

        expect(result.results[0].modDate).toBeUndefined();
        expect(result.results[0].matches).toEqual([]);
    });

    it('should surface a missing index as a distinguishable error naming the index', () => {
        let caught: DotAiIndexNotFoundError;
        spectator.service.semanticSearch(form).subscribe({ error: (e) => (caught = e) });

        spectator
            .expectOne(SEARCH_URL, HttpMethod.POST)
            .flush({ error: "Index 'nope' not found" }, { status: 404, statusText: 'Not Found' });

        expect(caught.indexNotFound).toBe(true);
        expect(caught.indexName).toBe('nope');
    });

    it('should not report indexNotFound for other failures', () => {
        let caught: DotAiIndexNotFoundError;
        spectator.service.semanticSearch(form).subscribe({ error: (e) => (caught = e) });

        spectator
            .expectOne(SEARCH_URL, HttpMethod.POST)
            .flush({ error: 'boom' }, { status: 500, statusText: 'Server Error' });

        expect(caught.indexNotFound).toBeFalsy();
    });
});
