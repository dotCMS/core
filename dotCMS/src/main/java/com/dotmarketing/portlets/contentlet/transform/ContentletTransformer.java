package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
public class ContentletTransformer implements DBTransformer {
    final List<Contentlet> list;


    public ContentletTransformer(final List<Map<String, Object>> initList){
        final List<Contentlet> newList = new ArrayList<>();
        if (initList != null){
            for(final Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Contentlet> asList() {
        return this.list;
    }

    @NotNull
    private static Contentlet transform(final Map<String, Object> map)  {
        final Contentlet contentlet;
        contentlet = new Contentlet();
        contentlet.setInode((String) map.get("inode"));
        contentlet.setModDate((Date) map.get("mod_date"));
        contentlet.setModUser((String) map.get("mod_user"));
        contentlet.setSortOrder(ConversionUtils.toInt(map.get("sort_order"),0));
        contentlet.setContentTypeId((String) map.get("structure_inode"));
        contentlet.setLastReview((Date) map.get("last_review"));
        contentlet.setNextReview((Date) map.get("next_review"));
        contentlet.setReviewInterval((String) map.get("review_interval"));

        final List disabledWysiwyg = new ArrayList();
        disabledWysiwyg.add(map.get("disabled_wysiwyg"));
        contentlet.setDisabledWysiwyg(disabledWysiwyg);
        contentlet.setIdentifier((String) map.get("identifier"));
        contentlet.setLanguageId(ConversionUtils.toLong(map.get("language_id"), 0L));

        String key;
        List<String> ignoredFields = new ArrayList<>();
        ignoredFields.add("inode");
        ignoredFields.add("mod_date");
        ignoredFields.add("mod_user");
        ignoredFields.add("sort_order");
        ignoredFields.add("structure_inode");
        ignoredFields.add("last_review");
        ignoredFields.add("next_review");
        ignoredFields.add("review_interval");
        ignoredFields.add("disabled_wysiwyg");

        for (Entry<String, Object> property: map.entrySet()){

            key = property.getKey();
            if (!ignoredFields.contains(key)) {
                contentlet.setProperty(property.getKey(), property.getValue());
            }

        }
        return contentlet;
    }
}

