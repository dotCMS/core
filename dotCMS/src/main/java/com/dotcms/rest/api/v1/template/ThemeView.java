package com.dotcms.rest.api.v1.template;

import com.dotmarketing.business.Theme;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.Date;

/**
 * This class holds a view with all attributes of the {@link Theme} class which is used to by the
 * REST Endpoint to return it in JSON format. Strictly speaking, keep in mind that a Theme is also
 * a Folder, so they share several properties.
 *
 * @author Jose Castro
 * @since Sep 15th, 2023
 */
public class ThemeView implements Serializable {

    private final String path;
    private final String defaultFileType;
    private final String filesMasks;
    private final Date iDate;
    private final String hostId;
    private final String identifier;
    private final String inode;
    private final Date modDate;
    private final String name;
    private final Boolean showOnMenu;
    private final Integer sortOrder;
    private final String title;
    private final String type;

    @JsonCreator
    public ThemeView(final Theme theme) {
        this.name = theme.getName();
        this.defaultFileType = theme.getDefaultFileType();
        this.iDate = theme.getIDate();
        this.hostId = theme.getHostId();
        this.identifier = theme.getIdentifier();
        this.inode = theme.getInode();
        this.modDate = theme.getModDate();
        this.showOnMenu = theme.isShowOnMenu();
        this.sortOrder = theme.getSortOrder();
        this.title = theme.getTitle();
        this.type = theme.getType();
        this.filesMasks = theme.getFilesMasks();
        this.path = theme.getPath();
    }

    /**
     * Returns the folder path to the theme folder inside dotCMS.
     *
     * @return The theme's folder path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the default type of File Assets that ca be added to this theme/folder.
     *
     * @return The default File Asset type.
     */
    public String getDefaultFileType() {
        return defaultFileType;
    }

    /**
     * Returns a comma-separated list of file extensions that are allowed to be added to this
     * theme/folder.
     *
     * @return The comma-separated list of file extensions.
     */
    public String getFilesMasks() {
        return filesMasks;
    }

    public Date getiDate() {
        return iDate;
    }

    /**
     * Returns the ID of the Site where this theme is located.
     *
     * @return The Site ID.
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * Returns the Identifier of the theme.
     *
     * @return The theme's Identifier.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the Inode of the theme.
     *
     * @return The theme's Inode.
     */
    public String getInode() {
        return inode;
    }

    /**
     * Returns the last modification date of the theme.
     *
     * @return The theme's last modification date.
     */
    public Date getModDate() {
        return modDate;
    }

    /**
     * Returns the name of the theme/folder.
     *
     * @return The theme's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the theme/folder can be shown on the menu or not via the Navigation Tool.
     *
     * @return True if the theme/folder can be shown on the menu, false otherwise.
     */
    public Boolean getShowOnMenu() {
        return showOnMenu;
    }

    /**
     * Returns the sort order of the theme/folder.
     *
     * @return The theme's sort order.
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * Returns the title of the theme/folder.
     *
     * @return The theme's title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the type of the theme/folder. In this case, the value will be {@code "folder"}.
     *
     * @return The theme's type.
     */
    public String getType() {
        return type;
    }

}
