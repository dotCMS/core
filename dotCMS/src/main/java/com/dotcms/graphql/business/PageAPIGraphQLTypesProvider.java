package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLList.list;

import com.dotcms.graphql.ContentFields;
import com.dotcms.graphql.datafetcher.LanguageDataFetcher;
import com.dotcms.graphql.datafetcher.MapFieldPropertiesDataFetcher;
import com.dotcms.graphql.datafetcher.UserDataFetcher;
import com.dotcms.graphql.datafetcher.page.PageRenderDataFetcher;
import com.dotcms.graphql.datafetcher.page.TemplateDataFetcher;
import com.dotcms.graphql.datafetcher.page.ViewAsDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import com.dotcms.visitor.domain.Geolocation;
import com.dotcms.visitor.domain.Visitor;
import com.dotcms.visitor.domain.Visitor.AccruedTag;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import eu.bitwalker.useragentutils.Browser;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.PropertyDataFetcher;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Singleton class that provides all the {@link GraphQLType}s needed for the Page API
 */

public enum PageAPIGraphQLTypesProvider implements GraphQLTypesProvider {

    INSTANCE;

    Map<String, GraphQLOutputType> typeMap = new HashMap<>();

    @Override
    public Collection<? extends GraphQLType> getTypes() {

        final Map<String, TypeFetcher> pageFields = new HashMap<>(ContentFields.getContentFields());
        pageFields.put("__icon__", new TypeFetcher(GraphQLString));
        pageFields.put("cachettl", new TypeFetcher(GraphQLString));
        pageFields.put("canEdit", new TypeFetcher(GraphQLBoolean));
        pageFields.put("canLock", new TypeFetcher(GraphQLBoolean));
        pageFields.put("canRead", new TypeFetcher(GraphQLBoolean));
        pageFields.put("deleted", new TypeFetcher(GraphQLBoolean));
        pageFields.put("description", new TypeFetcher(GraphQLString));
        pageFields.put("extension", new TypeFetcher(GraphQLString));
        pageFields.put("friendlyName", new TypeFetcher(GraphQLString));
        pageFields.put("hasLiveVersion", new TypeFetcher(GraphQLBoolean));
        pageFields.put("hasTitleImage", new TypeFetcher(GraphQLBoolean));
        pageFields.put("httpsRequired", new TypeFetcher(GraphQLBoolean));
        pageFields.put("image", new TypeFetcher(GraphQLString));
        pageFields.put("imageContentAsset", new TypeFetcher(GraphQLString));
        pageFields.put("imageVersion", new TypeFetcher(GraphQLString));
        pageFields.put("isContentlet", new TypeFetcher(GraphQLBoolean));
        pageFields.put("liveInode", new TypeFetcher(GraphQLString));
        pageFields.put("mimeType", new TypeFetcher(GraphQLString));
        pageFields.put("name", new TypeFetcher(GraphQLString));
        pageFields.put("pageURI", new TypeFetcher(GraphQLString));
        pageFields.put("pageUrl", new TypeFetcher(GraphQLString));
        pageFields.put("shortyLive", new TypeFetcher(GraphQLString));
        pageFields.put("path", new TypeFetcher(GraphQLString));
        pageFields.put("publishDate", new TypeFetcher(GraphQLString));
        pageFields.put("seoTitle", new TypeFetcher(GraphQLString));
        pageFields.put("seodescription", new TypeFetcher(GraphQLString));
        pageFields.put("shortDescription", new TypeFetcher(GraphQLString));
        pageFields.put("shortyWorking", new TypeFetcher(GraphQLString));
        pageFields.put("sortOrder", new TypeFetcher(GraphQLLong));
        pageFields.put("stInode", new TypeFetcher(GraphQLString));
        pageFields.put("statusIcons", new TypeFetcher(GraphQLString));
        pageFields.put("tags", new TypeFetcher(GraphQLString));
        pageFields.put("template", new TypeFetcher(
                GraphQLTypeReference.typeRef("Template"), new TemplateDataFetcher()));
        pageFields.put("templateIdentifier", new TypeFetcher(GraphQLString));
        pageFields.put("type", new TypeFetcher(GraphQLString));
        pageFields.put("url", new TypeFetcher(GraphQLString));
        pageFields.put("workingInode", new TypeFetcher(GraphQLString));
        pageFields.put("wfExpireDate", new TypeFetcher(GraphQLString));
        pageFields.put("wfExpireTime", new TypeFetcher(GraphQLString));
        pageFields.put("wfNeverExpire", new TypeFetcher(GraphQLString));
        pageFields.put("wfPublishDate", new TypeFetcher(GraphQLString));
        pageFields.put("wfPublishTime", new TypeFetcher(GraphQLString));
        pageFields.put("viewAs", new TypeFetcher(GraphQLTypeReference.typeRef("ViewAs")
                , new ViewAsDataFetcher()));
        pageFields.put("render", new TypeFetcher(GraphQLString, new PageRenderDataFetcher()));
        pageFields.put("urlContentMap",new TypeFetcher(GraphQLTypeReference.typeRef("Contentlet"),
                PropertyDataFetcher.fetching(
                        (Function<Contentlet, Contentlet>) (contentlet)->
                               Try.of(()->((URLMapInfo) contentlet.get("URLMapContent"))
                                       .getContentlet()).getOrNull())));

        typeMap.put("Page", TypeUtil.createObjectType("Page", pageFields));

        final Map<String, TypeFetcher> templateFields = new HashMap<>();
        templateFields.put("iDate", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("type", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("owner", new TypeFetcher(GraphQLTypeReference.typeRef("User"), new UserDataFetcher()));
        templateFields.put("inode", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("identifier", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("source", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("title", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("friendlyName", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("modDate", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("modUser", new TypeFetcher(GraphQLTypeReference.typeRef("User"), new UserDataFetcher()));
        templateFields.put("sortOrder", new TypeFetcher(GraphQLLong, new MapFieldPropertiesDataFetcher()));
        templateFields.put("showOnMenu", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("image", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("drawed", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("drawedBody", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("theme", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("anonymous", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("template", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("versionId", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("versionType", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("deleted", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("working", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("permissionId", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("name", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("live", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("archived", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("locked", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("permissionType", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("categoryId", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("new", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("idate", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("canEdit", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));

        typeMap.put("Template", TypeUtil.createObjectType("Template", templateFields));

        final Map<String, TypeFetcher> viewAsFields = new HashMap<>();
        viewAsFields.put("visitor", new TypeFetcher(GraphQLTypeReference.typeRef("Visitor"),
                new PropertyDataFetcher<ViewAsPageStatus>("visitor")));
        viewAsFields.put("language", new TypeFetcher(GraphQLTypeReference.typeRef("Language"),
                new LanguageDataFetcher()));
        viewAsFields.put("mode", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ViewAsPageStatus, String>)
                        (viewAs)->viewAs.getPageMode().name())));
        viewAsFields.put("persona", new TypeFetcher(GraphQLTypeReference.typeRef("PersonaBaseType"),
                new PropertyDataFetcher<ViewAsPageStatus>("persona")));

        typeMap.put("ViewAs", TypeUtil.createObjectType("ViewAs", viewAsFields));

        final Map<String, TypeFetcher> visitorFields = new HashMap<>();
        visitorFields.put("tags", new TypeFetcher(list(GraphQLTypeReference.typeRef("Tag")),
                new PropertyDataFetcher<Visitor>("tags")));
        visitorFields.put("device", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<String>("device")));
        visitorFields.put("isNew", new TypeFetcher(GraphQLBoolean,
                new PropertyDataFetcher<Boolean>("newVisitor")));
        visitorFields.put("userAgent", new TypeFetcher(GraphQLTypeReference.typeRef("UserAgent"),
                new PropertyDataFetcher<String>("userAgent")));
        visitorFields.put("personas",
                new TypeFetcher(list(GraphQLTypeReference.typeRef("WeightedPersona")),
                PropertyDataFetcher.fetching((Function<Visitor, Set>)
                        (visitor)->visitor.getWeightedPersonas().entrySet())));
        visitorFields.put("persona", new TypeFetcher(GraphQLTypeReference.typeRef("PersonaBaseType"),
                new PropertyDataFetcher<Visitor>("persona")));
        visitorFields.put("geo", new TypeFetcher(GraphQLTypeReference.typeRef("Geolocation"),
                new PropertyDataFetcher<Visitor>("geo")));

        typeMap.put("Visitor", TypeUtil.createObjectType("Visitor", visitorFields));

        final Map<String, TypeFetcher> weightedPersonaFields = new HashMap<>();
        weightedPersonaFields.put("persona", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Map<String, Float>>("key")));
        weightedPersonaFields.put("count", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<Map<String, Float>>("value")));

        typeMap.put("WeightedPersona", TypeUtil.createObjectType("WeightedPersona",
                weightedPersonaFields));

        final Map<String, TypeFetcher> tagFields = new HashMap<>();
        tagFields.put("tag", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<AccruedTag>("tag")));
        tagFields.put("count", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<AccruedTag>("count")));

        typeMap.put("Tag", TypeUtil.createObjectType("Tag", tagFields));

        final Map<String, TypeFetcher> userAgentFields = new HashMap<>();
        userAgentFields.put("operatingSystem", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<AccruedTag>("operatingSystem")));
        userAgentFields.put("browser", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<AccruedTag>("browser")));
        userAgentFields.put("id", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<AccruedTag>("id")));
        userAgentFields.put("browserVersion", new TypeFetcher(GraphQLTypeReference.typeRef("BrowserVersion"),
                new PropertyDataFetcher<AccruedTag>("browserVersion")));

        typeMap.put("UserAgent", TypeUtil.createObjectType("UserAgent", userAgentFields));

        final Map<String, TypeFetcher> browserVersionFields = new HashMap<>();
        browserVersionFields.put("version", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Browser>("version")));
        browserVersionFields.put("majorVersion", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Browser>("majorVersion")));
        browserVersionFields.put("minorVersion", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Browser>("minorVersion")));

        typeMap.put("BrowserVersion", TypeUtil.createObjectType("BrowserVersion", browserVersionFields));

        final Map<String, TypeFetcher> geoFields = new HashMap<>();
        geoFields.put("latitude", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("latitude")));
        geoFields.put("longitude", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("longitude")));
        geoFields.put("country", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("country")));
        geoFields.put("countryCode", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("countryCode")));
        geoFields.put("city", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("city")));
        geoFields.put("continent", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("continent")));
        geoFields.put("continentCode", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("continentCode")));
        geoFields.put("company", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("company")));
        geoFields.put("timezone", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("timezone")));
        geoFields.put("subdivision", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("subdivision")));
        geoFields.put("subdivisionCode", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("subdivisionCode")));
        geoFields.put("ipAddress", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Geolocation>("ipAddress")));

        typeMap.put("Geolocation", TypeUtil.createObjectType("Geolocation",
                geoFields));



        return typeMap.values();

    }

    Map<String, GraphQLOutputType> getTypesMap() {
        return typeMap;
    }
}
