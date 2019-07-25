package com.dotcms.rest.api.v1.personalization;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginatorOrdered;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import io.vavr.API;
import io.vavr.control.Try;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class PersonalizationPersonaPageViewPaginator implements PaginatorOrdered<PersonalizationPersonaPageView> {

    public  static final String PAGE_ID        = "pageId";
    private static final String PERSONAS_QUERY = "+contentType:persona +live:true +deleted:false +working:true +conhost:";


    private final MultiTreeAPI  multiTreeAPI;
    private final ContentletAPI contentletAPI;
    private final Map<String,Object> defaultPersona;
    public PersonalizationPersonaPageViewPaginator() {
        this(APILocator.getPersonaAPI(), APILocator.getMultiTreeAPI(),
                APILocator.getContentletAPI());

    }

    public PersonalizationPersonaPageViewPaginator(final PersonaAPI personaAPI,
                                                   final MultiTreeAPI multiTreeAPI,
                                                   final ContentletAPI contentletAPI) {


        this.multiTreeAPI = multiTreeAPI;
        this.contentletAPI = contentletAPI;
        
        final Map<String, Object> map = new HashMap<>();
        map.put("stInode", PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
        map.put("hostFolder",  APILocator.systemHost().getIdentifier());
        map.put("modUser",  APILocator.systemUser().getUserId());
        map.put("working",true);
        map.put("name", "modes.persona.no.persona");
        map.put("personalized", Boolean.FALSE);
        map.put("hostName", "SYSTEM_HOST");
        map.put("host", "SYSTEM_HOST");
        map.put("contentType", "persona");
        map.put("archived", false);
        map.put("baseType", "PERSONA");
        map.put("keyTag", Persona.DOT_PERSONA_PREFIX_SCHEME);
        map.put("hasTitleImage", false);
        this.defaultPersona = ImmutableMap.copyOf(map);
    }

    

        
    
    @Override
    public PaginatedArrayList<PersonalizationPersonaPageView> getItems(final User user, final String filter,  int limit,  int offset, final String orderBy,
                                                            final OrderDirection direction, final Map<String, Object> extraParams) {

        final boolean respectFrontendRoles = (Boolean)extraParams.get("respectFrontEndRoles");
        final String pageId  = extraParams.get(PAGE_ID).toString();
        String orderByString = UtilMethods.isSet(orderBy) ? orderBy : "title desc";
        final String hostId  = extraParams.get("hostId").toString();
        final StringBuilder query = new StringBuilder(PERSONAS_QUERY).append(hostId);
        final boolean noFilter = !UtilMethods.isSet(filter);
        if(noFilter) {
          offset = (offset <= 0) ? 0 : offset-1;
          limit  = (offset == 0) ? limit-1  : limit;
        }
        else {

            query.append(" +persona.name:").append(filter).append("*");
        }

        orderByString =  orderByString.trim().toLowerCase().endsWith(" asc") ||
                orderByString.trim().toLowerCase().endsWith(" desc")? orderByString:
                orderByString + " " + (UtilMethods.isSet(direction) ? direction.toString().toLowerCase(): OrderDirection.ASC.name());


        try {

            final List<Contentlet> contentlets  = this.contentletAPI.search
                    (query.toString(), limit, offset, orderByString, user,respectFrontendRoles);
            final Set<String> personaTagPerPage = this.multiTreeAPI.getPersonalizationsForPage (pageId);
            final List<PersonalizationPersonaPageView> personalizationPersonaPageViews = new ArrayList<>();
            
            if (offset == 0 && noFilter) {
              
              
              Language foundLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(HttpServletRequestThreadLocal.INSTANCE.getRequest());
              Map<String,Object> contentletMap= new HashMap<>(this.defaultPersona);
              contentletMap.put("name",APILocator.getLanguageAPI().getStringKey(foundLanguage, "modes.persona.no.persona"));
              contentletMap.put("title",APILocator.getLanguageAPI().getStringKey(foundLanguage, "modes.persona.no.persona"));
              contentletMap.put("languageId",foundLanguage.getId());

              personalizationPersonaPageViews.add(new PersonalizationPersonaPageView(pageId, contentletMap));
            }
            for (final Contentlet contentlet : contentlets) {

                final Map<String, Object> contentletMap = ContentletUtil.getContentPrintableMap(user, contentlet);
                contentletMap.put("personalized",
                        personaTagPerPage.contains(Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON +
                                contentlet.getStringProperty(PersonaAPI.KEY_TAG_FIELD)));

                personalizationPersonaPageViews.add(new PersonalizationPersonaPageView(pageId, contentletMap));
            }

            final PaginatedArrayList<PersonalizationPersonaPageView> result = new PaginatedArrayList<>();
            result.addAll(personalizationPersonaPageViews);
            result.setTotalResults(this.contentletAPI.indexCount(query.toString(), user, respectFrontendRoles));

            return result;
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

}
