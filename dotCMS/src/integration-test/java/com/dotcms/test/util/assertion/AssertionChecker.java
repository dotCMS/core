package com.dotcms.test.util.assertion;

import java.io.File;
import java.util.*;

import static com.dotcms.util.CollectionsUtils.list;

public interface AssertionChecker<T> {
    Map<String, Object> getFileArguments(T asset, File file);

    String getFilePathExpected(File file);

    default Collection<String> getFilesPathExpected() {
        return list(getFilePathExpected(null));
    }

    default Collection<File> getFile(T asset, File bundleRoot) {
        return list(getFileInner(asset, bundleRoot));
    }

    default File getFileInner(T asset, File bundleRoot) {
        throw new UnsupportedOperationException();
    }

    default Collection<String> getRegExToRemove(File file) {
        return Collections.EMPTY_LIST;
    }

    default boolean checkFileContent(T asset) {
        return true;
    }
}
