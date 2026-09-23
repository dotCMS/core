import { PAGE_PLACE_CONTENT_ENDPOINTS } from './definitions/page-place-content';
import { PAGE_VERIFY_ENDPOINTS } from './definitions/page-verify';
import {
    CONTEXT_ENDPOINTS,
    matchesEndpoint,
    modelChosenPolicy,
    toolPolicy,
    unlistedCalls
} from './endpoints';

describe('matchesEndpoint', () => {
    it('matches an exact endpoint, method included', () => {
        expect(matchesEndpoint(['GET /api/v1/site'], { method: 'GET', path: '/api/v1/site' })).toBe(
            true
        );
        expect(
            matchesEndpoint(['GET /api/v1/site'], { method: 'DELETE', path: '/api/v1/site' })
        ).toBe(false);
        expect(
            matchesEndpoint(['GET /api/v1/site'], { method: 'GET', path: '/api/v1/site/x' })
        ).toBe(false);
    });

    it('lets {param} stand for exactly one path segment', () => {
        const endpoints = ['POST /api/v1/page/{pageId}/content'] as const;

        expect(
            matchesEndpoint(endpoints, { method: 'POST', path: '/api/v1/page/abc-123/content' })
        ).toBe(true);
        expect(
            matchesEndpoint(endpoints, { method: 'POST', path: '/api/v1/page/a/b/content' })
        ).toBe(false);
        expect(matchesEndpoint(endpoints, { method: 'POST', path: '/api/v1/page//content' })).toBe(
            false
        );
    });

    it('lets a trailing /** stand for anything below the prefix, including the root page', () => {
        const endpoints = ['GET /api/v1/page/render/**'] as const;

        for (const path of [
            '/api/v1/page/render',
            '/api/v1/page/render/',
            '/api/v1/page/render/blog/my-post'
        ]) {
            expect(matchesEndpoint(endpoints, { method: 'GET', path })).toBe(true);
        }
        expect(matchesEndpoint(endpoints, { method: 'GET', path: '/api/v1/page/rendered' })).toBe(
            false
        );
    });

    it('treats pattern characters in a path literally', () => {
        expect(
            matchesEndpoint(['POST /api/content/_search'], {
                method: 'POST',
                path: '/api/contentX_search'
            })
        ).toBe(false);
    });
});

describe('unlistedCalls', () => {
    it('reports calls outside the list, resolved the way the request core resolves them', () => {
        const calls = [
            { path: '/api/v1/page/render/about-us' },
            { method: 'post', path: '/api/v1/page/render/../../workflow/actions/x/fire' },
            { method: 'DELETE', path: '/api/v1/site/abc' }
        ];

        expect(unlistedCalls(PAGE_VERIFY_ENDPOINTS, calls)).toEqual([
            'POST /api/v1/workflow/actions/x/fire',
            'DELETE /api/v1/site/abc'
        ]);
    });
});

describe('toolPolicy', () => {
    const policy = toolPolicy('page_place_content', PAGE_PLACE_CONTENT_ENDPOINTS, 'A hint.');

    it('allows what the tool owns', () => {
        expect(policy({ method: 'GET', path: '/api/v1/page/json/about-us' })).toBe(true);
    });

    it('refuses everything else, and says which tool and which request', () => {
        expect(() => policy({ method: 'DELETE', path: '/api/v1/page/json/about-us' })).toThrow(
            'The page_place_content tool can only reach the endpoints it owns, and DELETE ' +
                '/api/v1/page/json/about-us is not one of them. A hint.'
        );
    });
});

describe('modelChosenPolicy', () => {
    it('applies no policy at all when the consumer set no allow-list', () => {
        expect(modelChosenPolicy(undefined)).toBeUndefined();
    });

    it('bounds the model by the allow-list, while always permitting the context reads', () => {
        const policy = modelChosenPolicy(['/api/v1/content']);

        expect(policy?.({ method: 'GET', path: '/api/v1/content/abc' })).toBe(true);
        expect(policy?.({ method: 'DELETE', path: '/api/v1/site/abc' })).toBe(false);
        for (const endpoint of CONTEXT_ENDPOINTS) {
            const [method, path] = endpoint.split(' ');
            expect(policy?.({ method, path })).toBe(true);
        }
    });

    it('does not widen the context reads to other methods', () => {
        const policy = modelChosenPolicy(['/api/v1/content']);

        expect(policy?.({ method: 'DELETE', path: '/api/v1/site' })).toBe(false);
    });
});
