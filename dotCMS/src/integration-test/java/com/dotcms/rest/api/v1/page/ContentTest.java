package com.dotcms.rest.api.v1.page;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentTest {
    private final Map<String, List<Contentlet>> contents = new HashMap<>();

    void addContent (final String conteinerId, final Contentlet contentlet) {
        List<Contentlet> contentlets = contents.get(conteinerId);

        if (contentlets == null) {
            contentlets = new ArrayList<>();
            contents.put(conteinerId, contentlets);
        }

        contentlets.add(contentlet);
    }

    List<Contentlet> getContents(final String conteinerId) {
        return contents.get(conteinerId);
    }

    public long getNumber() {
        return contents.values().stream()
                .flatMap(containerContents -> containerContents.stream())
                .count();
    }
}