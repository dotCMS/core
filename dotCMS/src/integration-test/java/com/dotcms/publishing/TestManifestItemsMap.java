package com.dotcms.publishing;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class TestManifestItemsMap {
    private Map<String, List<String>> includes = new HashMap<>();
    private Set<String> alreadyCheck;

    private static String getLine(final ManifestItem asset, final String reason) {
        final ManifestInfo manifestInfo = asset.getManifestInfo();

        return list(
                "INCLUDE",
                manifestInfo.objectType(),
                manifestInfo.id(),
                manifestInfo.title(),
                manifestInfo.site(),
                manifestInfo.folder(),
                "",
                reason
        ).stream().collect(Collectors.joining(","));
    }

    public void add(final ManifestItem assetManifestItem, final String reason) {
        List<String> lines = includes.get(assetManifestItem.getManifestInfo().id());

        if (lines == null) {
            lines = new ArrayList<>();
            includes.put(assetManifestItem.getManifestInfo().id(), lines);
        }

        lines.add(getLine(assetManifestItem, reason));

    }

    public int size() {
        return includes.size();
    }

    public void startCheck() {
        alreadyCheck = new HashSet<>();
    }

    public boolean contains(final String line) {
        for (Entry<String, List<String>> includeEntry : includes.entrySet()) {
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
        return includes.entrySet().stream()
                .map(entry -> entry.getKey() + " -> " + entry.getValue().stream().collect(Collectors.joining("|")))
                .collect(Collectors.joining("\n"));
    }
}
