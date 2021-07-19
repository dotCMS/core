package com.dotcms.publishing;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
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
    private Set<String> alreadyCheck;


    private static String getIncludeLine(final ManifestItem asset, final String reason) {
        return getLine("INCLUDED", asset, reason, "");
    }

    private static String getExcludeLine(final ManifestItem asset, final String reason) {
        return getLine("EXCLUDED", asset, "", reason);
    }
    
    private static String getLine(final String includeExclude, final ManifestItem asset, 
            final String reasonInclude, final String reasonExclude) {
        final ManifestInfo manifestInfo = asset.getManifestInfo();

        return list(
                includeExclude,
                manifestInfo.objectType(),
                manifestInfo.id(),
                manifestInfo.title(),
                manifestInfo.site(),
                manifestInfo.folder(),
                reasonExclude,
                reasonInclude
        ).stream().collect(Collectors.joining(","));
    }

    public void addDependencies(final Map<ManifestItem, Collection<ManifestItem>> dependencies){
        for (Entry<ManifestItem, Collection<ManifestItem>> dependencyEntry : dependencies.entrySet()) {
            final String id = dependencyEntry.getKey().getManifestInfo().id();
            final String dependencyReeason = "Dependency from: " + id;

            final Collection<ManifestItem> entryDependencies = dependencyEntry.getValue();

            for (ManifestItem entryDependency : entryDependencies) {
                add(entryDependency, dependencyReeason);
            }
        }
    }

    public void add(final ManifestItem assetManifestItem, final String reason) {
        final String key = assetManifestItem.getManifestInfo().id();

        if (excludes.containsKey(key)) {
            return;
        }

        List<String> lines = includes.get(key);

        if (lines == null) {
            lines = new ArrayList<>();
            includes.put(key, lines);
        }

        lines.add(getIncludeLine(assetManifestItem, reason));

    }

    public void addExclude(final ManifestItem assetManifestItem, final String reason) {
        final String key = assetManifestItem.getManifestInfo().id();

        if (includes.containsKey(key)) {
            includes.remove(key);
        }

        List<String> lines = excludes.get(key);

        if (lines == null) {
            lines = new ArrayList<>();
            excludes.put(key, lines);
        }

        lines.add(getExcludeLine(assetManifestItem, reason));
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
        for (Entry<String, List<ManifestItem>> excludeEntry : excludes.entrySet()) {

            final List<ManifestItem> entryDependencies = excludeEntry.getValue();

            for (ManifestItem assetExclude : entryDependencies) {
                addExclude(assetExclude, excludeEntry.getKey());
            }
        }
    }
}
