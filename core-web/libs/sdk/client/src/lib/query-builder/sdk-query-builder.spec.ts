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
        const queryForBlogsNotArticles = queryBuilder
            .field('summary')
            .term('Skiing trip')
            .not()
            .term('Swiss Alps')
            .build();

        expect(queryForBlogsNotArticles).toBe('+summary:"Skiing trip" NOT "Swiss Alps"');
    });

    it('should return a query with an exclusion field', () => {
        const queryForBlogsNotArticles = queryBuilder
            .field('contentType')
            .term('Blog')
            .field('title')
            .term('my title')
            .and()
            .excludeField('title')
            .term('his title')
            .build();

        expect(queryForBlogsNotArticles).toBe(
            '+contentType:Blog +title:"my title" AND -title:"his title"'
        );
    });

    it('should return a query with multiple operands and terms', () => {
        const query = queryBuilder
            .field('contentType')
            .term('Blog')
            .or()
            .term('Activity')
            .field('conhost')
            .term('48190c8c-42c4-46af-8d1a-0cd5db894797')
            .or()
            .term('cool-site')
            .field('languageId')
            .term('1')
            .and()
            .term('2')
            .field('deleted')
            .term('false')
            .field('working')
            .term('true')
            .field('variant')
            .term('default')
            .field('title')
            .term('Snowboard')
            .excludeField('summary')
            .term('Swiss Alps')
            .field('authors')
            .term('John Doe')
            .not()
            .term('Jane Doe')
            .build();

        expect(query).toBe(
            '+contentType:Blog OR Activity +conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 OR cool-site +languageId:1 AND 2 +deleted:false +working:true +variant:default +title:Snowboard -summary:"Swiss Alps" +authors:"John Doe" NOT "Jane Doe"'
        );
    });
});
