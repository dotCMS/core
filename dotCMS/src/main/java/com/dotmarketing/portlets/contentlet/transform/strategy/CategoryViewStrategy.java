package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.contenttype.model.field.CategoryField;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates a Map view including categories
 */
class CategoryViewStrategy extends AbstractTransformStrategy<Contentlet> {

    CategoryViewStrategy(final TransformToolbox toolBox) {
        super(toolBox);
    }

    @Override
    Map<String, Object> transform(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user)
            throws DotDataException, DotSecurityException {

        if (contentlet.getInode() == null) {
            throw new DotStateException("Contentlet needs an inode to get fields");
        }

        final List<Category> categories = toolBox.categoryAPI.getParents(contentlet, user, true);
        if (categories != null && !categories.isEmpty()) {

            final List<CategoryField> categoryFields = contentlet.getContentType()
                    .fields(CategoryField.class).stream().filter(Objects::nonNull)
                    .map(CategoryField.class::cast).collect(Collectors.toList());

            for (final CategoryField field : categoryFields) {
                map.put(field.variable(), transform(field, categories, user));
            }
        }

        return map;
    }

    /**
     * Create a single view Category view for all the passed categories
     * @param field
     * @param categories
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Map<String, Object> transform(final CategoryField field,
            final List<Category> categories, final User user
    ) throws DotSecurityException, DotDataException {

        final Category parentCategory = toolBox.categoryAPI.find(field.values(), user, true);
        final List<Map<String, Object>> childCategories = new ArrayList<>();
        if (parentCategory != null) {
            for (final Category category : categories) {
                if (toolBox.categoryAPI.isParent(category, parentCategory, user, true)) {
                    childCategories.add(transform(category));
                }
            }
        }
        return ImmutableMap.of("categories", childCategories);
    }

    /**
     * Create a single view Category view
     * @param cat
     * @return
     */
    private Map<String, Object> transform(final Category cat) {
        final Builder<String,Object> builder = new Builder<>();
        builder.put("inode", cat.getInode());
        builder.put("active", cat.isActive());
        builder.put("name", cat.getCategoryName());
        builder.put("key", cat.getKey());
        builder.put("keywords", cat.getKeywords());
        builder.put("velocityVar", cat.getCategoryVelocityVarName());
        return builder.build();
    }
}
