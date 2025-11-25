package com.dotcms.analytics.track.matchers;

import com.dotcms.visitor.filter.characteristics.Character;
import com.dotcms.visitor.filter.characteristics.CharacterWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * Matcher for pages or files
 * @author jsanca
 */
public class PagesAndUrlMapsRequestMatcher implements RequestMatcher {

    public static final String PAGES_AND_URL_MAPS_MATCHER_ID = "pagesAndUrlMapsMatcher";
    private final CharacterWebAPI characterWebAPI;

    public PagesAndUrlMapsRequestMatcher() {
        this(WebAPILocator.getCharacterWebAPI());
    }

    public PagesAndUrlMapsRequestMatcher(final CharacterWebAPI characterWebAPI) {
        this.characterWebAPI = characterWebAPI;
    }

    @Override
    public boolean runBeforeRequest() {
        return true;
    }

    @Override
    public boolean match(final HttpServletRequest request, final HttpServletResponse response) {

        final Character character = this.characterWebAPI.getOrCreateCharacter(request, response);
        if (Objects.nonNull(character)) {

            final CMSFilter.IAm iAm = (CMSFilter.IAm) character.getMap().
                    getOrDefault("iAm", CMSFilter.IAm.NOTHING_IN_THE_CMS);

            // should we have a fallback when nothing is returned???
            return iAm == CMSFilter.IAm.PAGE; // this captures also url maps
        }

        return false;
    }

    @Override
    public String getId() {
        return PAGES_AND_URL_MAPS_MATCHER_ID;
    }
}
