package com.dotcms.cost;

public class RequestPrices {


    public enum Price {

        // for testing
        FREE(0),
        ONE(1),
        TWO(2),
        THREE(3),
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8),
        NINE(9),
        TEN(10),
        TWENTY(20),
        THIRTY(30),
        TEN_THOUSAND(10000),


        /*
         * PRICED ITEMS
         *
         * A price is an order-of-magnitude estimate of the *resource-time* an operation
         * consumes — CPU, heap, or a request thread parked on a socket. Reading from an
         * in-memory cache is microseconds and is the unit; a remote HTTP call parks the
         * thread for hundreds of milliseconds and is priced ~250x that. What matters is
         * the ratio between tiers, not the absolute numbers.
         *
         *      1  in-memory cache read
         *      2  per-row hydrate / small alloc
         *      5  render a template fragment
         *     10  parse/compile CPU-bound, or one DB round trip
         *     25  one ES round trip, or a multi-query write
         *     50  heavy CPU + heap (image, Tika), or a write transaction
         *    100  one remote HTTP round trip
         *
         * Rule of thumb when adding one: what does this hold a thread (or a core, or a
         * chunk of heap) for? Price that, not how important the operation feels.
         *
         * On why a DB query is only 10x a cache read when it is ~1000x the latency: what
         * is being metered is capacity consumed *on this node*, not wall time. A query
         * parks the thread and burns the cycles on Postgres, so it costs this JVM far less
         * than its latency suggests. The Velocity-tenant profiles bear that out - template
         * rendering dominates those requests, not the DB frames. If a node ever runs out
         * of request threads before it runs out of CPU, that reasoning inverts and the DB
         * and HTTP tiers should go back up.
         */

        // --- tier 1: memory reads. Deliberately near-free; these must not dominate a
        // response just because it returned a lot of rows.
        COSTING_INIT(1),
        FILE_METADATA_FROM_CACHE(1),
        ES_CACHE(1),

        // --- tier 2: per-item work in memory (allocate, transform, hydrate one object).
        VELOCITY_BUILD_CONTEXT(2),
        BLOCK_EDITOR_HYDRATION(2),

        /*
         * Content is priced per contentlet, in two parts:
         *
         *   CONTENT_FROM_CACHE  base fee, charged for every contentlet asked for
         *   CONTENT_FROM_DB     surcharge, added only when we had to read it from Postgres
         *
         * So one contentlet costs 1 warm and 11 cold; a thousand cost 1,000 warm and 11,000
         * cold. The 10x gap is the point: caching is something customers control - through
         * cacheable containers and pages, cache TTLs, and how they shape their queries - so
         * it should visibly pay off in their bill.
         *
         * What is NOT priced is how we service a miss: batch size, query plan, how many SQL
         * statements it took. That is our implementation detail and a customer cannot
         * optimise against it, which is why there is no generic DB_QUERY price. The line is
         * between "did this need the database" (theirs) and "how did we ask the database"
         * (ours).
         */
        CONTENT_FROM_CACHE(1),

        // --- tier 5-10: CPU-bound work, no I/O.
        VELOCITY_MERGE(5),
        VELOCITY_PARSE(10),
        XSLT_PARSE(10),
        // Parsing and validating an incoming GraphQL document, before a single field is
        // fetched. This is the only charge that scales with the size of the *query* rather
        // than the size of the result, so a deeply nested document is not free.
        GRAPHQL_QUERY(10),

        // --- tier 10: one round trip to Postgres. Deliberately only 10x a cache read -
        // see the note above on capacity vs latency.
        //
        // There is intentionally no generic DB_QUERY here. Charging per SQL statement would
        // make a customer's cost depend on batch sizes and query plans they cannot see or
        // change. Whether the database was needed at all is theirs to influence and IS
        // priced; how many statements it took to satisfy is ours and is not.
        //
        // Surcharge added to CONTENT_FROM_CACHE when a contentlet had to be read from
        // Postgres. Charged in ESContentFactoryImpl.findContentlets (per missed row) and on
        // both findInDb variants (single-contentlet path).
        CONTENT_FROM_DB(10),
        FILE_METADATA_FROM_DB(10),
        CONTENT_GET_REFERENCES(10),
        LOGIN_USERNAME_PASS(10),
        CONTENT_CHECKOUT(10),
        WORKFLOW_ACTION_RUN(10),
        // Recurses through ContentHelper.addRelationshipsToJSON, one DB query per level;
        // a single ?depth= bump multiplies the work, so it is priced as the query it is.
        CONTENT_GET_RELATED(10),
        // Folder-tree walk, DB-blocked and recursive through NavResultHydrated.getChildren.
        NAV_BUILD(10),

        // --- tier 25: one round trip to Elasticsearch, or a multi-query write.
        ES_QUERY(25),
        ES_COUNT(25),
        // Blocking ES write on the request thread after every checkin.
        CONTENT_INDEX(25),
        CONTENT_MOVE(25),
        CONTENT_COPY(25),

        // --- tier 50: heavy CPU and heap, or a write transaction spanning many queries.
        // Decoding, resizing and re-encoding an image, or running Tika over a binary,
        // burns a core and a large buffer for a long time - it is not a "2".
        IMAGE_FILTER_TRANSFORM(50),
        FILE_METADATA_GENERATE(50),
        CONTENT_CHECKIN(50),
        CONTENT_DELETE(50),

        // --- tier 100: outbound HTTP. The thread is parked for the whole remote
        // round-trip, which is unbounded and outside our control.
        HTTP_FETCH(100),
        XML_FETCH_AND_PARSE(100),
        XSLT_FETCH_AND_PARSE(100);


        final public int price;

        Price(int price) {
            this.price = price;
        }
    }


}
