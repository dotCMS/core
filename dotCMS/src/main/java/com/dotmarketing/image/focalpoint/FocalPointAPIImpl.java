package com.dotmarketing.image.focalpoint;

import static com.dotmarketing.util.StringUtils.isSet;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

public class FocalPointAPIImpl implements FocalPointAPI {

    //Focal point can be associated with temp files and/or inodes
    //If we work directly with an inode we would be associating the focal point metadata with the contentlet when clicking on the image
    //While there is workflow that needs to be followed for that.
    //The Metadata should be stored only if we Hit save and then Publish
    //Some of that logic takes place in BinaryExporterServlet and then in ESContentletAPIImpl
    public static final String TMP = "temp_";

    private final Pattern         fpPattern = Pattern.compile(StringPool.COMMA);
    private final FileMetadataAPI fileMetadataAPI;
    private final TempFileAPI tempFileAPI;
    private final ContentletAPI contentletAPI;
    private final Supplier<User> currentUserSupplier;

    public FocalPointAPIImpl() {
        this(APILocator.getFileMetadataAPI(), APILocator.getTempFileAPI() ,APILocator.getContentletAPI(),
                () -> {
                    final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
                    if (null != request) {
                        return PortalUtil.getUser(request);
                    }
                    return null;
                });
    }

    @VisibleForTesting
    FocalPointAPIImpl(final FileMetadataAPI fileMetadataAPI, final TempFileAPI tempFileAPI ,final ContentletAPI contentletAPI, final Supplier<User> currentUserSupplier) {
        this.fileMetadataAPI = fileMetadataAPI;
        this.tempFileAPI = tempFileAPI;
        this.contentletAPI = contentletAPI;
        this.currentUserSupplier = currentUserSupplier;
    }

    @Override
    public void writeFocalPoint(final String inode, final String fieldVar, final FocalPoint focalPoint) {

        if(focalPoint.isEmpty()){
          return;
        }

        if(isTempResourceId(inode)) {
            Logger.debug(FocalPointAPIImpl.class, "Metadata writing fp under temp id "+inode);
            writeFocalPointMeta(inode, fieldVar, focalPoint);
        } else {
            final Optional<Contentlet> contentlet = findContentletByInode(inode);
            if (contentlet.isPresent()) {
                writeFocalPointMeta(contentlet.get(), fieldVar, focalPoint);
                Logger.debug(FocalPointAPIImpl.class, "Metadata writing fp under inode "+inode);
            } else {
                Logger.warn(FocalPointAPIImpl.class,
                        "Unable to persist focal point info.  Couldn't find a contentlet for the given inode "
                                + inode);
            }
        }
    }

    /**
     * Write the focal point to a temp resource ID
     * @param tempResourceId
     * @param fieldVar
     * @param focalPoint
     */
    private void writeFocalPointMeta(final String tempResourceId, final String fieldVar, final FocalPoint focalPoint) {
        try {
            fileMetadataAPI.putCustomMetadataAttributes(tempResourceId, ImmutableMap.of(fieldVar, ImmutableMap.of(FOCAL_POINT, focalPoint.toString())));
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Write the focal point to a contentlet
     * @param contentlet
     * @param fieldVar
     * @param focalPoint
     */
    private void writeFocalPointMeta(final Contentlet contentlet, final String fieldVar, final FocalPoint focalPoint) {
        try {
            fileMetadataAPI.putCustomMetadataAttributes(contentlet, ImmutableMap.of(fieldVar, ImmutableMap.of(FOCAL_POINT, focalPoint.toString())));
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     *
     * @param contentlet
     * @param fieldVar
     * @return
     */
    private Optional<FocalPoint> readFocalPointMeta(final Contentlet contentlet, final String fieldVar) {

       try {
           final Metadata metadata = fileMetadataAPI.getMetadata(contentlet, fieldVar);
           return parseFocalPoint(
                   (String) metadata.getCustomMeta().get(FOCAL_POINT));
       }catch (Exception e){
          Logger.debug (FocalPointAPIImpl.class, "Metadata Error retrieving focal point from custom metadata", e);
       }
        return Optional.empty();
    }

    @Override
    public Optional<FocalPoint> parseFocalPoint(final String focalPoint) {

        try {
            final String[] value = fpPattern.split(focalPoint);
            return Optional.of(new FocalPoint(Float.valueOf(value[0]), Float.valueOf(value[1])));
        } catch (Exception e) {
            Logger.debug(FocalPointAPIImpl.class, "Error parsing FP Metadata ", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<FocalPoint> readFocalPoint(final String inode, final String fieldVar) {
        Optional<FocalPoint> focalPoint = Optional.empty();
        if (isTempResourceId(inode)) {
            focalPoint = readFocalPointMeta(inode);
            Logger.debug(FocalPointAPIImpl.class, "Metadata readFocalPoint from TempId "+ inode );
        } else {
            final Optional<Contentlet> optional = findContentletByInode(inode);
            if (optional.isPresent()) {
                focalPoint = readFocalPointMeta(optional.get(), fieldVar);
                Logger.debug(FocalPointAPIImpl.class, "Metadata readFocalPoint from inode "+ inode + " fieldVar " + fieldVar);
            }
        }
        return focalPoint;

    }

    /**
     * Focal point md associated with a temp resource
     * @param tempResourceId
     * @return
     */
    private Optional<FocalPoint> readFocalPointMeta(final String tempResourceId){
        try {
           final Optional<Metadata> optional = fileMetadataAPI.getMetadata(tempResourceId);
           if(optional.isPresent()){
               final Metadata metadata = optional.get();
               return parseFocalPoint(
                       (String) metadata.getCustomMeta().get(FOCAL_POINT));
           }
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<FocalPoint> parseFocalPointFromParams(final Map<String, String[]> parameters) {

        return Try.of(() -> parseFocalPoint(parameters.get("fp")[0])).getOrElse(Optional.empty());
    }

    /**
     * contentlet inode finder method
     * @param inode
     * @return
     */
    private Optional<Contentlet> findContentletByInode(final String inode){
      return Optional.ofNullable(Try.of(()->contentletAPI.find(inode, currentUserSupplier.get(), false)).getOrNull());
    }

    /**
     * Temp AssetID validator
     * @param assetId
     * @return
     */
    private boolean isTempResourceId(final String assetId){

        return isSet(assetId) && (tempFileAPI.isTempResource(assetId) || (assetId.startsWith(TMP) && UUIDUtil.isUUID(assetId.substring(TMP.length()))));
    }

}
