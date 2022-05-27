package com.dotcms.content.model.hydration;

import com.dotcms.contenttype.model.field.ImageField;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import io.vavr.control.Try;
import java.util.Optional;

public class HydrationUtils {

    static Optional<Contentlet> findLinkedBinary(final Contentlet contentlet, final ImageField imageField){
        final Object fileAssetIdentifier = contentlet.get(imageField.variable());

        final Optional<Contentlet> fileAsContentOptional = APILocator.getContentletAPI()
                .findContentletByIdentifierOrFallback(fileAssetIdentifier.toString(),
                        Try.of(contentlet::isLive).getOrElse(false)
                        , contentlet.getLanguageId(),
                        APILocator.systemUser(), true);
        if (fileAsContentOptional.isPresent()) {
            final Contentlet fileAsset = fileAsContentOptional.get();
            return Optional.of(fileAsset);
        } else {
            return Optional.empty();
        }
    }

}
