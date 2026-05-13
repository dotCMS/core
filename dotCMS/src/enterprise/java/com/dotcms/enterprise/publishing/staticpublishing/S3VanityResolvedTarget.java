package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

import java.io.File;
import java.util.Optional;

/**
 * Holds the dotCMS resource resolved from a Vanity URL forward target.
 */
public final class S3VanityResolvedTarget {

    final DotAsset type;
    final String canonicalPath;
    final IHTMLPage htmlPage;
    final URLMapInfo urlMapInfo;
    final FileAsset fileAsset;

    /**
     * Creates a resolved static publishing target.
     *
     * @param type dotCMS target type
     * @param canonicalPath normalized forward target path
     * @param htmlPage page to render
     * @param urlMapInfo URL Map information when the target is URL mapped
     */
    public S3VanityResolvedTarget(final DotAsset type, final String canonicalPath,
                                  final IHTMLPage htmlPage, final URLMapInfo urlMapInfo) {
        this(type, canonicalPath, htmlPage, urlMapInfo, null);
    }

    /**
     * Creates a resolved static publishing target.
     *
     * @param type dotCMS target type
     * @param canonicalPath normalized forward target path
     * @param htmlPage page to render
     * @param urlMapInfo URL Map information when the target is URL mapped
     * @param fileAsset file asset when the target is a binary asset
     */
    public S3VanityResolvedTarget(final DotAsset type, final String canonicalPath,
                                  final IHTMLPage htmlPage, final URLMapInfo urlMapInfo,
                                  final FileAsset fileAsset) {
        this.type = type;
        this.canonicalPath = canonicalPath;
        this.htmlPage = htmlPage;
        this.urlMapInfo = urlMapInfo;
        this.fileAsset = fileAsset;
    }

    /**
     * Returns the contentlet inode required to render URL mapped pages.
     *
     * @return contentlet inode when available
     */
    public Optional<String> contentletInode() {
        return urlMapInfo == null ? Optional.empty() : Optional.of(urlMapInfo.getContentlet().getInode());
    }

    /**
     * Returns the physical file backing a resolved File Asset.
     *
     * @return physical file when this target is a File Asset
     */
    public Optional<File> physicalFile() {
        return fileAsset == null ? Optional.empty() : Optional.of(fileAsset.getFileAsset());
    }
}
