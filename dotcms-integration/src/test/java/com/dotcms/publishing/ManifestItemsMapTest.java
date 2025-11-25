package com.dotcms.publishing;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import io.vavr.collection.Stream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class ManifestItemsMapTest {
    private Map<String, List<String>> includes = new HashMap<>();
    private Map<String, List<String>> excludes = new HashMap<>();
    private Map<String, List<String>> evaluateResons = new HashMap<>();
    private Set<String> alreadyCheck;


    private static String getIncludeLine(final ManifestItem asset, final String evaluateReason) {
        return getLine("INCLUDED", asset, evaluateReason, "");
    }

    private static String getExcludeLine(final ManifestItem asset,
            final String evaluateReason, final String excludeReason) {
        return getLine("EXCLUDED", asset, evaluateReason, excludeReason);
    }
    
    private static String getLine(final String includeExclude, final ManifestItem asset, 
            final String reasonEvaluate, final String reasonExclude) {
        final ManifestInfo manifestInfo = asset.getManifestInfo();

        return list(
                includeExclude,
                manifestInfo.objectType(),
                manifestInfo.id(),
                manifestInfo.inode(),
                manifestInfo.title(),
                manifestInfo.site(),
                manifestInfo.folder(),
                reasonExclude,
                reasonEvaluate
        ).stream().collect(Collectors.joining(","));
    }

    public void addDependencies(final Map<ManifestItem, Collection<ManifestItem>> dependencies){
        for (Entry<ManifestItem, Collection<ManifestItem>> dependencyEntry : dependencies.entrySet()) {
            final ManifestItem key = dependencyEntry.getKey();
            final String dependencyReeason = String.format("Dependency from: ID: %s Title: %s", key.getManifestInfo().id(),
                    key.getManifestInfo().title());

            final Collection<ManifestItem> entryDependencies = dependencyEntry.getValue();

            for (ManifestItem entryDependency : entryDependencies) {
                add(entryDependency, dependencyReeason);
            }
        }
    }

    public void add(final ManifestItem assetManifestItem, final List<String> evaluateReasons) {
        for (final String evaluateReason : evaluateReasons) {
            add(assetManifestItem, evaluateReason);
        }
    }

    public void add(final ManifestItem assetManifestItem, final String evaluateReason) {
        final String key = assetManifestItem.getManifestInfo().id();

        if (excludes.containsKey(key)) {
            return;
        }

        List<String> lines = includes.get(key);

        if (lines == null) {
            lines = new ArrayList<>();
            includes.put(key, lines);
        }

        lines.add(getIncludeLine(assetManifestItem, evaluateReason));

        final List<String> evaluateReasons = evaluateResons
                .computeIfAbsent(key, k -> new ArrayList<>());
        if (UtilMethods.isSet(evaluateReason) && !evaluateReasons.contains(evaluateReason)) {
            evaluateReasons.add(evaluateReason);
        }

    }

    public void addExclude(final ManifestItem assetManifestItem, final String excludeReason) {
        addExclude(assetManifestItem, "", excludeReason);
    }

    public void addExclude(final ManifestItem assetManifestItem,
            final String evaluateReason, final String excludeReason) {
        final String key = assetManifestItem.getManifestInfo().id();

        if (includes.containsKey(key)) {
            includes.remove(key);
        }

        List<String> lines = excludes.get(key);

        if (lines == null) {
            lines = new ArrayList<>();
            excludes.put(key, lines);
        }

        final List<String> evaluateReasons = evaluateResons
                .computeIfAbsent(key, k -> new ArrayList<>());
        if (UtilMethods.isSet(evaluateReason) && !evaluateReasons.contains(evaluateReason)) {
            evaluateReasons.add(evaluateReason);
        }

        if (!UtilMethods.isSet(evaluateReason)) {
            lines.add(getExcludeLine(assetManifestItem, "", excludeReason));
        } else {
            for (final String reason : evaluateReasons) {
                lines.add(getExcludeLine(assetManifestItem, reason, excludeReason));
            }
        }
    }

    public int size() {
        return includes.size() + excludes.size();
    }

    public void startCheck() {
        alreadyCheck = new HashSet<>();
    }

    public boolean contains(final String line) {
        if (line.startsWith("INCLUDE")) {
            return contains(line, includes);
        } else {
            return contains(line, excludes);
        }
    }

    private boolean contains(final String line, final Map<String, List<String>> map) {
        for (Entry<String, List<String>> includeEntry : map.entrySet()) {
            final List<String> lines = includeEntry.getValue();

            if (lines.contains(line)) {
                if (alreadyCheck.contains(includeEntry.getKey())) {
                    return false;
                } else {
                    alreadyCheck.add(includeEntry.getKey());
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String toString(){
        return Stream.concat(includes.entrySet(), excludes.entrySet())
                .map(entry -> entry.getKey() + " -> " + entry.getValue().stream().collect(Collectors.joining("|")))
                .collect(Collectors.joining("\n"));
    }

    public void addExcludes(final Map<String, List<ManifestItem>> excludes) {
        addExcludes(excludes, null);
    }

    public void addExcludes(
            final Map<String, List<ManifestItem>> excludes,
            final ManifestItem dependencyFrom) {
        for (Entry<String, List<ManifestItem>> excludeEntry : excludes.entrySet()) {

            final List<ManifestItem> entryDependencies = excludeEntry.getValue();

            for (ManifestItem assetExclude : entryDependencies) {
                if (dependencyFrom != null) {
                    final String dependencyReeason = String.format(
                            "Dependency from: ID: %s Title: %s",
                            dependencyFrom.getManifestInfo().id(),
                            dependencyFrom.getManifestInfo().title());
                    addExclude(assetExclude, dependencyReeason, excludeEntry.getKey());
                } else {
                    addExclude(assetExclude, excludeEntry.getKey());
                }
            }
        }
    }
}
