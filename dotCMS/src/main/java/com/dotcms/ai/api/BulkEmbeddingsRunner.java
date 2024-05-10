package com.dotcms.ai.api;

import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.List;
import java.util.stream.Collectors;

public class BulkEmbeddingsRunner implements Runnable {

    final User user;
    final EmbeddingsForm embeddingsForm;
    final List<String> inodes;

    public BulkEmbeddingsRunner(List<String> inodes, EmbeddingsForm embeddingsForm) {
        user = Try.of(() -> APILocator.getUserAPI().loadUserById(embeddingsForm.userId)).getOrElse(APILocator.systemUser());
        this.embeddingsForm = embeddingsForm;
        this.inodes = inodes;
    }

    @Override
    public void run() {
        for (String inode : inodes) {
            try {

                Contentlet contentlet = APILocator.getContentletAPI().find(inode, user, false);
                if (UtilMethods.isEmpty(contentlet::getContentType) || !contentlet.isLive()) {
                    continue;
                }

                List<Field> fields = contentlet.getContentType().fields().stream().filter(f -> embeddingsForm.fieldsAsList().contains(f.variable().toLowerCase())).collect(Collectors.toList());

                // if a velocity template is passed in, use it.  Otherwise, try the fields
                if (!EmbeddingsAPI.impl().generateEmbeddingsForContent(contentlet, embeddingsForm.velocityTemplate, embeddingsForm.indexName)) {
                    EmbeddingsAPI.impl().generateEmbeddingsForContent(contentlet, fields, embeddingsForm.indexName);
                }
            } catch (Exception e) {
                Logger.warn(this.getClass(), "unable to embed content:" + inode + " error:" + e.getMessage(), e);

            }

        }

    }
}
