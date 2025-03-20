import { QueryBuilder } from './query';

describe('QueryBuilder', () => {
    let queryBuilder: QueryBuilder;

    beforeEach(() => {
        queryBuilder = new QueryBuilder();
    });

    it('should return a query with a simple term', () => {
        const queryForBlogs = queryBuilder.field('contentType').equals('Blog').build();

        expect(queryForBlogs).toBe('+contentType:Blog');
    });

    it('should return a query with multiple fields with a simple term ', () => {
        const queryForBlogsInSuperCoolSite = queryBuilder
            .field('contentType')
            .equals('Blog')
            .field('conhost')
            .equals('my-super-cool-site')
            .build();

        expect(queryForBlogsInSuperCoolSite).toBe('+contentType:Blog +conhost:my-super-cool-site');
    });

    it('should return a query with an "OR" operand', () => {
        const queryForBlogsOrArticles = queryBuilder
            .field('contentType')
            .equals('Blog')
            .or()
            .equals('Article')
            .build();

        expect(queryForBlogsOrArticles).toBe('+contentType:Blog OR Article');
    });

    it('should return a query with an "AND" operand', () => {
        const queryForBlogsAndArticles = queryBuilder
            .field('contentType')
            .equals('Blog')
            .and()
            .equals('Article')
            .build();

        expect(queryForBlogsAndArticles).toBe('+contentType:Blog AND Article');
    });

    it('should return a query with a "NOT" operand', () => {
        const queryForSkiingTripsNotInSwissAlps = queryBuilder
            .field('summary')
            .equals('Skiing trip')
            .not()
            .equals('Swiss Alps')
            .build();

        expect(queryForSkiingTripsNotInSwissAlps).toBe(`+summary:'Skiing trip' NOT 'Swiss Alps'`);
    });

    it('should return a query with an exclusion field', () => {
        const queryForFootballBlogsWithoutMessi = queryBuilder
            .field('contentType')
            .equals('Blog')
            .field('title')
            .equals('Football')
            .excludeField('summary')
            .equals('Lionel Messi')
            .build();

        expect(queryForFootballBlogsWithoutMessi).toBe(
            `+contentType:Blog +title:Football -summary:'Lionel Messi'`
        );
    });

    it('should build a raw query from the query builder', () => {
        const queryForBlogs = queryBuilder
            .raw('+summary:Snowboard')
            .not()
            .equals('Swiss Alps')
            .field('contentType')
            .equals('Blog')
            .build();

        expect(queryForBlogs).toBe(`+summary:Snowboard NOT 'Swiss Alps' +contentType:Blog`);
    });

    it('should return a query with a raw query appended', () => {
        const queryForBlogs = queryBuilder
            .field('contentType')
            .equals('Blog')
            .raw('+summary:Snowboard')
            .not()
            .equals('Swiss Alps')
            .build();

        expect(queryForBlogs).toBe(`+contentType:Blog +summary:Snowboard NOT 'Swiss Alps'`);
    });

    it('should return a query with a raw query created with a queryBuilder appended and a term', () => {
        const anotherQueryBuilder = new QueryBuilder();

        const snowboardInCanada = anotherQueryBuilder
            .field('summary')
            .equals('Snowboard')
            .field('country')
            .equals('Canada')
            .build();

        const queryForBlogs = queryBuilder
            .field('contentType')
            .equals('Blog')
            .raw(snowboardInCanada)
            .build();

        expect(queryForBlogs).toBe('+contentType:Blog +summary:Snowboard +country:Canada');
    });

    it('should return a query with all possible combinations', () => {
        const blogOrActivity = queryBuilder
            .field('contentType')
            .equals('Blog')
            .or()
            .equals('Activity');

        const customIdSiteOrCoolSite = blogOrActivity
            .field('conhost')
            .equals('48190c8c-42c4-46af-8d1a-0cd5db894797')
            .or()
            .equals('cool-site');

        const englishAndSpanish = customIdSiteOrCoolSite
            .field('languageId')
            .equals('1')
            .and()
            .equals('2');

        const notDeleted = englishAndSpanish.field('deleted').equals('false');

        const currentlyWorking = notDeleted.field('working').equals('true');

        const defaultVariant = currentlyWorking.field('variant').equals('default');

        const snowboardOutsideSwissAlps = defaultVariant
            .field('title')
            .equals('Snowboard')
            .excludeField('summary')
            .equals('Swiss Alps');

        const writtenByJohnDoe = snowboardOutsideSwissAlps.field('authors').equals('John Doe');

        const withoutJaneDoeHelp = writtenByJohnDoe.not().equals('Jane Doe');

        const query = withoutJaneDoeHelp.build();

        expect(query).toBe(
            `+contentType:Blog OR Activity +conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 OR cool-site +languageId:1 AND 2 +deleted:false +working:true +variant:default +title:Snowboard -summary:'Swiss Alps' +authors:'John Doe' NOT 'Jane Doe'`
        );
    });
});
