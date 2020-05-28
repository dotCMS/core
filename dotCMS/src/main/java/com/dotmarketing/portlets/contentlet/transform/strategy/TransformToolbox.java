package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.DISABLED_WYSIWYG_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DISABLE_WORKFLOW;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DONT_VALIDATE_ME;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.DOT_NAME_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IS_TEST_MODE;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LAST_REVIEW_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.NULL_PROPERTIES;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.REVIEW_INTERNAL_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ACTION_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ASSIGN_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_COMMENTS_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_IN_PROGRESS;
import static com.liferay.portal.language.LanguageUtil.getLiteralLocale;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.ContentHelper;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TransformToolbox {

    //This set contains all the properties that we want to prevent from making it into the final contentlet or transformed map.
    public static final Set<String> privateInternalProperties = ImmutableSet
            .of(NULL_PROPERTIES,
                DISABLE_WORKFLOW,
                DONT_VALIDATE_ME,
                LAST_REVIEW_KEY,
                REVIEW_INTERNAL_KEY,
                DISABLED_WYSIWYG_KEY,
                DOT_NAME_KEY,
                WORKFLOW_IN_PROGRESS,
                WORKFLOW_ASSIGN_KEY,
                WORKFLOW_ACTION_KEY,
                WORKFLOW_COMMENTS_KEY,
                DONT_VALIDATE_ME,
                IS_TEST_MODE
            );

    static final String NOT_APPLICABLE = "N/A";

    final IdentifierAPI identifierAPI;
    final HostAPI hostAPI;
    final LanguageAPI languageAPI;
    final FileAssetAPI fileAssetAPI;
    final VersionableAPI versionableAPI;
    final UserAPI userAPI;
    final ContentletAPI contentletAPI;
    final HTMLPageAssetAPI htmlPageAssetAPI;
    final ContentHelper contentHelper;

    @VisibleForTesting
    public TransformToolbox(final IdentifierAPI identifierAPI,
            final HostAPI hostAPI,
            final LanguageAPI languageAPI,
            final FileAssetAPI fileAssetAPI, final VersionableAPI versionableAPI,
            final UserAPI userAPI,  final ContentletAPI contentletAPI,
            final HTMLPageAssetAPI htmlPageAssetAPI,
            final ContentHelper contentHelper) {
        this.identifierAPI = identifierAPI;
        this.hostAPI = hostAPI;
        this.languageAPI = languageAPI;
        this.fileAssetAPI = fileAssetAPI;
        this.versionableAPI = versionableAPI;
        this.userAPI = userAPI;
        this.contentletAPI = contentletAPI;
        this.htmlPageAssetAPI = htmlPageAssetAPI;
        this.contentHelper = contentHelper;
    }

    public TransformToolbox() {
        this(APILocator.getIdentifierAPI(), APILocator.getHostAPI(), APILocator.getLanguageAPI(),
                APILocator.getFileAssetAPI(), APILocator.getVersionableAPI(),
                APILocator.getUserAPI(), APILocator.getContentletAPI(),
                APILocator.getHTMLPageAssetAPI(), ContentHelper.getInstance());
    }

    public static Contentlet copyContentlet(final Contentlet contentlet) {
        final Contentlet newContentlet = new Contentlet();
        if (null != contentlet && null != contentlet.getMap()) {
            newContentlet.getMap().putAll(contentlet.getMap());
        }
        return newContentlet;
    }

    static Map<String, Object> mapLanguage(final Language language, final boolean wrapAsMap) {

        final Builder<String, Object> builder = new Builder<>();

        builder
                .put("languageId", language.getId())
                .put("language", language.getLanguage())
                .put("languageCode", language.getLanguageCode())
                .put("country", language.getCountry())
                .put("countryCode", language.getCountryCode())
                .put("languageFlag", getLiteralLocale(language.getLanguageCode(), language.getCountryCode()));

        final String iso = UtilMethods.isSet(language.getCountryCode())
                ? language.getLanguageCode() + StringPool.DASH + language.getCountryCode()
                : language.getLanguageCode();
        builder.put("isoCode", iso.toLowerCase());

        if(wrapAsMap){
            builder.put("id", language.getId());
            return ImmutableMap.of("languageMap", builder.build(), "language",language.getLanguage());
        }
        return builder.build();
    }

    static Map<String, Object> mapIdentifier(final Identifier identifier, final boolean wrapAsMap) {

        final Builder<String, Object> builder = new Builder<>();
        builder
                .put("identifier", identifier.getId())
                .put("parentPath", identifier.getParentPath())
                .put("hostId", identifier.getHostId());

        if(wrapAsMap){
            builder.put("id", identifier.getId());
            return ImmutableMap.of("identifierMap", builder.build(), "identifier",identifier.getId());
        }
        return builder.build();
    }

    public static Map<String, Object> transform(final Field field, final Contentlet contentlet) {
        File file;
        try {
            file = contentlet.getBinary(field.variable());
        } catch (IOException e) {
            throw new DotStateException(e);
        }

        if (file == null) {
            return emptyMap();
        }

        return transform(file, contentlet, field);
    }

    public static Map<String, Object> transform(final File file, final Contentlet contentlet, final Field field) {
        DotPreconditions.checkNotNull(file, IllegalArgumentException.class, "File can't be null");
        final Map<String, Object> map = new HashMap<>();

        map.put("versionPath", "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getInode()) + "/" + field.variable() + "/" + file.getName());
        final int contentLanguageSize = Try
                .of(()->APILocator.getLanguageAPI().getLanguages()).getOrElse(emptyList()).size();
        map.put("idPath", "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getIdentifier()) + "/" + field.variable() + "/" + file.getName()
                + (contentLanguageSize > 1?"?language_id=" + contentlet.getLanguageId(): StringPool.BLANK));
        map.put("name", file.getName());
        map.put("size", file.length());
        map.put("mime", Config.CONTEXT.getMimeType(file.getName()));
        map.put("isImage", UtilMethods.isImage(file.getName()));

        return map;
    }


}
