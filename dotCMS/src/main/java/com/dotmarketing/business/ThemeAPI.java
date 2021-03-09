package com.dotmarketing.business;

import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.util.List;

/**
 * It is intended as a Helper class to handle Theme
 * A theme is a set of assets under /application/themes
 */
public interface ThemeAPI {
    String THEME_PNG = "theme.png";
    String THEME_THUMBNAIL_KEY = "themeThumbnail";

    /**
     * Gets the thumbnail from the folder, the {@link Theme} already has the thumbnail calculated
     * @param folder
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    String getThemeThumbnail(final Folder folder, final User user) throws DotSecurityException, DotDataException;

    /**
     * Returns the system theme
     * @return Theme
     */
    Theme systemTheme();

    /**
     * Find the theme by id (folder id)
     * @param themeId {@link String} theme folder id
     * @param
     * @return
     */
    Theme findThemeById(String themeId, User user,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException;

    /**
     * Convert from Folder to Theme
     * @param folder {@link Folder} folder
     * @param user {@link User}
     * @param respectFrontendRoles {@link Boolean}
     * @return
     */
    Theme fromFolder(Folder folder, User user,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException;

    /**
     * Return a collection of themes.
     * @param themeId {@link String} template folder id
     * @param user {@link User}
     * @param limit {@link Integer} pagination limit
     * @param offset {@link Integer} pagination offset
     * @param hostId {@link String} host id (required)
     * @param direction {@link OrderDirection} asc or desc order
     * @param searchParams {@link String} general search filter param
     * @param respectFrontendRoles {@link Boolean}
     * @return List of Themes
     */
    List<Theme> findThemes(String themeId, User user, int limit, int offset,
            String hostId, OrderDirection direction, String searchParams,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
}