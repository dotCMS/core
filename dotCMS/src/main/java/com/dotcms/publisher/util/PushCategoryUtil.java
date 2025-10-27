package com.dotcms.publisher.util;

import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PushCategoryUtil {


    private Map<String, File> categoriesByInode;
    private Set<String> categoriesTopLevel;

    private int categoriesCount;

    public PushCategoryUtil(Collection<File> categories, String categoryExtension) throws DotPublishingException {

        categoriesByInode = new HashMap<>();
        categoriesTopLevel = new HashSet<>();
        categoriesCount = 0;

        for (File categoryFile : categories) {

            if (categoryFile.getName().endsWith(categoryExtension)) {
                categoriesCount++;
            }

            if (categoryFile.isDirectory()) {
                continue;
            }

            try {
                CategoryWrapper wrapper = getCategoryWrapperFromFile(categoryFile);

                categoriesByInode.put(wrapper.getCategory().getInode(), categoryFile);

                if (wrapper.isTopLevel()) {
                    categoriesTopLevel.add(wrapper.getCategory().getInode());
                }

            } catch (IOException fnfe) {
                throw new DotPublishingException(fnfe.getMessage(), fnfe);
            }
        }
    }

    /**
     * Find all top level categories. Returns all the Wrapper.
     * <p>
     * Mar 6, 2013 - 9:53:45 AM
     */
    public List<CategoryWrapper> findTopLevelWrappers() throws IOException {
        List<CategoryWrapper> topLevels = new ArrayList<>();
        for (String categoryInode : categoriesTopLevel) {
            File categoryFile = categoriesByInode.get(categoryInode);
            topLevels.add(getCategoryWrapperFromFile(categoryFile));
        }
        return topLevels;
    }

    public CategoryWrapper getCategoryWrapperFromInode(final String inode) throws IOException {
        File categoryFile = categoriesByInode.get(inode);
        return (categoryFile != null) ? getCategoryWrapperFromFile(categoryFile) : null;
    }

    private CategoryWrapper getCategoryWrapperFromFile(final File category) throws IOException {

        return BundlerUtil.readObject(category, CategoryWrapper.class);

    }

    public int getCategoryXMLCount() {
        return categoriesCount;
    }
}
