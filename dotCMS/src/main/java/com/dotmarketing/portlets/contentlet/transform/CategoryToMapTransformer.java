package com.dotmarketing.portlets.contentlet.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * FieldsToMapTransformer that converts contentlet objects into Maps
 */
public class CategoryToMapTransformer implements FieldsToMapTransformer {
    final Map<String, Object> mapOfMaps;
    final List<Category> cats ;


    public CategoryToMapTransformer(final Contentlet con, final User user) {
        if (con.getInode() == null) {
            throw new DotStateException("Contentlet needs an inode to get fields");
        }
        try {
            this.cats = APILocator.getCategoryAPI().getParents(con, user, true);
        } catch (DotDataException | DotSecurityException e) {
            throw new DotStateException(e);
        }
        final Map<String, Object> newMap = new HashMap<>();
        if(cats!=null && !cats.isEmpty()) {

            for (final Field field : con.getContentType().fields()) {
                if (field instanceof CategoryField) {
                    newMap.put(field.variable(), transform(field, con, user));
                }
            }
            
        }

        this.mapOfMaps = newMap;
    }

    
    @Override
    public Map<String, Object> asMap() {
        return this.mapOfMaps;
    }



    @NotNull
    private Map<String, Object> transform(final Field field, final Contentlet con, final User user) {

        final Map<String, Object> map = new HashMap<>();

        try {

            final Category parentCategory        = APILocator.getCategoryAPI().find(field.values(), user, true);
            final List<Map<String, Object>> childCategories = new ArrayList<>();

            if(parentCategory != null) {
                for (Category category : cats) {
                    if (APILocator.getCategoryAPI().isParent(category, parentCategory, user,true)) {
                        childCategories.add(transform(category));
                    }
                }
            }
            map.put("categories", childCategories);
            
        } catch (Exception e) {
            throw new DotStateException(String.format("Unable to get the Categories for given contentlet with inode= %s", con.getInode()), e);

        }
        


        return map;
    }
    @NotNull
    private Map<String, Object> transform(final Category cat) {

        final Map<String, Object> map = new HashMap<>();

        map.put("inode", cat.getInode());
        map.put("active", cat.isActive());
        map.put("name", cat.getCategoryName());


        map.put("key", cat.getKey());
        map.put("keywords", cat.getKeywords());
        map.put("velocityVar", cat.getCategoryVelocityVarName());
        return map;
    }
    
}

