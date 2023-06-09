package com.dotcms.cli.common;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodeInfo;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.language.Language;

import java.util.List;
import java.util.Set;

/**
 * Utility class for file-related operations.
 */
public class FilesUtils {

    private FilesUtils() {
        //Hide public constructor
    }

    private static final String STATUS_LIVE = "live";
    private static final String STATUS_WORKING = "working";

    /**
     * Converts a boolean status to a string representation.
     *
     * @param isLive the status to convert
     * @return the string representation of the status
     */
    public static String statusToString(boolean isLive) {
        return isLive ? STATUS_LIVE : STATUS_WORKING;
    }

    /**
     * Collects unique statuses and languages from the provided tree node and its children.
     *
     * @param node the tree node to collect statuses and languages from
     * @return a TreeNodeInfo object containing the collected statuses and languages
     */
    public static TreeNodeInfo CollectUniqueStatusesAndLanguages(TreeNode node) {

        TreeNodeInfo nodeInfo = new TreeNodeInfo();

        if (node.assets() != null) {
            for (AssetView asset : node.assets()) {
                if (asset.live()) {
                    nodeInfo.addLiveLanguage(asset.lang());
                } else {
                    nodeInfo.addWorkingLanguage(asset.lang());
                }

                nodeInfo.addLanguage(asset.lang());

                nodeInfo.incrementAssetsCount();
            }
        }

        for (TreeNode child : node.children()) {

            TreeNodeInfo childInfo = CollectUniqueStatusesAndLanguages(child);

            nodeInfo.languages().addAll(childInfo.languages());
            nodeInfo.liveLanguages().addAll(childInfo.liveLanguages());
            nodeInfo.workingLanguages().addAll(childInfo.workingLanguages());

            nodeInfo.incrementAssetsCount(childInfo.assetsCount());
            nodeInfo.incrementFoldersCount(childInfo.foldersCount());

            nodeInfo.incrementFoldersCount();
        }

        return nodeInfo;
    }

    /**
     * Fallbacks to the default language in case of no languages found scanning the assets.
     *
     * @param languages           the list of available languages
     * @param uniqueLiveLanguages the set of unique live languages
     * @throws RuntimeException if no default language is found in the list of languages
     */
    public static void FallbackDefaultLanguage(
            final List<Language> languages, Set<String> uniqueLiveLanguages) {

        // Get the default language from the list of languages
        var defaultLanguage = languages.stream()
                .filter(Language::defaultLanguage)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No default language found"));

        var languageTag = new StringBuilder(defaultLanguage.languageCode());
        if (defaultLanguage.countryCode() != null && !defaultLanguage.countryCode().isEmpty()) {
            languageTag.append("-").append(defaultLanguage.countryCode());
        }

        uniqueLiveLanguages.add(languageTag.toString());
    }
}
