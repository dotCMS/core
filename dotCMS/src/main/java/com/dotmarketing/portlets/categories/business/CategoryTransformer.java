package com.dotmarketing.portlets.categories.business;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.categories.model.Category;
import java.util.List;
import java.util.Map;

public class CategoryTransformer implements DBTransformer<Category> {

    private List<Map<String, Object>> input;

    private List<Category> transformed;

    public CategoryTransformer(
            final List<Map<String, Object>> input) {
        this.input = input;
    }

    @Override
    public List<Category> asList() {
        if(null == transformed){
          transformed = new CategoryFactoryImpl().convertForCategories(input);
        }
        return transformed;
    }

}
