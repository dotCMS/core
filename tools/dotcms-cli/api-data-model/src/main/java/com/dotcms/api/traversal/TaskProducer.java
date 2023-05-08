package com.dotcms.api.traversal;

import com.dotcms.model.asset.FolderView;
import io.quarkus.arc.DefaultBean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class TaskProducer {

    @Produces
    @DefaultBean
    FolderView produceFolderView(
            @Any final String site,
            @Any final String parentFolderName,
            @Any final String folderName) {

        return FolderView.builder()
                .site(site)
                .path(parentFolderName)
                .name(folderName)
                .level(0)
                .build();
    }

}
