package com.dotcms.publisher.business;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.base.CaseFormat;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.StringPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dotcms.publisher.ajax.RemotePublishAjaxAction.ADD_ALL_CATEGORIES_TO_BUNDLE_KEY;

/**
 * Util class to transform {@link PublishQueueElement} into Map
 */
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
        return getMap(publishQueueElement.getAsset(),
                publishQueueElement.getType(), publishQueueElement.getOperation());
    }

    public Map<String, Object> getMap(final String id, final String type) {
        return getMap(id, type, -1);
    }

    private static Map<String, Object> getMap(final String id, final String type,
            final Integer operation) {

        final Map<String, Object> result;

        if (isContentlet(type)) {
            result = getMapForContentlet(id);
        } else if (isLanguage(type)) {
            result = getMapForLanguage(id);
        } else if (ADD_ALL_CATEGORIES_TO_BUNDLE_KEY.equals(id)) {
            result = getMapForCategory();
        } else {

            String title = PublishAuditUtil.getInstance().getTitle(type, id);

            if (title.equals( type )) {
                title = "";
                Logger.warn( PublishQueueElementTransformer.class, () ->
                        "Unable to find Asset of type: [" + type + "] with identifier: [" + id + "]" );

                try{
                    Logger.info( PublishQueueElementTransformer.class, "Cleaning Publishing Queue, identifier [" + id + "] no longer exists");
                    PublisherAPIImpl.getInstance().deleteElementFromPublishQueueTable(type);
                } catch (DotPublisherException dpe){
                    Logger.warn(PublishQueueElementTransformer.class,
                            () -> "Unable to delete Asset from Publishing Queue with identifier: [" + type + "]", dpe );
                }
            }

            result = new HashMap<>(Map.of(TITLE_KEY, UtilMethods.isSet(title) ? title : StringPool.BLANK));
        }

        if (Objects.nonNull(result)) {
            result.putAll(Map.of(
                    TYPE_KEY, type,
                    OPERATION_KEY, operation,
                    ASSET_KEY, id
            ));
        }

        return result;
    }

    private static Map<String, Object> getMapForLanguage(final String id) {

        final Language language = APILocator.getLanguageAPI().getLanguage(id);

        return new HashMap<>(UtilMethods.isSet(language) ?
                Map.of(
                    TITLE_KEY, String.format( "%s(%s)", language.getLanguage(), language.getCountryCode()),
                    LANGUAGE_CODE_KEY, language.getLanguageCode(),
                    COUNTRY_CODE_KEY, language.getCountryCode(),
                    CONTENT_TYPE_NAME_KEY,  CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL,
                            PusheableAsset.LANGUAGE.getType())
                ) :
                Map.of(TITLE_KEY, id));
    }

    private static Map<String, Object> getMapForCategory(){
        try {
            return new HashMap<>(Map.of(TITLE_KEY, LanguageUtil.get("Syncing_all_Categories")));
        } catch (LanguageException e) {
            return new HashMap<>(Map.of(TITLE_KEY, "Syncing All Categories "));
        }
    }

    private static Map<String, Object> getMapForContentlet(final String id) {

        final Contentlet contentlet;

        try {
            contentlet = PublishAuditUtil.getInstance()
                    .findContentletByIdentifier(id);

            return new HashMap<>(UtilMethods.isSet(contentlet) ?
                    Map.of(
                        TITLE_KEY, contentlet.getTitle(),
                        INODE_KEY, contentlet.getInode(),
                        CONTENT_TYPE_NAME_KEY, contentlet.getContentType().name(),
                        HTML_PAGE_KEY, contentlet.isHTMLPage()
                    ) : Map.of(TITLE_KEY, id, INODE_KEY, id));
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(PublishQueueElementTransformer.class, e.getMessage());
            return null;
        }
    }

    private static boolean isContentlet(final String type) {
        return type.equals(PusheableAsset.CONTENTLET.getType());
    }

    private static boolean isLanguage(final String type) {
        return type.equals(PusheableAsset.LANGUAGE.getType());
    }
}
