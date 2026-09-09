import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotAiIndex } from '@dotcms/dotcms-models';

import { DotAiEmbeddingsService } from './dot-ai-embeddings.service';
import { AI_API_ENDPOINT } from './dot-ai.constants';

/**
 * Every conversion this service owns is asserted here, because this is the only place each
 * one is allowed to happen. Shapes verified against the running backend:
 *   - `indexCount` is wrapped one level and keyed by index name
 *   - `contentTypes` is comma-joined at the SQL level (STRING_AGG)
 *   - deletes answer `{deleted:N}`, rebuild answers `{created:true}`
 */
describe('DotAiEmbeddingsService', () => {
    let spectator: SpectatorHttp<DotAiEmbeddingsService>;

    const createHttp = createHttpFactory(DotAiEmbeddingsService);

    const EMBEDDINGS_URL = `${AI_API_ENDPOINT}/embeddings`;

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('getIndexes', () => {
        it('should unwrap indexCount, fold the key into name and split contentTypes', () => {
            let result: DotAiIndex[];
            spectator.service.getIndexes().subscribe((r) => (result = r));

            spectator.expectOne(`${EMBEDDINGS_URL}/indexCount`, HttpMethod.GET).flush({
                indexCount: {
                    default: {
                        fragments: 10,
                        contents: 4,
                        tokenTotal: 2000,
                        tokensPerChunk: 200,
                        contentTypes: 'Blog,News'
                    }
                }
            });

            expect(result).toEqual([
                {
                    name: 'default',
                    fragments: 10,
                    contents: 4,
                    tokenTotal: 2000,
                    tokensPerChunk: 200,
                    contentTypes: ['Blog', 'News']
                }
            ]);
        });

        it('should return an empty array when there are no indexes', () => {
            let result: DotAiIndex[];
            spectator.service.getIndexes().subscribe((r) => (result = r));

            spectator
                .expectOne(`${EMBEDDINGS_URL}/indexCount`, HttpMethod.GET)
                .flush({ indexCount: {} });

            expect(result).toEqual([]);
        });

        it('should yield an empty contentTypes array when the server sends none', () => {
            let result: DotAiIndex[];
            spectator.service.getIndexes().subscribe((r) => (result = r));

            spectator.expectOne(`${EMBEDDINGS_URL}/indexCount`, HttpMethod.GET).flush({
                indexCount: {
                    cache: {
                        fragments: 1,
                        contents: 1,
                        tokenTotal: 1,
                        tokensPerChunk: 1,
                        contentTypes: null
                    }
                }
            });

            expect(result[0].contentTypes).toEqual([]);
        });
    });

    describe('buildIndex', () => {
        it('should POST an EmbeddingsForm body, not a CompletionsForm', () => {
            const form = {
                indexName: 'blogs',
                query: '+contentType:blog',
                fields: 'title,body',
                velocityTemplate: '$title'
            };

            spectator.service.buildIndex(form).subscribe();

            const req = spectator.expectOne(EMBEDDINGS_URL, HttpMethod.POST);
            expect(JSON.parse(req.request.body)).toEqual(form);
        });

        it('should return the build result that seeds BUILDING', () => {
            let result: DotAiIndex[];
            spectator.service
                .buildIndex({ indexName: 'blogs', query: '+contentType:blog' })
                .subscribe((r) => (result = r));

            spectator
                .expectOne(EMBEDDINGS_URL, HttpMethod.POST)
                .flush({ timeToEmbeddings: '2s', totalToEmbed: 12, indexName: 'blogs' });

            expect(result).toEqual({
                timeToEmbeddings: '2s',
                totalToEmbed: 12,
                indexName: 'blogs'
            });
        });
    });

    describe('deleteIndex / deleteFromIndex', () => {
        it('should DELETE with a body carrying only the index name', () => {
            let result: number | undefined;
            spectator.service.deleteIndex('blogs').subscribe((r) => (result = r));

            const req = spectator.expectOne(EMBEDDINGS_URL, HttpMethod.DELETE);
            // A DELETE that silently drops its body is the easy failure here.
            expect(req.request.body).toBeTruthy();
            expect(JSON.parse(req.request.body)).toEqual({ indexName: 'blogs' });
            req.flush({ deleted: 7 });

            expect(result).toBe(7);
        });

        it('should DELETE with a deleteQuery when removing matching content', () => {
            let result: number | undefined;
            spectator.service
                .deleteFromIndex('blogs', '+contentType:blog')
                .subscribe((r) => (result = r));

            const req = spectator.expectOne(EMBEDDINGS_URL, HttpMethod.DELETE);
            expect(JSON.parse(req.request.body)).toEqual({
                indexName: 'blogs',
                deleteQuery: '+contentType:blog'
            });
            req.flush({ deleted: 3 });

            expect(result).toBe(3);
        });

        it('should map a missing deleted count to 0', () => {
            let result: number | undefined;
            spectator.service.deleteIndex('blogs').subscribe((r) => (result = r));

            spectator.expectOne(EMBEDDINGS_URL, HttpMethod.DELETE).flush({});

            expect(result).toBe(0);
        });
    });

    describe('rebuildEmbeddingsDb', () => {
        it('should DELETE /db and map {created:true} to a boolean', () => {
            let result: boolean | undefined;
            spectator.service.rebuildEmbeddingsDb().subscribe((r) => (result = r));

            spectator.expectOne(`${EMBEDDINGS_URL}/db`, HttpMethod.DELETE).flush({ created: true });

            expect(result).toBe(true);
        });
    });
});
