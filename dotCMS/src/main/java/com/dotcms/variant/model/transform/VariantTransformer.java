package com.dotcms.variant.model.transform;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.DotStateException;
import io.vavr.Lazy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transformer for {@link Variant}
 */
public class VariantTransformer implements DBTransformer {

    private final List<Map<String, Object>> initList;
    final Lazy<List<Variant>> list = Lazy.of(() -> transform());

    public VariantTransformer(final List<Map<String, Object>> initList){
        this.initList = initList;
    }

    public VariantTransformer(final Map<String, Object> initObject){
        this.initList = CollectionsUtils.list(initObject);
    }

    @Override
    public List<Variant> asList() {
        return list.get();
    }

    public Variant from() throws DotStateException {
        return transform(initList.get(0));
    }

    private List<Variant> transform() {
        return initList.stream().map(variantMap -> transform(variantMap))
                .collect(Collectors.toList());
    }

    private Variant transform(final Map<String, Object> variantMap) {
        return Variant.builder()
                .description(Optional.ofNullable((String) variantMap.get("description")))
                .name(variantMap.get("name").toString())
                .archived(ConversionUtils.toBooleanFromDb(variantMap.get("archived")))
                .build();
    }
}
