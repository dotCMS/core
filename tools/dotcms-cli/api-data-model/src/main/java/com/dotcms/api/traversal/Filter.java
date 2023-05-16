package com.dotcms.api.traversal;

import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Filter {

    private final Set<PathMatcher> includes;
    private final Set<PathMatcher> excludes;
    private final String rootPath;

    private Filter(Set<PathMatcher> includes, Set<PathMatcher> excludes, String rootPath) {
        this.includes = includes;
        this.excludes = excludes;
        this.rootPath = rootPath;
    }

    public FolderView apply(FolderView folder) {

        List<FolderView> filteredSubFolders = new ArrayList<>();
        List<AssetView> filteredAssets = new ArrayList<>();

        if (folder.subFolders() != null) {
            folder.subFolders().iterator().forEachRemaining(subFolder -> {

                var folderPath = subFolder.path();

                if (!rootPath.equals("/")) {
                    folderPath = folderPath.replaceFirst("^" + rootPath, "");
                }

                if (use(folderPath)) {
                    filteredSubFolders.add(subFolder);
                }
            });
        }

        if (folder.assets() != null) {
            folder.assets().versions().iterator().forEachRemaining(assetVersion -> {

                var assetPath = Paths.get(assetVersion.path()).
                        resolve(assetVersion.name()).toString();

                if (!rootPath.equals("/")) {
                    assetPath = assetPath.replaceFirst("^" + rootPath, "");
                }

                if (use(assetPath)) {
                    filteredAssets.add(assetVersion);
                }
            });
        }

        folder = folder.withSubFolders(filteredSubFolders);
        var versions = AssetVersionsView.builder().versions(filteredAssets).build();
        return folder.withAssets(versions);
    }

    private boolean use(String path) {

        FileSystem fileSystem = FileSystems.getDefault();

        // Check if the path should be used according to the excludes
        for (PathMatcher exclude : excludes) {
            if (exclude.matches(fileSystem.getPath(path))) {
                return false;
            }
        }

        // Check if the path should be used according to the includes
        if (includes.isEmpty()) {
            return true;
        }

        for (PathMatcher include : includes) {
            if (include.matches(fileSystem.getPath(path))) {
                return true;
            }
        }

        // If no include patterns matched, exclude the path by default
        return false;
    }

    public static Filter.Builder builder() {
        return new Filter.Builder();
    }

    public static class Builder {

        private final Set<PathMatcher> includePatterns = new HashSet<>();
        private final Set<PathMatcher> excludePatterns = new HashSet<>();

        private String rootPath = "/";

        private Builder() {
        }

        public Builder rootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        public Builder include(String... include) {
            for (String pattern : include) {
                includePatterns.add(
                        FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                );
            }
            return this;
        }

        public Builder exclude(String... exclude) {
            for (String pattern : exclude) {
                excludePatterns.add(
                        FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                );
            }
            return this;
        }

        public Filter build() {
            return new Filter(includePatterns, excludePatterns, rootPath);
        }
    }
}
