package com.dotcms.rest.api.v1.personalization;

import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginatorOrdered;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.util.*;

public class PersonalizationPersonaPageViewPaginator implements PaginatorOrdered<PersonalizationPersonaPageView> {

  public static final String PAGE_ID = "pageId";

  private final MultiTreeAPI multiTreeAPI;
  private final PersonaAPI personaAPI;

  public PersonalizationPersonaPageViewPaginator() {
    this(APILocator.getPersonaAPI(), APILocator.getMultiTreeAPI());

  }

  public PersonalizationPersonaPageViewPaginator(final PersonaAPI personaAPI, final MultiTreeAPI multiTreeAPI) {

    this.multiTreeAPI = multiTreeAPI;
    this.personaAPI = personaAPI;

  }

  @Override
  public PaginatedArrayList<PersonalizationPersonaPageView> getItems(final User user, final String filter, int limit, int offset,
      final String orderBy, final OrderDirection direction, final Map<String, Object> extraParams) {

    final boolean respectFrontEndRoles = (Boolean) extraParams.get("respectFrontEndRoles");
    final String pageId = extraParams.get(PAGE_ID).toString();
    String orderByString = UtilMethods.isSet(orderBy) ? orderBy : "title desc";
    orderByString =
        orderByString.trim().toLowerCase().endsWith(" asc") || orderByString.trim().toLowerCase().endsWith(" desc") ? orderByString
            : orderByString + " " + (UtilMethods.isSet(direction) ? direction.toString().toLowerCase() : OrderDirection.ASC.name());

    final Host host = Try.of(() -> APILocator.getHostAPI().find(extraParams.get("hostId").toString(), user, respectFrontEndRoles))
        .getOrElse(APILocator.systemHost());

    try {
      Tuple2<List<Persona>, Integer> personas = personaAPI.getPersonasIncludingDefaultPersona(host, filter, respectFrontEndRoles, limit,
          offset, orderByString, user, respectFrontEndRoles);

      final Set<String> personaTagPerPage = this.multiTreeAPI.getPersonalizationsForPage(pageId);
      final List<PersonalizationPersonaPageView> personalizationPersonaPageViews = new ArrayList<>();

      final Language foundLanguage = Try.of(
                () -> WebAPILocator.getLanguageWebAPI().getBackendLanguage()
              ).getOrElse(APILocator.getLanguageAPI().getDefaultLanguage());

      for (final Persona persona : personas._1) {

        final Map<String, Object> personaMap = ContentletUtil.getContentPrintableMap(user, persona);
        personaMap.put("personalized",
            personaTagPerPage.contains(Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + persona.getKeyTag()));
        if (PersonaAPI.DEFAULT_PERSONA_NAME_KEY.equals(persona.getName())) {
          personaMap.put("name", APILocator.getLanguageAPI().getStringKey(foundLanguage, "modes.persona.no.persona"));
          personaMap.put("title", APILocator.getLanguageAPI().getStringKey(foundLanguage, "modes.persona.no.persona"));
          personaMap.put("languageId", foundLanguage.getId());
        }

        personalizationPersonaPageViews.add(new PersonalizationPersonaPageView(pageId, personaMap));
      }

      final PaginatedArrayList<PersonalizationPersonaPageView> result = new PaginatedArrayList<>();
      result.addAll(personalizationPersonaPageViews);
      result.setTotalResults(personas._2);

      return result;
    } catch (Exception e) {

      Logger.error(this, e.getMessage(), e);
      throw new DotRuntimeException(e);
    }
  }

}
