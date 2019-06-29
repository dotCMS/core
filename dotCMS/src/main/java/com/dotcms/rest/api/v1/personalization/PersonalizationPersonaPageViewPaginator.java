package com.dotcms.rest.api.v1.personalization;

import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginatorOrdered;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class PersonalizationPersonaPageViewPaginator implements PaginatorOrdered<PersonalizationPersonaPageView> {

    public  static final String PAGE_ID        = "pageId";
    private static final String PERSONAS_QUERY = "+contentType:persona +live:true +deleted:false +working:true";

    private final PersonaAPI    personaAPI;
    private final MultiTreeAPI  multiTreeAPI;
    private final ContentletAPI contentletAPI;

    public PersonalizationPersonaPageViewPaginator() {
        this(APILocator.getPersonaAPI(), APILocator.getMultiTreeAPI(),
                APILocator.getContentletAPI());
    }

    public PersonalizationPersonaPageViewPaginator(final PersonaAPI personaAPI,
                                                   final MultiTreeAPI multiTreeAPI,
                                                   final ContentletAPI contentletAPI) {

        this.personaAPI = personaAPI;
        this.multiTreeAPI = multiTreeAPI;
        this.contentletAPI = contentletAPI;
    }

    @Override
    public PaginatedArrayList<PersonalizationPersonaPageView> getItems(final User user, final String filter, final int limit, final int offset, final String orderBy,
                                                            final OrderDirection direction, final Map<String, Object> extraParams) {

        final boolean respectFrontendRoles = (Boolean)extraParams.get("respectFrontEndRoles");
        final String pageId  = extraParams.get(PAGE_ID).toString();
        String orderByString = UtilMethods.isSet(orderBy) ? orderBy : "title desc";

        orderByString =  orderByString.trim().toLowerCase().endsWith(" asc") ||
                orderByString.trim().toLowerCase().endsWith(" desc")? orderByString:
                orderByString + " " + (UtilMethods.isSet(direction) ? direction.toString().toLowerCase(): OrderDirection.ASC.name());

        try {

            final List<Contentlet> contentlets  = this.contentletAPI.search
                    (PERSONAS_QUERY, limit, offset, orderByString, user,respectFrontendRoles);
            final Set<String> personaTagPerPage = this.multiTreeAPI.getPersonalizationsForPage (pageId);
            final List<PersonalizationPersonaPageView> personalizationPersonaPageViews = new ArrayList<>();

            for (final Contentlet contentlet : contentlets) {

                final Persona persona = this.personaAPI.fromContentlet(contentlet);
                final ContentletToMapTransformer transformer = new ContentletToMapTransformer(persona);
                final Map<String, Object> contentletMap = transformer.toMaps().stream().findFirst().orElse(Collections.EMPTY_MAP);
                contentletMap.put("personalized",
                        personaTagPerPage.contains(Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + persona.getKeyTag()));

                personalizationPersonaPageViews.add(new PersonalizationPersonaPageView(pageId, contentletMap));
            }

            final PaginatedArrayList<PersonalizationPersonaPageView> result = new PaginatedArrayList<>();
            result.addAll(personalizationPersonaPageViews);
            result.setTotalResults(this.contentletAPI.indexCount(PERSONAS_QUERY, user, respectFrontendRoles));

            return result;
        } catch (DotDataException | IllegalAccessException | InvocationTargetException| DotSecurityException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

}
