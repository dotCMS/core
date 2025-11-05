package com.dotmarketing.beans.transform;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.postgresql.util.PGobject;

public class MultiTreeTransformer implements DBTransformer<MultiTree> {

    //MultiTree fields
    private static final String CHILD = "child";
    private static final String PARENT1 = "parent1";
    private static final String PARENT2 = "parent2";
    private static final String RELATION_TYPE = "relation_type";
    private static final String TREE_ORDER = "tree_order";
    private static final String PERSONALIZATION = "personalization";
    private static final String VARIANT = "variant_id";
    private static final String STYLE_PROPERTIES = "style_properties";

    private final ArrayList<MultiTree> list = new ArrayList<>();

    @Override
    public List<MultiTree> asList() {
        return this.list;
    }

    public MultiTreeTransformer(final List<Map<String, Object>> initList) {
        if (initList != null) {
            for (final Map<String, Object> map : initList) {
                this.list.add(transform(map));
            }
        }
    }

    private static MultiTree transform(final Map<String, Object> map) {
        final ObjectMapper jsonMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

        final MultiTree multiTree = new MultiTree();
        multiTree.setContentlet((String) map.get(CHILD));
        multiTree.setHtmlPage((String) map.get(PARENT1));
        multiTree.setContainer((String) map.get(PARENT2));
        multiTree.setPersonalization((String) map.get(PERSONALIZATION));
        if (UtilMethods.isSet(map.get(RELATION_TYPE))) {
            multiTree.setRelationType((String) map.get(RELATION_TYPE));
        }
        if (UtilMethods.isSet(map.get(TREE_ORDER))) {
            multiTree.setTreeOrder(ConversionUtils.toInt(map.get(TREE_ORDER), 0));
        }
        if (UtilMethods.isSet(map.get(VARIANT))) {
            multiTree.setVariantId(map.get(VARIANT).toString());
        }
        if  (UtilMethods.isSet(map.get(STYLE_PROPERTIES))) {
            multiTree.setStyleProperties(Try.of(() -> jsonMapper
                            .readValue(DbConnectionFactory.isPostgres()
                                    ? ((PGobject) map.get(STYLE_PROPERTIES)).getValue()
                                    : (String) map.get(STYLE_PROPERTIES), HashMap.class))
                    .getOrElse(new HashMap<String, Object>()));
        }
        return multiTree;
    }
}