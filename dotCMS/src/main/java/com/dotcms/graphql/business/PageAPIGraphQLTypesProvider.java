package com.dotcms.graphql.business;

import static com.dotcms.graphql.InterfaceType.DOT_CONTENTLET;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.OWNER_KEY;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.scalars.ExtendedScalars.GraphQLLong;
import static graphql.schema.GraphQLList.list;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.graphql.ContentFields;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.datafetcher.LanguageDataFetcher;
import com.dotcms.graphql.datafetcher.MapFieldPropertiesDataFetcher;
import com.dotcms.graphql.datafetcher.UserDataFetcher;
import com.dotcms.graphql.datafetcher.page.ContainersDataFetcher;
import com.dotcms.graphql.datafetcher.page.LayoutDataFetcher;
import com.dotcms.graphql.datafetcher.page.PageRenderDataFetcher;
import com.dotcms.graphql.datafetcher.page.RenderedContainersDataFetcher;
import com.dotcms.graphql.datafetcher.page.RunningExperimentFetcher;
import com.dotcms.graphql.datafetcher.page.TemplateDataFetcher;
import com.dotcms.graphql.datafetcher.page.VanityURLFetcher;
import com.dotcms.graphql.datafetcher.page.ViewAsDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.visitor.domain.Geolocation;
import com.dotcms.visitor.domain.Visitor;
import com.dotcms.visitor.domain.Visitor.AccruedTag;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import com.dotmarketing.portlets.templates.design.bean.ContainerHolder;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.Sidebar;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutColumn;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import com.dotmarketing.util.Logger;
import eu.bitwalker.useragentutils.Browser;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.PropertyDataFetcher;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

/**
 * Singleton class that provides all the {@link GraphQLType}s needed for the Page API
 */

public enum PageAPIGraphQLTypesProvider implements GraphQLTypesProvider {

    INSTANCE;

    public static final String DOT_PAGE = "DotPage";
    public static final String DOT_PAGE_TEMPLATE = "DotPageTemplate";
    public static final String DOT_PAGE_VIEW_AS = "DotPageViewAs";
    public static final String DOT_PAGE_VISITOR = "DotPageVisitor";
    public static final String DOT_PAGE_WEIGHTED_PERSONA = "DotPageWeightedPersona";
    public static final String DOT_PAGE_TAG = "DotPageTag";
    public static final String DOT_PAGE_USER_AGENT = "DotPageUserAgent";
    public static final String DOT_PAGE_BROWSER_VERSION = "DotPageBrowserVersion";
    public static final String DOT_PAGE_GEOLOCATION = "DotPageGeolocation";
    public static final String DOT_PAGE_LAYOUT = "DotPageLayout";
    public static final String DOT_PAGE_SIDEBAR = "DotPageSidebar";
    public static final String DOT_PAGE_LAYOUT_ROW = "DotPageLayoutRow";
    public static final String DOT_PAGE_LAYOUT_COLUMN = "DotPageLayoutColumn";
    public static final String DOT_PAGE_CONTAINER_UUID = "DotPageContainerUUID";
    public static final String DOT_PAGE_RENDERED_CONTAINER = "DotPageRenderedContainer";
    public static final String DOT_PAGE_CONTAINER_STRUCTURE = "DotPageContainerStructure";
    public static final String DOT_PAGE_CONTAINER_CONTENTLETS = "DotPageContainerContentlets";
    public static final String DOT_PAGE_CONTAINER = "DotPageContainer";
    public static final String DOT_PAGE_VANITY_URL = "DotPageVanityURL";
    public static final String DOT_PAGE_BODY = "DotPageBody";
    Map<String, GraphQLOutputType> typesMap = new HashMap<>();

