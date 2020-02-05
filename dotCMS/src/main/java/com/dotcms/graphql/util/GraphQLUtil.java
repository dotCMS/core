package com.dotcms.graphql.util;

import com.dotcms.graphql.InterfaceType;
import java.util.Set;

public class GraphQLUtil {

    public static Set<String> getFieldReservedWords() {
        return InterfaceType.RESERVED_GRAPHQL_FIELD_NAMES;
    }
}
