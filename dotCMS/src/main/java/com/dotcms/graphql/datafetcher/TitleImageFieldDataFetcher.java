package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformer;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class TitleImageFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();

            final Optional<Field> imageField = contentlet.getTitleImage();

            if (!imageField.isPresent()) {
                return Collections.emptyMap();
            }

            Map<String, Object> titleImageMap = Collections.emptyMap();

            if (imageField.get() instanceof ImageField || imageField.get() instanceof FileField) {
                final String imageContentletId = contentlet.getStringProperty(imageField.get().variable());

                final User user = ((DotGraphQLContext) environment.getContext()).getUser();
                final Contentlet
                    imageContentlet =
                    APILocator.getContentletAPI().findContentletByIdentifier(imageContentletId, contentlet.isLive(),
                        contentlet.getLanguageId(), user, true);
                final File imageFile = imageContentlet.getBinary("fileAsset");

                if (imageContentlet.getTitleImage().isPresent()) {
                    titleImageMap =
                        new BinaryToMapTransformer(contentlet)
                            .transform(imageFile, contentlet, imageContentlet.getTitleImage().get());
                }


            } else if (imageField.get() instanceof BinaryField) {
                titleImageMap = new BinaryToMapTransformer(contentlet)
                    .transform(contentlet.getTitleImage().get(), contentlet);
            }

            return titleImageMap;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
