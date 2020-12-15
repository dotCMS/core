package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.control.Try;

import java.util.Map;

import static com.dotmarketing.business.ThemeAPI.THEME_THUMBNAIL_KEY;

/**
 * Abstraction of the Theme as a folder + thumbnail
 * @author jsanca
 */
public class Theme extends Folder  {

    public static final String SYSTEM_THEME = "SYSTEM_THEME";

    private final String themeThumbnail;
    private final String path;

    public Theme(final String themeThumbnail) {
        this(themeThumbnail, null);
    }

    public Theme(final String themeThumbnail, final String path) {
        super();
        this.themeThumbnail = themeThumbnail;
        this.path           = path;
    }

    public Theme(final Folder baseFolder, final String themeThumbnail) {
        this(themeThumbnail, null);
        this.setIdentifier(baseFolder.getIdentifier());
        this.setInode(baseFolder.getInode());
        this.setDefaultFileType(baseFolder.getDefaultFileType());
        this.setFilesMasks(baseFolder.getFilesMasks());
        this.setHostId(baseFolder.getHostId());
        this.setModDate(baseFolder.getModDate());
        this.setName(baseFolder.getName());
        this.setShowOnMenu(baseFolder.isShowOnMenu());
        this.setSortOrder(baseFolder.getSortOrder());
        this.setTitle(baseFolder.getTitle());
        this.setIDate(baseFolder.getIDate());
        this.setType(baseFolder.getType());
        this.setOwner(baseFolder.getOwner());
        this.setVersionId(baseFolder.getVersionId());
    }

    @Override
    public String getPath() {
        return null != this.path? path: super.getPath();
    }

    @Override
    public Map<String, Object> getMap() {
        final Map<String, Object> retMap = Try.of(()->super.getMap()).get();
        retMap.put(THEME_THUMBNAIL_KEY, this.themeThumbnail);
        return retMap;
    }

    @JsonIgnore
    public boolean isSystemTheme () {

        return SYSTEM_THEME.equals(this.getIdentifier());
    }

    public String getThemeThumbnail() {
        return themeThumbnail;
    }
}
