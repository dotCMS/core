import { QueryBuilder } from './sdk-query-builder';

describe('QueryBuilder', () => {
    let queryBuilder: QueryBuilder;

    beforeEach(() => {
        queryBuilder = new QueryBuilder();
    });

    it('should return a query with a simple term', () => {
        const queryForBlogs = queryBuilder.field('contentType').term('Blog').build();

        expect(queryForBlogs).toBe('+contentType:Blog');
    });

    it('should return a query with multiple fields with a simple term ', () => {
        const queryForBlogsInSuperCoolSite = queryBuilder
            .field('contentType')
            .term('Blog')
            .field('conhost')
            .term('my-super-cool-site')
            .build();

        expect(queryForBlogsInSuperCoolSite).toBe('+contentType:Blog +conhost:my-super-cool-site');
    });

    it("should return a query with an 'OR' operand", () => {
        const queryForBlogsOrArticles = queryBuilder
            .field('contentType')
            .term('Blog')
            .or()
            .term('Article')
            .build();

        expect(queryForBlogsOrArticles).toBe('+contentType:Blog OR Article');
    });

    it("should return a query with an 'AND' operand", () => {
        const queryForBlogsAndArticles = queryBuilder
            .field('contentType')
            .term('Blog')
            .and()
            .term('Article')
            .build();

        expect(queryForBlogsAndArticles).toBe('+contentType:Blog AND Article');
    });

    it("should return a query with a 'NOT' operand", () => {
        const queryForSkiingTripsNotInSwissAlps = queryBuilder
            .field('summary')
            .term('Skiing trip')
            .not()
            .term('Swiss Alps')
            .build();

        expect(queryForSkiingTripsNotInSwissAlps).toBe('+summary:"Skiing trip" NOT "Swiss Alps"');
    });

    it('should return a query with an exclusion field', () => {
        const queryForFootballBlogsWithoutMessi = queryBuilder
            .field('contentType')
            .term('Blog')
            .field('title')
            .term('Football')
            .excludeField('summary')
            .term('Lionel Messi')
            .build();

        expect(queryForFootballBlogsWithoutMessi).toBe(
            '+contentType:Blog +title:Football -summary:"Lionel Messi"'
        );
    });

    it('should build a raw query from the query builder', () => {
        const queryForBlogs = queryBuilder
            .raw('+summary:Snowboard')
            .not()
            .term('Swiss Alps')
            .field('contentType')
            .term('Blog')
            .build();

        expect(queryForBlogs).toBe('+summary:Snowboard NOT "Swiss Alps" +contentType:Blog');
    });

    it('should return a query with a raw query appended', () => {
        const queryForBlogs = queryBuilder
            .field('contentType')
            .term('Blog')
            .raw('+summary:Snowboard')
            .not()
            .term('Swiss Alps')
            .build();

        expect(queryForBlogs).toBe('+contentType:Blog +summary:Snowboard NOT "Swiss Alps"');
    });

    it('should return a query with a raw query created with a queryBuilder appended and a term', () => {
        const anotherQueryBuilder = new QueryBuilder();

        const snowboardInCanada = anotherQueryBuilder
            .field('summary')
            .term('Snowboard')
            .field('country')
            .term('Canada')
            .build();

        const queryForBlogs = queryBuilder
            .field('contentType')
            .term('Blog')
            .raw(snowboardInCanada)
            .build();

        expect(queryForBlogs).toBe('+contentType:Blog +summary:Snowboard +country:Canada');
    });

    it('should return a query with all possible combinations', () => {
        const blogOrActivity = queryBuilder.field('contentType').term('Blog').or().term('Activity');

        const customIdSiteOrCoolSite = blogOrActivity
            .field('conhost')
            .term('48190c8c-42c4-46af-8d1a-0cd5db894797')
            .or()
            .term('cool-site');

        const englishAndSpanish = customIdSiteOrCoolSite
            .field('languageId')
            .term('1')
            .and()
            .term('2');

        const notDeleted = englishAndSpanish.field('deleted').term('false');

        const currentlyWorking = notDeleted.field('working').term('true');

        const defaultVariant = currentlyWorking.field('variant').term('default');

        const snowboardOutsideSwissAlps = defaultVariant
            .field('title')
            .term('Snowboard')
            .excludeField('summary')
            .term('Swiss Alps');

        const writtenByJohnDoe = snowboardOutsideSwissAlps.field('authors').term('John Doe');

        const withoutJaneDoeHelp = writtenByJohnDoe.not().term('Jane Doe');

        const query = withoutJaneDoeHelp.build();

        expect(query).toBe(
            '+contentType:Blog OR Activity +conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 OR cool-site +languageId:1 AND 2 +deleted:false +working:true +variant:default +title:Snowboard -summary:"Swiss Alps" +authors:"John Doe" NOT "Jane Doe"'
        );
    });
});
