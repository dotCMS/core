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

    private final Set<PathMatcher> folderIncludes;
    private final Set<PathMatcher> assetIncludes;
    private final Set<PathMatcher> folderExcludes;
    private final Set<PathMatcher> assetExcludes;
    private final String rootPath;

    private Filter(
            Set<PathMatcher> folderIncludes,
            Set<PathMatcher> assetIncludes,
            Set<PathMatcher> folderExcludes,
            Set<PathMatcher> assetExcludes,
            String rootPath) {
        this.folderIncludes = folderIncludes;
        this.assetIncludes = assetIncludes;
        this.folderExcludes = folderExcludes;
        this.assetExcludes = assetExcludes;
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

                if (use(true, folderPath)) {
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

                if (use(false, assetPath)) {
                    filteredAssets.add(assetVersion);
                }
            });
        }

        folder = folder.withSubFolders(filteredSubFolders);
        var versions = AssetVersionsView.builder().versions(filteredAssets).build();
        return folder.withAssets(versions);
    }

    private boolean use(Boolean isFolder, String path) {

        var includes = isFolder ? folderIncludes : assetIncludes;
        var excludes = isFolder ? folderExcludes : assetExcludes;

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

        private final Set<PathMatcher> includeFolderPatterns = new HashSet<>();
        private final Set<PathMatcher> includeAssetPatterns = new HashSet<>();
        private final Set<PathMatcher> excludeFolderPatterns = new HashSet<>();
        private final Set<PathMatcher> excludeAssetPatterns = new HashSet<>();

        private String rootPath = "/";

        private Builder() {
        }

        public Builder rootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        public Builder includeFolder(String... include) {
            for (String pattern : include) {
                includeFolderPatterns.add(
                        FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                );
            }
            return this;
        }

        public Builder includeAsset(String... include) {
            for (String pattern : include) {
                includeAssetPatterns.add(
                        FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                );
            }
            return this;
        }

        public Builder excludeFolder(String... exclude) {
            for (String pattern : exclude) {
                excludeFolderPatterns.add(
                        FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                );
            }
            return this;
        }

        public Builder excludeAsset(String... exclude) {
            for (String pattern : exclude) {
                excludeAssetPatterns.add(
                        FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                );
            }
            return this;
        }

        public Filter build() {
            return new Filter(
                    includeFolderPatterns,
                    includeAssetPatterns,
                    excludeFolderPatterns,
                    excludeAssetPatterns,
                    rootPath
            );
        }
    }
}
