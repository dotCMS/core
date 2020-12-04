package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

/**
 * It is intended as a Helper class to handle Theme
 */
public interface ThemeAPI {
    String THEME_PNG = "theme.png";
    String THEME_THUMBNAIL_KEY = "themeThumbnail";

    String getThemeThumbnail(final Folder folder, final User user) throws DotSecurityException, DotDataException;

    /**
     * Returns the system theme
     * @return Theme
     */
    Theme systemTheme();
}
