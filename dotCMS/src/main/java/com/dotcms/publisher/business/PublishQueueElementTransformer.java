package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.google.common.base.CaseFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PublishQueueElementTransformer {

    public static final String TYPE_KEY = "type";
    public static final String TITLE_KEY = "title";
    public static final String INODE_KEY = "inode";
    public static final String CONTENT_TYPE_NAME_KEY = "content type name";
    public static final String LANGUAGE_CODE_KEY = "language code";
    public static final String COUNTRY_CODE_KEY = "country code";

    public PublishQueueElementTransformer(){}

    public List<Map<String, String>> transform(final List<PublishQueueElement> publishQueueElements){
        return publishQueueElements.stream()
                .map(PublishQueueElementTransformer::getMap)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Map<String, String> getMap(final PublishQueueElement publishQueueElement) {

        if (isContentlet(publishQueueElement)) {
            return getMapForContentlet(publishQueueElement);
        } else if (isLanguage(publishQueueElement)) {
            return getMapForLanguage(publishQueueElement);
        } else {

            String title = PublishAuditUtil.getInstance()
                    .getTitle(publishQueueElement.getType(),
                            publishQueueElement.getAsset());

            if (title.equals( publishQueueElement.getType() )) {
                title = "";
                Logger.warn( PublishQueueElementTransformer.class, () ->
                        "Unable to find Asset of type: [" + publishQueueElement.getType() + "] with identifier: [" + publishQueueElement.getAsset() + "]" );

                try{
                    Logger.info( PublishQueueElementTransformer.class, "Cleaning Publishing Queue, identifier [" + publishQueueElement.getAsset() + "] no longer exists");
                    PublisherAPIImpl.getInstance().deleteElementFromPublishQueueTable(publishQueueElement.getAsset());
                } catch (DotPublisherException dpe){
                    Logger.warn(PublishQueueElementTransformer.class,
                            () -> "Unable to delete Asset from Publishing Queue with identifier: [" + publishQueueElement.getAsset() + "]", dpe );
                }
            }

            return map(
                TYPE_KEY, publishQueueElement.getType(),
                TITLE_KEY, title
            );
        }
    }

    private static Map<String, String> getMapForLanguage(
            final PublishQueueElement publishQueueElement) {

        final Language language = APILocator.getLanguageAPI().getLanguage(publishQueueElement.getAsset());

        return map(
                TYPE_KEY, publishQueueElement.getType(),
                TITLE_KEY, String.format( "%s(%s)", language.getLanguage(), language.getCountryCode()),
                LANGUAGE_CODE_KEY, language.getLanguageCode(),
                COUNTRY_CODE_KEY, language.getCountryCode(),
                CONTENT_TYPE_NAME_KEY,  CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, publishQueueElement.getType())
        );
    }

    private static Map<String, String> getMapForContentlet(
            final PublishQueueElement publishQueueElement) {

        final Contentlet contentlet;

        try {
            contentlet = PublishAuditUtil.getInstance()
                    .findContentletByIdentifier(publishQueueElement.getAsset());

            return map(
                    TYPE_KEY, publishQueueElement.getType(),
                    TITLE_KEY, contentlet.getTitle(),
                    INODE_KEY, contentlet.getInode(),
                    CONTENT_TYPE_NAME_KEY, contentlet.getContentType().name()
            );
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(PublishQueueElementTransformer.class, e.getMessage());
            return null;
        }
    }

    private static boolean isContentlet(PublishQueueElement publishQueueElement) {
        return publishQueueElement.getType().equals(PusheableAsset.CONTENTLET.getType());
    }

    private static boolean isLanguage(PublishQueueElement publishQueueElement) {
        return publishQueueElement.getType().equals(PusheableAsset.LANGUAGE.getType());
    }
}
