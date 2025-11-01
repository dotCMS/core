package com.dotcms.cost;

public class RequestPrices {


    public enum Price {

        // for testing
        FREE(0),
        ONE(1),
        TWO(2),
        THREE(3),
        FIVE(5),
        SEVEN(7),
        EIGHT(8),
        NINE(9),
        TEN(10),
        TWENTY(20),
        THIRTY(30),
        TEN_THOUSAND(10000),


        // PRICED ITEMS
        COSTING_INIT(1),
        CONTENT_FROM_CACHE(1),
        CONTENT_FROM_DB(3),
        CONTENT_GET_RELATED(1),
        CONTENT_GET_REFERENCES(2),
        CONTENT_MOVE(2),
        CONTENT_COPY(2),
        CONTENT_DELETE(2),
        CONTENT_CHECKOUT(1),
        CONTENT_CHECKIN(1),
        WORKFLOW_ACTION_RUN(1),
        BLOCK_EDITOR_HYDRATION(1),
        FILE_METADATA_GENERATE(3),
        FILE_METADATA_FROM_CACHE(1),
        FILE_METADATA_FROM_DB(3),
        HTTP_FETCH(4),
        VELOCITY_BUILD_CONTEXT(1),
        VELOCITY_MERGE(3),
        VELOCITY_PARSE(5),
        LOGIN_USERNAME_PASS(3),
        XML_FETCH_AND_PARSE(5),
        XSLT_PARSE(3),
        XSLT_FETCH_AND_PARSE(6),
        IMAGE_FILTER_TRANSFORM(2),
        ES_CACHE(1),
        ES_QUERY(3),
        ES_COUNT(3);


        final int price;

        Price(int price) {
            this.price = price;
        }
    }


}
