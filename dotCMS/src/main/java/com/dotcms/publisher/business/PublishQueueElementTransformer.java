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
    public static final String CONTENT_TYPE_NAME_KEY = "content_type_name";
    public static final String LANGUAGE_CODE_KEY = "language_code";
    public static final String COUNTRY_CODE_KEY = "country_code";
    public static final String OPERATION_KEY = "operation";
    public static final String ASSET_KEY = "asset";
    public static final String HTML_PAGE_KEY = "isHtmlPage";

    public PublishQueueElementTransformer(){}

    /**
     * Transform the {@link PublishQueueElement} into a {@link Map}, the map contain the follow items:
     *
     * For each content:
     * - type: The value is content for each content.
     * - title: Content's title.
     * - inode: Content's inode.
     * - content type name: Content's content type name.
     *
     * For each language:
     * - type: The value is content for each language.
     * - title: String with the sintax [language]-[country_code].
     * - language code: Language's code.
     * - country code: Language's country code.
     * - content type name: The value is 'Language' for each language
     *
     * For anything else:
     * - type: asset's type.
     * - title: asset's title.
     *
     * @param publishQueueElements
     * @return
     */
    public List<Map<String, Object>> transform(final List<PublishQueueElement> publishQueueElements){
        return publishQueueElements.stream()
                .map(PublishQueueElementTransformer::getMap)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Map<String, Object> getMap(final PublishQueueElement publishQueueElement) {

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
                TITLE_KEY, title,
                OPERATION_KEY, publishQueueElement.getOperation().toString(),
                ASSET_KEY, publishQueueElement.getAsset()
            );
        }
    }

    private static Map<String, Object> getMapForLanguage(
            final PublishQueueElement publishQueueElement) {

        final Language language = APILocator.getLanguageAPI().getLanguage(publishQueueElement.getAsset());

        return map(
                TYPE_KEY, publishQueueElement.getType(),
                TITLE_KEY, String.format( "%s(%s)", language.getLanguage(), language.getCountryCode()),
                LANGUAGE_CODE_KEY, language.getLanguageCode(),
                COUNTRY_CODE_KEY, language.getCountryCode(),
                CONTENT_TYPE_NAME_KEY,  CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, publishQueueElement.getType()),
                OPERATION_KEY, publishQueueElement.getOperation().toString(),
                ASSET_KEY, publishQueueElement.getAsset()
        );
    }

    private static Map<String, Object> getMapForContentlet(
            final PublishQueueElement publishQueueElement) {

        final Contentlet contentlet;

        try {
            contentlet = PublishAuditUtil.getInstance()
                    .findContentletByIdentifier(publishQueueElement.getAsset());

            return map(
                    TYPE_KEY, publishQueueElement.getType(),
                    TITLE_KEY, contentlet.getTitle(),
                    INODE_KEY, contentlet.getInode(),
                    CONTENT_TYPE_NAME_KEY, contentlet.getContentType().name(),
                    OPERATION_KEY, publishQueueElement.getOperation().toString(),
                    ASSET_KEY, publishQueueElement.getAsset(),
                    HTML_PAGE_KEY, contentlet.isHTMLPage()
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
