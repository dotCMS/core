package com.dotcms.api.traversal;

import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static com.dotcms.model.asset.BasicMetadataFields.PATH_META_KEY;

/**
 * A filter that determines whether a path should be included or excluded based on the given
 * includes and excludes patterns. This filter can be applied to folders and assets separately.
 */
public class Filter {

    private final Set<PathMatcher> folderIncludes;
    private final Set<PathMatcher> assetIncludes;
    private final Set<PathMatcher> folderExcludes;
    private final Set<PathMatcher> assetExcludes;
    private final String rootPath;

    /**
     * Creates a new filter with the specified includes and excludes patterns for folders and
     * assets, and the root path.
     *
     * @param folderIncludes The patterns to include folders.
     * @param assetIncludes  The patterns to include assets.
     * @param folderExcludes The patterns to exclude folders.
     * @param assetExcludes  The patterns to exclude assets.
     * @param rootPath       The root path to apply the filter. This is used to remove the root path
     *                       prefix from the folder and asset paths before applying the filter.
     */
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

    /**
     * Applies the filter to the given folder, filtering its sub-folders and assets based on the
     * configured includes and excludes patterns.
     *
     * @param folder The folder to apply the filter to.
     * @return The filtered folder view.
     */
    public FolderView apply(FolderView folder) {

        List<FolderView> filteredSubFolders = new ArrayList<>();
        List<AssetView> filteredAssets = new ArrayList<>();

        // We need to analyze also the root folder
        if (folder.path().equals("/")) {
            folder = evaluateFolder(folder);
        }

        Optional.ofNullable(folder.subFolders()).ifPresent(
                subFolders -> subFolders.stream().
                        map(this::evaluateFolder).
                        forEach(filteredSubFolders::add)
        );

        if (folder.assets() != null) {
            folder.assets().versions().iterator().forEachRemaining(assetVersion -> {

                var assetVersionPath = assetVersion.metadata().get(PATH_META_KEY.key()).toString();
                var assetPath = Paths.get(assetVersionPath).
                        resolve(assetVersion.name()).toString();

                if (!rootPath.equals("/")) {
                    assetPath = assetPath.replaceFirst("^" + rootPath, "");
                }

                if (useAsset(assetPath)) {
                    filteredAssets.add(assetVersion);
                }
            });
        }

        folder = folder.withSubFolders(filteredSubFolders);
        var versions = AssetVersionsView.builder().versions(filteredAssets).build();
        return folder.withAssets(versions);
    }

    /**
     * Evaluates the given folder by determining whether it should be included based on the includes
     * and excludes patterns for folders setting specific flags in the {@link FolderView} object.
     *
     * @param subFolder The folder to validate.
     * @return The validated folder view.
     */
    private FolderView evaluateFolder(FolderView subFolder) {

        var folderPath = subFolder.path();

        if (!rootPath.equals("/")) {
            folderPath = folderPath.replaceFirst("^" + rootPath, "");
        }

        FileSystem fileSystem = FileSystems.getDefault();

        // Check if the path should be used according to the excludes
        for (final PathMatcher exclude : folderExcludes) {
            if (exclude.matches(fileSystem.getPath(folderPath))) {

                subFolder = subFolder.withExplicitGlobExclude(true);
                return subFolder.withImplicitGlobInclude(false);
            }
        }

        // Check if the path should be used according to the includes, no includes means include all
        if (folderIncludes.isEmpty()) {
            return subFolder.withImplicitGlobInclude(true);
        }

        for (final PathMatcher include : folderIncludes) {
            if (include.matches(fileSystem.getPath(folderPath))) {
                subFolder = subFolder.withExplicitGlobInclude(true);
                return subFolder.withImplicitGlobInclude(true);
            }
        }

        // If no include patterns matched, exclude the path by default
        return subFolder.withImplicitGlobInclude(false);
    }

    /**
     * Determines whether the given asset path should be included based on the includes and excludes
     * patterns for assets.
     *
     * @param path The asset path to check.
     * @return {@code true} if the path should be included, {@code false} otherwise.
     */
    private boolean useAsset(String path) {

        FileSystem fileSystem = FileSystems.getDefault();

        // Check if the path should be used according to the excludes
        for (PathMatcher exclude : assetExcludes) {
            if (exclude.matches(fileSystem.getPath(path))) {
                return false;
            }
        }

        // Check if the path should be used according to the includes
        if (assetIncludes.isEmpty()) {
            return true;
        }

        for (PathMatcher include : assetIncludes) {
            if (include.matches(fileSystem.getPath(path))) {
                return true;
            }
        }

        // If no include patterns matched, exclude the path by default
        return false;
    }

    /**
     * Creates a new builder for constructing a {@code Filter} instance.
     *
     * @return A new builder instance.
     */
    public static Filter.Builder builder() {
        return new Filter.Builder();
    }

    /**
     * A builder for constructing a {@code Filter} instance with includes and excludes patterns for
     * folders and assets.
     */
    public static class Builder {

        private final Set<PathMatcher> includeFolderPatterns = new HashSet<>();
        private final Set<PathMatcher> includeAssetPatterns = new HashSet<>();
        private final Set<PathMatcher> excludeFolderPatterns = new HashSet<>();
        private final Set<PathMatcher> excludeAssetPatterns = new HashSet<>();

        private String rootPath = "/";

        private Builder() {
        }

        /**
         * Sets the root path for the filter. The root path is used to remove the root path prefix
         * from the folder and asset paths before applying the filter.
         *
         * @param rootPath The root path to set.
         * @return The builder instance.
         */
        public Builder rootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        /**
         * Adds include patterns for folders.
         *
         * @param include The include patterns for folders.
         * @return The builder instance.
         */
        public Builder includeFolder(final String... include) {

            Stream.of(include).forEach(
                    pattern -> includeFolderPatterns.add(
                            FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                    )
            );

            return this;
        }

        /**
         * Adds include patterns for assets.
         *
         * @param include The include patterns for assets.
         * @return The builder instance.
         */
        public Builder includeAsset(final String... include) {

            Stream.of(include).forEach(
                    pattern -> includeAssetPatterns.add(
                            FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                    )
            );

            return this;
        }

        /**
         * Adds exclude patterns for folders.
         *
         * @param exclude The exclude patterns for folders.
         * @return The builder instance.
         */
        public Builder excludeFolder(final String... exclude) {

            Stream.of(exclude).forEach(
                    pattern -> excludeFolderPatterns.add(
                            FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                    )
            );

            return this;
        }

        /**
         * Adds exclude patterns for assets.
         *
         * @param exclude The exclude patterns for assets.
         * @return The builder instance.
         */
        public Builder excludeAsset(final String... exclude) {

            Stream.of(exclude).forEach(
                    pattern -> excludeAssetPatterns.add(
                            FileSystems.getDefault().getPathMatcher("glob:" + pattern)
                    )
            );

            return this;
        }

        /**
         * Builds a new {@code Filter} instance with the configured includes and excludes patterns
         * and the root path.
         *
         * @return A new {@code Filter} instance.
         */
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
