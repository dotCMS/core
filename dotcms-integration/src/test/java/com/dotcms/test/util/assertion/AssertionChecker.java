package com.dotcms.test.util.assertion;

import java.io.File;
import java.util.*;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Provide method to help assert files into a bundle root directory
 *
 * @see com.dotcms.test.util.FileTestUtil#assertBundleFile(File, Object)
 * @see com.dotcms.test.util.FileTestUtil#assertBundleFile(File, Object, String)
 * @see com.dotcms.test.util.FileTestUtil#assertBundleFile(File, Object, Collection)
 *
 * @param <T> Asset type for who this class work
 */
public interface AssertionChecker<T> {

    /**
     * Get file arguments to use in to populate a template file
     *
     * @param asset Asset to be test
     * @param file File to compare with the populated template, if the {@link AssertionChecker#getFile(Object, File)}
     *             method return more that one files then this parameter can be used to decide which arguments apply
     *
     * @return
     *
     * @see com.dotcms.test.util.FileTestUtil#assertBundleFile(File, Object)
     * @see com.dotcms.test.util.FileTestUtil#assertBundleFile(File, Object, String)
     * @see com.dotcms.test.util.FileTestUtil#assertBundleFile(File, Object, Collection)
     * @see com.dotcms.test.util.FileTestUtil#getFormattedContent(File, Map)
     */
    Map<String, Object> getFileArguments(T asset, File file);

    /**
     * Return the default file path use as template file.
     *
     * @param file if the {@link AssertionChecker#getFile(Object, File)}
     *             method return more that one files then this parameter can be used to decide which template apply
     * @return
     */
    String getFilePathExpected(File file);

    /**
     * Return the default file path use as template file for all the files can be returned by {@link AssertionChecker#getFile(Object, File)}.
     *
     * @param file if the {@link AssertionChecker#getFile(Object, File)}
     *             method return more that one files then this parameter can be used to decide which template apply
     * @return
     */
    default Collection<String> getFilesPathExpected() {
        return list(getFilePathExpected(null));
    }

    /**
     * Return the file that should be created in <code>bundleRoot</code> for the <code>asset</code>
     *
     * @param asset asset to be test
     * @param bundleRoot Bundle root directory
     * @return
     */
    default Collection<File> getFile(T asset, File bundleRoot) {
        return list(getFileInner(asset, bundleRoot));
    }

    /**
     * Helping method when a concrete class want to return just one file in the {@link AssertionChecker#getFile(Object, File)}
     * @param asset
     * @param bundleRoot
     * @return
     */
    default File getFileInner(T asset, File bundleRoot) {
        throw new UnsupportedOperationException();
    }

    /**
     * Overwrite if the concrete class want to remove some section (as date property for example) before compare the
     * files content.
     *
     * @param file
     * @return
     */
    default Collection<String> getRegExToRemove(File file) {
        return Collections.EMPTY_LIST;
    }

    default boolean checkFileContent(T asset) {
        return true;
    }
}