    @Override
    public Collection<? extends GraphQLType> getTypes() {

        Logger.debug(this, ()-> "Creating Page API GraphQL Types");
        typesMap.clear();

        // Page type
        final Map<String, TypeFetcher> pageFields = new HashMap<>(ContentFields.getContentFields());
        pageFields.put("icon", new TypeFetcher(GraphQLString));
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
                GraphQLTypeReference.typeRef(DOT_PAGE_TEMPLATE), new TemplateDataFetcher()));
        pageFields.put("templateIdentifier", new TypeFetcher(GraphQLString));
        pageFields.put("type", new TypeFetcher(GraphQLString));
        pageFields.put("url", new TypeFetcher(GraphQLString));
        pageFields.put("workingInode", new TypeFetcher(GraphQLString));
        pageFields.put("wfExpireDate", new TypeFetcher(GraphQLString));
        pageFields.put("wfExpireTime", new TypeFetcher(GraphQLString));
        pageFields.put("wfNeverExpire", new TypeFetcher(GraphQLString));
        pageFields.put("wfPublishDate", new TypeFetcher(GraphQLString));
        pageFields.put("wfPublishTime", new TypeFetcher(GraphQLString));
        pageFields.put("viewAs", new TypeFetcher(GraphQLTypeReference.typeRef(DOT_PAGE_VIEW_AS)
                , new ViewAsDataFetcher()));
        pageFields.put("render", new TypeFetcher(GraphQLString, new PageRenderDataFetcher()));
        pageFields.put("urlContentMap",new TypeFetcher(GraphQLTypeReference.typeRef(DOT_CONTENTLET),
                PropertyDataFetcher.fetching(
                        (Function<Contentlet, Contentlet>) (contentlet)->
                               Try.of(()->((URLMapInfo) contentlet.get("URLMapContent"))
                                       .getContentlet()).getOrNull())));

        pageFields.put("layout", new TypeFetcher(GraphQLTypeReference.typeRef(DOT_PAGE_LAYOUT),
                new LayoutDataFetcher()));
        pageFields.put("containers", new TypeFetcher(list(GraphQLTypeReference.typeRef(DOT_PAGE_CONTAINER)),
                new ContainersDataFetcher()));
        pageFields.put("vanityUrl", new TypeFetcher(
                GraphQLTypeReference.typeRef(DOT_PAGE_VANITY_URL), new VanityURLFetcher())
        );
        pageFields.put("runningExperimentId", new TypeFetcher(
                GraphQLString, new RunningExperimentFetcher())
        );
        
        // Expose the page as its underlying contentlet type to enable inline fragments
        // for accessing content-type-specific fields like SEO metadata
        pageFields.put("page", new TypeFetcher(
                GraphQLTypeReference.typeRef(DOT_CONTENTLET),
                PropertyDataFetcher.fetching((Contentlet contentlet) -> contentlet)));

        typesMap.put(DOT_PAGE, TypeUtil.createObjectType(DOT_PAGE, pageFields));

        // Template type
        final Map<String, TypeFetcher> templateFields = new HashMap<>();
        templateFields.put("iDate", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("type", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("owner", new TypeFetcher(GraphQLTypeReference.typeRef(
                CustomFieldType.USER.getTypeName()), new UserDataFetcher()));
        templateFields.put("inode", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("identifier", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("source", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("title", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("friendlyName", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("modDate", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("modUser", new TypeFetcher(GraphQLTypeReference.typeRef(CustomFieldType.USER.getTypeName()), new UserDataFetcher()));
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

        typesMap.put(DOT_PAGE_TEMPLATE, TypeUtil.createObjectType(DOT_PAGE_TEMPLATE, templateFields));

        // ViewAs type
        final Map<String, TypeFetcher> viewAsFields = new HashMap<>();
        viewAsFields.put("visitor", new TypeFetcher(GraphQLTypeReference.typeRef(DOT_PAGE_VISITOR),
                new PropertyDataFetcher<ViewAsPageStatus>("visitor")));
        viewAsFields.put("language", new TypeFetcher(GraphQLTypeReference.typeRef(CustomFieldType.LANGUAGE.getTypeName()),
                new LanguageDataFetcher()));
        viewAsFields.put("mode", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ViewAsPageStatus, String>)
                        (viewAs)->viewAs.getPageMode().name())));
        viewAsFields.put("variantId", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching(ViewAsPageStatus::getVariantId)));
        if(LicenseManager.getInstance().isEnterprise()) {
            viewAsFields
                    .put("persona", new TypeFetcher(GraphQLTypeReference.typeRef("PersonaBaseType"),
                            new PropertyDataFetcher<ViewAsPageStatus>("persona")));
        }


        typesMap.put(DOT_PAGE_VIEW_AS, TypeUtil.createObjectType(DOT_PAGE_VIEW_AS, viewAsFields));

        // Visitor type
        final Map<String, TypeFetcher> visitorFields = new HashMap<>();
        visitorFields.put("tags", new TypeFetcher(list(GraphQLTypeReference.typeRef(DOT_PAGE_TAG)),
                new PropertyDataFetcher<Visitor>("tags")));
        visitorFields.put("device", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<String>("device")));
        visitorFields.put("isNew", new TypeFetcher(GraphQLBoolean,
                new PropertyDataFetcher<Boolean>("newVisitor")));
        visitorFields.put("userAgent", new TypeFetcher(GraphQLTypeReference.typeRef(DOT_PAGE_USER_AGENT),
                new PropertyDataFetcher<String>("userAgent")));
        visitorFields.put("personas",
                new TypeFetcher(list(GraphQLTypeReference.typeRef(DOT_PAGE_WEIGHTED_PERSONA)),
                PropertyDataFetcher.fetching((Function<Visitor, Set>)
                        (visitor)->visitor.getWeightedPersonas().entrySet())));
        if(LicenseManager.getInstance().isEnterprise()) {
            visitorFields
                    .put("persona", new TypeFetcher(GraphQLTypeReference.typeRef("PersonaBaseType"),
                            new PropertyDataFetcher<Visitor>("persona")));
        }

        visitorFields.put("geo", new TypeFetcher(GraphQLTypeReference.typeRef(DOT_PAGE_GEOLOCATION),
        PropertyDataFetcher.fetching((Function<Visitor, Geolocation>)
                (visitor)->Try.of(visitor::getGeo).getOrNull())));

        typesMap.put(DOT_PAGE_VISITOR, TypeUtil.createObjectType(DOT_PAGE_VISITOR, visitorFields));

        // WeightedPersona type
        final Map<String, TypeFetcher> weightedPersonaFields = new HashMap<>();
        weightedPersonaFields.put("persona", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Map<String, Float>>("key")));
        weightedPersonaFields.put("count", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<Map<String, Float>>("value")));

        typesMap.put(DOT_PAGE_WEIGHTED_PERSONA, TypeUtil.createObjectType(DOT_PAGE_WEIGHTED_PERSONA,
                weightedPersonaFields));

        // Tag type
        final Map<String, TypeFetcher> tagFields = new HashMap<>();
        tagFields.put("tag", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<AccruedTag>("tag")));
        tagFields.put("count", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<AccruedTag>("count")));

        typesMap.put(DOT_PAGE_TAG, TypeUtil.createObjectType(DOT_PAGE_TAG, tagFields));

        // UserAgent type
        final Map<String, TypeFetcher> userAgentFields = new HashMap<>();
        userAgentFields.put("operatingSystem", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<AccruedTag>("operatingSystem")));
        userAgentFields.put("browser", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<AccruedTag>("browser")));
        userAgentFields.put("id", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<AccruedTag>("id")));
        userAgentFields.put("browserVersion", new TypeFetcher(GraphQLTypeReference.typeRef(DOT_PAGE_BROWSER_VERSION),
                new PropertyDataFetcher<AccruedTag>("browserVersion")));

        typesMap.put(
                DOT_PAGE_USER_AGENT, TypeUtil.createObjectType(DOT_PAGE_USER_AGENT, userAgentFields));

        // BrowserVersion type
        final Map<String, TypeFetcher> browserVersionFields = new HashMap<>();
        browserVersionFields.put("version", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Browser>("version")));
        browserVersionFields.put("majorVersion", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Browser>("majorVersion")));
        browserVersionFields.put("minorVersion", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Browser>("minorVersion")));

        typesMap.put(DOT_PAGE_BROWSER_VERSION, TypeUtil.createObjectType(DOT_PAGE_BROWSER_VERSION, browserVersionFields));

        // Geolocation type
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

        typesMap.put(DOT_PAGE_GEOLOCATION, TypeUtil.createObjectType(DOT_PAGE_GEOLOCATION,
                geoFields));

        // Layout type
        final Map<String, TypeFetcher> layoutFields = new HashMap<>();
        layoutFields.put("width", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<TemplateLayout>("width")));
        layoutFields.put("title", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<TemplateLayout>("title")));
        layoutFields.put("header", new TypeFetcher(GraphQLBoolean,
                new PropertyDataFetcher<TemplateLayout>("header")));
        layoutFields.put("footer", new TypeFetcher(GraphQLBoolean,
                new PropertyDataFetcher<TemplateLayout>("footer")));
        layoutFields.put("body", new TypeFetcher(GraphQLTypeReference.typeRef(DOT_PAGE_BODY),
                new PropertyDataFetcher<TemplateLayout>("body")));
        layoutFields.put("sidebar", new TypeFetcher(GraphQLTypeReference.typeRef(DOT_PAGE_SIDEBAR),
                new PropertyDataFetcher<TemplateLayout>("sidebar")));

        typesMap.put(DOT_PAGE_LAYOUT, TypeUtil.createObjectType(DOT_PAGE_LAYOUT,
                layoutFields));

        // Sidebar type
        final Map<String, TypeFetcher> sidebarFields = new HashMap<>();
        sidebarFields.put("widthPercent", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<Sidebar>("widthPercent")));
        sidebarFields.put("width", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<Sidebar>("width")));
        sidebarFields.put("location", new TypeFetcher((GraphQLString),
                new PropertyDataFetcher<Sidebar>("location")));
        sidebarFields.put("preview", new TypeFetcher((GraphQLBoolean),
                new PropertyDataFetcher<Sidebar>("preview")));
        sidebarFields.put("containers", new TypeFetcher(list(GraphQLTypeReference.typeRef(DOT_PAGE_CONTAINER_UUID)),
                new PropertyDataFetcher<ContainerHolder>("containers")));

        typesMap.put(DOT_PAGE_SIDEBAR, TypeUtil.createObjectType(DOT_PAGE_SIDEBAR,
                sidebarFields));

        // Body type
        final Map<String, TypeFetcher> bodyFields = new HashMap<>();
        bodyFields.put("rows", new TypeFetcher(list(GraphQLTypeReference.typeRef(DOT_PAGE_LAYOUT_ROW)),
                new PropertyDataFetcher<TemplateLayout>("rows")));

        typesMap.put(DOT_PAGE_BODY, TypeUtil.createObjectType(DOT_PAGE_BODY,
                bodyFields));

        // LayoutRow type
        final Map<String, TypeFetcher> rowFields = new HashMap<>();
        rowFields.put("columns", new TypeFetcher(list(GraphQLTypeReference.typeRef(DOT_PAGE_LAYOUT_COLUMN)),
                new PropertyDataFetcher<TemplateLayoutRow>("columns")));
        rowFields.put("styleClass", new TypeFetcher((GraphQLString),
                new PropertyDataFetcher<TemplateLayoutRow>("styleClass")));

        typesMap.put(DOT_PAGE_LAYOUT_ROW, TypeUtil.createObjectType(DOT_PAGE_LAYOUT_ROW,
                rowFields));

        // LayoutColumn type
        final Map<String, TypeFetcher> columnFields = new HashMap<>();
        columnFields.put("widthPercent", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<TemplateLayoutColumn>("widthPercent")));
        columnFields.put("width", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<TemplateLayoutColumn>("width")));
        columnFields.put("leftOffset", new TypeFetcher((GraphQLLong),
                new PropertyDataFetcher<TemplateLayoutColumn>("leftOffset")));
        columnFields.put("left", new TypeFetcher((GraphQLLong),
                new PropertyDataFetcher<TemplateLayoutColumn>("left")));
        columnFields.put("styleClass", new TypeFetcher((GraphQLString),
                new PropertyDataFetcher<TemplateLayoutColumn>("styleClass")));
        columnFields.put("preview", new TypeFetcher((GraphQLBoolean),
                new PropertyDataFetcher<TemplateLayoutColumn>("preview")));
        columnFields.put("containers", new TypeFetcher(list(GraphQLTypeReference.typeRef(DOT_PAGE_CONTAINER_UUID)),
                new PropertyDataFetcher<ContainerHolder>("containers")));

        typesMap.put(DOT_PAGE_LAYOUT_COLUMN, TypeUtil.createObjectType(DOT_PAGE_LAYOUT_COLUMN,
                columnFields));

        // ContainerUUID type
        final Map<String, TypeFetcher> containerUUIDFields = new HashMap<>();
        containerUUIDFields.put("identifier", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<ContainerUUID>("identifier")));
        containerUUIDFields.put("uuid", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<ContainerUUID>("uuid")));

        typesMap.put(DOT_PAGE_CONTAINER_UUID, TypeUtil.createObjectType(DOT_PAGE_CONTAINER_UUID,
                containerUUIDFields));

        // Container type
        final Map<String, TypeFetcher> containerFields = new HashMap<>();
        containerFields.put(ARCHIVED_KEY, new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isArchived())
                                .getOrElse(false))));
        containerFields.put("categoryId", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getCategoryId())));
        containerFields.put("identifier", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getIdentifier())));
        containerFields.put("deleted", new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isDeleted())
                                .getOrElse(false))));
        containerFields.put("friendlyName", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getFriendlyName())));
        containerFields.put("host", new TypeFetcher(GraphQLTypeReference.typeRef(CustomFieldType.SITE.getTypeName()),
                PropertyDataFetcher.fetching((Function<ContainerRaw, Host>)
                        (containerRaw)-> {
                            final Container container = containerRaw.getContainer();
                            if (FileAssetContainerUtil.getInstance().isFileAssetContainer(container)) {
                                final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;
                                return fileAssetContainer.getHost();
                            } else {
                                return null;
                            }
                        })));
        containerFields.put("iDate", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getiDate().toString())));
        containerFields.put("idate", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getIDate().toString())));
        containerFields.put("inode", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getInode())));
        containerFields.put("languageId", new TypeFetcher(GraphQLLong,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Long>)
                        (containerRaw)-> {
                            final Container container = containerRaw.getContainer();
                            if (FileAssetContainerUtil.getInstance().isFileAssetContainer(container)) {
                                final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;
                                return fileAssetContainer.getLanguageId();
                            } else {
                                return 0L;
                            }
                        })));
        containerFields.put("live", new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isLive())
                                .getOrElse(false))));
        containerFields.put("locked", new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isLocked())
                                .getOrElse(false))));
        containerFields.put("luceneQuery", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getLuceneQuery())));
        containerFields.put("maxContentlets", new TypeFetcher(GraphQLInt,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Integer>)
                        (containerRaw)->containerRaw.getContainer().getMaxContentlets())));
        containerFields.put("modDate", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getModDate().toString())));
        containerFields.put(MOD_USER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef(CustomFieldType.USER.getTypeName()),
                new UserDataFetcher()));
        containerFields.put("name", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getName())));
        containerFields.put("new", new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isNew())
                                .getOrElse(false))));
        containerFields.put("notes", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getNotes())));
        containerFields.put(OWNER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef(CustomFieldType.USER.getTypeName()),
                new UserDataFetcher()));
        containerFields.put("parentPermissionable", new TypeFetcher(
                GraphQLTypeReference.typeRef(CustomFieldType.SITE.getTypeName()),
                PropertyDataFetcher.fetching((Function<ContainerRaw, Host>)
                        (containerRaw)-> (Host) Try.of(()->
                                containerRaw.getContainer().getParentPermissionable())
                                .getOrElse((Permissionable) null))));
        containerFields.put("path", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)-> {
                            final Container container = containerRaw.getContainer();
                            if (FileAssetContainerUtil.getInstance().isFileAssetContainer(container)) {
                                final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;

                                return FileAssetContainerUtil.getInstance().getFullPath(fileAssetContainer);
                            } else {
                                return null;
                            }
                        })));
        containerFields.put("permissionId", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getPermissionId())));
        containerFields.put("permissionType", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getPermissionType())));
        containerFields.put("postLoop", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getPostLoop())));
        containerFields.put("preLoop", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getPreLoop())));
        containerFields.put("showOnMenu", new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isShowOnMenu())
                                .getOrElse(false))));
        containerFields.put("sortOrder", new TypeFetcher(GraphQLInt,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Integer>)
                        (containerRaw)->containerRaw.getContainer().getSortOrder())));
        containerFields.put("source", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getSource().name())));
        containerFields.put("staticify", new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isStaticify())
                                .getOrElse(false))));
        containerFields.put("title", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getTitle())));
        containerFields.put("type", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getType())));
        containerFields.put("useDiv", new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isUseDiv())
                                .getOrElse(false))));
        containerFields.put("versionId", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getVersionId())));
        containerFields.put("versionType", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<ContainerRaw, String>)
                        (containerRaw)->containerRaw.getContainer().getVersionType())));
        containerFields.put("working", new TypeFetcher(GraphQLBoolean,
                PropertyDataFetcher.fetching((Function<ContainerRaw, Boolean>)
                        (containerRaw)->Try.of(()->containerRaw.getContainer().isWorking())
                                .getOrElse(false))));

        containerFields.put("rendered", new TypeFetcher(list(GraphQLTypeReference.typeRef(DOT_PAGE_RENDERED_CONTAINER))
                , new RenderedContainersDataFetcher()));

        containerFields.put("containerStructures", new TypeFetcher(
                list(GraphQLTypeReference.typeRef(DOT_PAGE_CONTAINER_STRUCTURE)),
                PropertyDataFetcher.fetching(ContainerRaw::getContainerStructures)));

        containerFields.put("containerContentlets", new TypeFetcher(
                list(GraphQLTypeReference.typeRef(DOT_PAGE_CONTAINER_CONTENTLETS)),
                PropertyDataFetcher.fetching((Function<ContainerRaw, Set<Entry<String, List<Contentlet>>>>)
                        (containerRaw)->containerRaw.getContentlets().entrySet())));

        // RenderedContainer type
        final Map<String, TypeFetcher> renderedContainerFields = new HashMap<>();
        renderedContainerFields.put("uuid", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<Entry<String, String>, String>)
                        Entry::getKey)));
        renderedContainerFields.put("render", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<Entry<String, String>, String>)
                        Entry::getValue)));

        typesMap.put(DOT_PAGE_RENDERED_CONTAINER, TypeUtil.createObjectType(DOT_PAGE_RENDERED_CONTAINER,
                renderedContainerFields));

        // ContainerStructure type
        final Map<String, TypeFetcher> containerStructureFields = new HashMap<>();
        containerStructureFields.put("id", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching(ContainerStructure::getId)));
        containerStructureFields.put("structureId", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching(ContainerStructure::getStructureId)));
        containerStructureFields.put("containerInode", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching(ContainerStructure::getContainerInode)));
        containerStructureFields.put("containerId", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching(ContainerStructure::getContainerId)));
        containerStructureFields.put("code", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching(ContainerStructure::getCode)));
        containerStructureFields.put("contentTypeVar", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching(ContainerStructure::getContentTypeVar)));

        typesMap.put(DOT_PAGE_CONTAINER_STRUCTURE, TypeUtil.createObjectType(DOT_PAGE_CONTAINER_STRUCTURE,
                containerStructureFields));

        // ContainerContentlets type
        final Map<String, TypeFetcher> containerContentletsFields = new HashMap<>();
        containerContentletsFields.put("uuid", new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<Entry<String, List<Contentlet>>, String>)
                        Entry::getKey)));
        containerContentletsFields.put("contentlets", new TypeFetcher(
                list(GraphQLTypeReference.typeRef(DOT_CONTENTLET)),
                PropertyDataFetcher.fetching((Function<Entry<String, List<Contentlet>>, List<Contentlet>>)
                        Entry::getValue)));

        typesMap.put(DOT_PAGE_CONTAINER_CONTENTLETS, TypeUtil.createObjectType(DOT_PAGE_CONTAINER_CONTENTLETS,
                containerContentletsFields));

        typesMap.put(DOT_PAGE_CONTAINER, TypeUtil.createObjectType(DOT_PAGE_CONTAINER,
                containerFields));

        // Vanity URL type
        final Map<String, TypeFetcher> vanityURLFields = getVanityURLFields();
        typesMap.put(DOT_PAGE_VANITY_URL,
                TypeUtil.createObjectType(DOT_PAGE_VANITY_URL, vanityURLFields));

        return typesMap.values();
    }

    /**
     * Get the fields for the Vanity URL type
     *
     * @return a map of field names to TypeFetchers
     */
    private Map<String, TypeFetcher> getVanityURLFields() {

        final Map<String, TypeFetcher> vanityURLFields = new HashMap<>();

        vanityURLFields.put("id", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<CachedVanityUrl>("vanityUrlId"))
        );
        vanityURLFields.put("uri", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<CachedVanityUrl>("url"))
        );
        vanityURLFields.put("siteId", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<CachedVanityUrl>("siteId"))
        );
        vanityURLFields.put("languageId", new TypeFetcher(GraphQLLong,
                new PropertyDataFetcher<CachedVanityUrl>("languageId"))
        );
        vanityURLFields.put("forwardTo", new TypeFetcher(GraphQLString,
                new PropertyDataFetcher<CachedVanityUrl>("forwardTo"))
        );
        vanityURLFields.put("action", new TypeFetcher(GraphQLInt,
                new PropertyDataFetcher<CachedVanityUrl>("response"))
        );
        vanityURLFields.put("order", new TypeFetcher(GraphQLInt,
                new PropertyDataFetcher<CachedVanityUrl>("order"))
        );

        return vanityURLFields;
    }

    Map<String, GraphQLOutputType> getTypesMap() {
        return typesMap;
    }
}
