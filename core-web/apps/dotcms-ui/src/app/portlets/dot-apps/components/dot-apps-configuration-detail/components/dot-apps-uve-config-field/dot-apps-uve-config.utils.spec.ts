import {
    createUveRoute,
    hasUveRouteErrors,
    isHttpUrl,
    parseUveConfig,
    serializeUveConfig,
    validateUveRoute
} from './dot-apps-uve-config.utils';

const SAMPLE = {
    config: [
        { pattern: '/blogs/(.*)', url: 'https://myspa.blogs.com:3000' },
        {
            pattern: '.*',
            url: 'https://myspa.com:3000',
            options: { allowedDevURLs: ['http://localhost:3000'] }
        }
    ]
};

describe('dot-apps-uve-config utils', () => {
    describe('parseUveConfig', () => {
        it('should read every route in order', () => {
            const routes = parseUveConfig(JSON.stringify(SAMPLE));

            expect(
                routes.map(({ pattern, url, allowedDevURLs }) => ({ pattern, url, allowedDevURLs }))
            ).toEqual([
                { pattern: '/blogs/(.*)', url: 'https://myspa.blogs.com:3000', allowedDevURLs: [] },
                {
                    pattern: '.*',
                    url: 'https://myspa.com:3000',
                    allowedDevURLs: ['http://localhost:3000']
                }
            ]);
        });

        it('should return no routes for an empty value', () => {
            expect(parseUveConfig('')).toEqual([]);
        });

        it.each([
            ['broken JSON', '{"config": ['],
            ['no config list', '{"routes": []}'],
            ['non-object entry', '{"config": ["x"]}'],
            ['non-string pattern', '{"config": [{"pattern": 1, "url": "https://a.com"}]}'],
            [
                'non-list dev URLs',
                '{"config": [{"pattern": ".*", "url": "https://a.com", "options": {"allowedDevURLs": "x"}}]}'
            ]
        ])('should return null for %s', (_, value) => {
            expect(parseUveConfig(value)).toBeNull();
        });
    });

    describe('serializeUveConfig', () => {
        it('should round-trip the sample', () => {
            const routes = parseUveConfig(JSON.stringify(SAMPLE));

            expect(JSON.parse(serializeUveConfig(routes))).toEqual(SAMPLE);
        });

        it('should keep keys the form does not edit', () => {
            const value = JSON.stringify({
                config: [
                    {
                        pattern: '.*',
                        url: 'https://a.com',
                        custom: true,
                        options: { other: 1, allowedDevURLs: ['http://localhost:3000'] }
                    }
                ]
            });

            expect(JSON.parse(serializeUveConfig(parseUveConfig(value)))).toEqual(
                JSON.parse(value)
            );
        });

        it('should drop blank dev URLs and an empty options object', () => {
            const route = { ...createUveRoute('.*'), url: 'https://a.com', allowedDevURLs: ['  '] };

            expect(JSON.parse(serializeUveConfig([route]))).toEqual({
                config: [{ pattern: '.*', url: 'https://a.com' }]
            });
        });
    });

    describe('validateUveRoute', () => {
        it('should accept a complete route', () => {
            const route = {
                ...createUveRoute('/blogs/(.*)'),
                url: 'https://a.com',
                allowedDevURLs: ['http://localhost:3000']
            };

            expect(hasUveRouteErrors(validateUveRoute(route))).toBe(false);
        });

        it('should require pattern and URL', () => {
            const errors = validateUveRoute(createUveRoute());

            expect(errors.pattern).toBe('apps.uve.route.error.required');
            expect(errors.url).toBe('apps.uve.route.error.required');
        });

        it('should reject an invalid RegEx, URL and dev URL', () => {
            const route = {
                ...createUveRoute('(['),
                url: 'myspa.com',
                allowedDevURLs: ['localhost']
            };
            const errors = validateUveRoute(route);

            expect(errors.pattern).toBe('apps.uve.route.error.pattern');
            expect(errors.url).toBe('apps.uve.route.error.url');
            expect(errors.allowedDevURLs).toEqual(['apps.uve.route.error.url']);
        });
    });

    it('isHttpUrl should only accept http and https', () => {
        expect(isHttpUrl('https://a.com')).toBe(true);
        expect(isHttpUrl('http://localhost:3000')).toBe(true);
        expect(isHttpUrl('ftp://a.com')).toBe(false);
        expect(isHttpUrl('a.com')).toBe(false);
    });
});
