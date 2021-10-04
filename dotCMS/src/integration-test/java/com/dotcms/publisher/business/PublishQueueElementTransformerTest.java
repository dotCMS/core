package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.google.common.base.CaseFormat;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PublishQueueElementTransformerTest {


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link PublishQueueElementTransformer#transform(List)} )}
     * When: A contentlet, language and Content type are created and add into a Bundle
     * Should: return the three assets (No dependency) also with the follow information:
     * - For contentlet: title, inode, structure name
     * - For language: language code, country code, title, structure name
     * - For Content Type: Title
     */
    @Test
    public void getDetailedAssets() throws DotPublisherException {
        final ContentType contentTypeForContentlet = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentTypeForContentlet).nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .addAssets(list(contentlet, language, contentType))
                .nextPersisted();

        final List<PublishQueueElement> queueElementsByBundleId = PublisherAPIImpl.getInstance()
                .getQueueElementsByBundleId(bundle.getId());

        final PublishQueueElementTransformer publishQueueElementTransformer =
                new PublishQueueElementTransformer();

        final List<Map<String, Object>> detailedAssets = publishQueueElementTransformer
                .transform(queueElementsByBundleId);

        assertEquals(3, detailedAssets.size());

        for (final Map<String, Object> detailedAsset : detailedAssets) {
            final String type = detailedAsset.get(PublishQueueElementTransformer.TYPE_KEY).toString();

            if (type.equals(PusheableAsset.CONTENTLET.getType())) {
                assertEquals(contentlet.getTitle(),
                        detailedAsset.get(PublishQueueElementTransformer.TITLE_KEY));
                assertEquals(contentlet.getInode(),
                        detailedAsset.get(PublishQueueElementTransformer.INODE_KEY));
                assertEquals(contentlet.getContentType().name(),
                        detailedAsset.get(PublishQueueElementTransformer.CONTENT_TYPE_NAME_KEY));
            } else if (type.equals(PusheableAsset.CONTENT_TYPE.getType())) {
                assertEquals(contentType.name(),
                        detailedAsset.get(PublishQueueElementTransformer.TITLE_KEY));
            } else if (type.equals(PusheableAsset.LANGUAGE.getType())) {
                assertEquals(language.getLanguageCode(),
                        detailedAsset.get(PublishQueueElementTransformer.LANGUAGE_CODE_KEY));
                assertEquals(language.getCountryCode(),
                        detailedAsset.get(PublishQueueElementTransformer.COUNTRY_CODE_KEY));
                assertEquals(language.getLanguage() + "(" + language.getCountryCode() + ")",
                        detailedAsset.get(PublishQueueElementTransformer.TITLE_KEY));
                assertEquals(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, PusheableAsset.LANGUAGE.getType()),
                        detailedAsset.get(PublishQueueElementTransformer.CONTENT_TYPE_NAME_KEY));
            } else {
                throw new AssertionError(type + " Type not expected");
            }
        }
    }

}
