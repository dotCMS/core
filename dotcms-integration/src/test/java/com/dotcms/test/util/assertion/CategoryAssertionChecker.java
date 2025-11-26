package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link Category}
 */
public class CategoryAssertionChecker implements AssertionChecker<Category> {
    @Override
    public Map<String, Object> getFileArguments(final Category category, File file) {
        return Map.of(
                "inode", category.getInode(),
                "name", category.getCategoryName(),
                "key", category.getKey(),
                "sort_order", category.getSortOrder(),
                "var_name", category.getCategoryVelocityVarName()
        );
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/category/category.category.dpc.xml";
    }

    @Override
    public File getFileInner(final Category category, final File bundleRoot) {
        try {
            return FileBundlerTestUtil.getCategoryPath(category, bundleRoot);
        } catch (DotSecurityException | DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> getRegExToRemove(File file) {
        return list(
                "<iDate>.*</iDate>",
                "<modDate>.*</modDate>",
                "<iDate class=\"sql-timestamp\">.*</iDate>",
                "<modDate class=\"sql-timestamp\">.*</modDate>"
        );
    }
}
