import { buildContentletByTypeQuery } from './slash-menu-catalog';

const PREFIX = '+contentType:Blog +languageId:1 +deleted:false +working:true';

describe('buildContentletByTypeQuery', () => {
    it('lists the type (title-boosted) when there is no filter', () => {
        expect(buildContentletByTypeQuery('Blog', 1)).toBe(`${PREFIX} +catchall:** title:''^15`);
    });

    it('treats a UUID as an exact identifier (escaped, no wildcards / title boost)', () => {
        const uuid = '6a8102b5-fdb0-4ad5-9a5d-e982bcdb54c8';
        expect(buildContentletByTypeQuery('Blog', 1, uuid)).toBe(
            `${PREFIX} +catchall:6a8102b5\\-fdb0\\-4ad5\\-9a5d\\-e982bcdb54c8`
        );
    });

    it('builds a wildcard clause plus a title-phrase boost for a single word', () => {
        expect(buildContentletByTypeQuery('Blog', 1, 'beach')).toBe(
            `${PREFIX} +catchall:*beach* title:"beach"^15`
        );
    });

    it('tokenizes a multi-word filter into one mandatory wildcard clause per word', () => {
        expect(buildContentletByTypeQuery('Blog', 1, 'blue lagoon')).toBe(
            `${PREFIX} +catchall:*blue* +catchall:*lagoon* title:"blue lagoon"^15`
        );
    });

    it('splits hyphenated tokens (the analyzer separates on hyphens) but keeps the title phrase', () => {
        expect(buildContentletByTypeQuery('Blog', 1, 'cross-country')).toBe(
            `${PREFIX} +catchall:*cross* +catchall:*country* title:"cross\\-country"^15`
        );
    });

    it('escapes Lucene special characters in user input', () => {
        expect(buildContentletByTypeQuery('Blog', 1, 'a&b')).toBe(
            `${PREFIX} +catchall:*a\\&b* title:"a\\&b"^15`
        );
    });

    it('trims surrounding whitespace before tokenizing', () => {
        expect(buildContentletByTypeQuery('Blog', 1, '   beach   ')).toBe(
            `${PREFIX} +catchall:*beach* title:"beach"^15`
        );
    });
});
