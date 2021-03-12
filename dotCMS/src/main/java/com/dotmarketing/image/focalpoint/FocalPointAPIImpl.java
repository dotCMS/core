package com.dotmarketing.image.focalpoint;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.werken.xpath.impl.Op;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.liferay.util.StringPool;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

public class FocalPointAPIImpl implements FocalPointAPI {

    private final Pattern         fpPattern = Pattern.compile(StringPool.COMMA);
    private final FileMetadataAPI fileMetadataAPI;
    private final ContentletAPI contentletAPI;
    private final Supplier<User> currentUserSupplier;

    public FocalPointAPIImpl() {
        this(APILocator.getFileMetadataAPI(), APILocator.getContentletAPI(),
                () -> {
                    final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
                    if (null != request) {
                        return PortalUtil.getUser(request);
                    }
                    return null;
                });
    }

    @VisibleForTesting
    FocalPointAPIImpl(final FileMetadataAPI fileMetadataAPI, final ContentletAPI contentletAPI, final Supplier<User> currentUserSupplier) {
        this.fileMetadataAPI = fileMetadataAPI;
        this.contentletAPI = contentletAPI;
        this.currentUserSupplier = currentUserSupplier;
    }

    @Override
    public void writeFocalPoint(final String inode, final String fieldVar, final FocalPoint focalPoint) {

        final Optional<Contentlet> contentlet = findContentlet(inode);
        if(contentlet.isPresent()){
                writeFocalPoint(contentlet.get(), fieldVar, focalPoint);
        } else {
           Logger.warn(FocalPointAPIImpl.class, "Unable to persist focal point info.  Couldn't find a contentlet for the given inode " + inode);
        }

    }


    private void writeFocalPoint(final Contentlet contentlet, final String fieldVar, final FocalPoint focalPoint) {
        try {
            fileMetadataAPI.putCustomMetadataAttributes(contentlet, ImmutableMap.of(fieldVar, ImmutableMap.of(FOCAL_POINT, focalPoint.toString())));
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }

    }

    private Optional<FocalPoint> readFocalPoint(final Contentlet contentlet, final String fieldVar) {

       try {
           final Metadata metadata = fileMetadataAPI.getMetadata(contentlet, fieldVar);
           return parseFocalPoint(
                   (String) metadata.getCustomMeta().get(FOCAL_POINT));
       }catch (Exception e){
          Logger.error (FocalPointAPIImpl.class,"Error retrieving focal point from custom metadata", e);
       }
        return Optional.empty();
    }

    @Override
    public Optional<FocalPoint> parseFocalPoint(final String forcalPoint) {

        try {
            final String[] value = this.fpPattern.split(forcalPoint);
            return Optional.of(new FocalPoint(Float.valueOf(value[0]), Float.valueOf(value[1])));
        } catch (Exception e) {
            Logger.debug(this.getClass(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<FocalPoint> readFocalPoint(final String inode, final String fieldVar) {


        final Optional<Contentlet> optional = findContentlet(inode);
        if(optional.isPresent()){
            return readFocalPoint(optional.get(), fieldVar);
        }

        return Optional.empty();

    }


    @Override
    public Optional<FocalPoint> parseFocalPointFromParams(final Map<String, String[]> parameters) {

        return Try.of(() -> parseFocalPoint(parameters.get("fp")[0])).getOrElse(Optional.empty());
    }


    private Optional<Contentlet> findContentlet(final String inode){
      return Optional.ofNullable(Try.of(()->contentletAPI.find(inode, currentUserSupplier.get(), false)).getOrNull());
    }

}
