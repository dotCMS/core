package com.dotmarketing.image.focalpoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.liferay.util.StringPool;
import org.apache.commons.io.IOUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

public class FocalPointAPIImpl implements FocalPointAPI {

    private final static String FOCALPOINT_EXTENSION = ".dotfp";

    private final FocalPointCache cache;
    private final FileAssetAPI    fileAssetAPI;
    private final Pattern         fpPattern = Pattern.compile(StringPool.COMMA);

    public FocalPointAPIImpl() {
        this(APILocator.getFileAssetAPI(), new FocalPointCache());

    }

    public FocalPointAPIImpl(final FileAssetAPI fileAssetAPI, final FocalPointCache cache) {
        this.fileAssetAPI = fileAssetAPI;
        this.cache = cache;
    }


    private File getFPFile(final String inode, final String fieldVar) {
        File assetOpt = fileAssetAPI.getContentMetadataFile(inode);
        return new File(assetOpt.getParent(), fieldVar + FOCALPOINT_EXTENSION);
    }

    @Override
    public void writeFocalPoint(final String inode, final String fieldVar, final FocalPoint focalPoint) {

        final File dotFP = getFPFile(inode, fieldVar);
        dotFP.getParentFile().mkdirs();

        if (focalPoint.x == 0 && focalPoint.y == 0) {
            Logger.info(this.getClass(), "Deleteing focalpoint:" + focalPoint);
            dotFP.delete();
            if (cache != null) {
                cache.remove(inode, fieldVar);
            }
            return;
        }

        try (OutputStream out = Files.newOutputStream(dotFP.toPath())) {
            Logger.info(this.getClass(), "Writing focalpoint:" + focalPoint + " to " + dotFP);
            IOUtils.write(focalPoint.x + "," + focalPoint.y, out, Charset.defaultCharset());
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }

        if (cache != null) {
            cache.add(inode, fieldVar, focalPoint);
        }

    }

    private Optional<FocalPoint> readFocalPoint(final File dotFP) {

        try (InputStream input = Files.newInputStream(dotFP.toPath())) {

            final String value = IOUtils.toString(input, Charset.defaultCharset());
            return parseFocalPoint(value);
        } catch (Exception e) {
            Logger.debug(this.getClass(), e.getMessage(), e);
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

        final Optional<FocalPoint> retVal = cache != null ? cache.get(inode, fieldVar) : Optional.empty();
        if (retVal.isPresent()) {
            return retVal;
        }

        final File file = getFPFile(inode, fieldVar);
        return readFocalPoint(file);
    }


    @Override
    public Optional<FocalPoint> parseFocalPointFromParams(final Map<String, String[]> parameters) {

        return Try.of(() -> parseFocalPoint(parameters.get("fp")[0])).getOrElse(Optional.empty());
    }
}
