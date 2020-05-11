package com.dotcms.storage;

import com.dotcms.tika.TikaUtils;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSortedMap;
import io.vavr.control.Try;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Default implementation
 * @author jsanca
 */
public class FileStorageAPIImpl implements FileStorageAPI {

    // width,height,contentType,author,keywords,fileSize,content,length,title


    @Override
    public Map<String, Object> generateRawBasicMetaData(final File binary) {

        return this.generateBasicMetaData(binary, s -> true); // raw = no filter
    }

    @Override
    public Map<String, Object> generateRawFullMetaData(final  File binary, final int maxLength) {

        return this.generateFullMetaData(binary, s -> true, maxLength); // raw = no filter
    }

    @Override
    public Map<String, Object> generateBasicMetaData(final File binary, final Predicate<String> metaDataKeyFilter) {

        final ImmutableSortedMap.Builder<String, Object> mapBuilder =
                new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

        if (this.validBinary(binary)) {

            if (metaDataKeyFilter.test(TITLE_META_KEY)) {
                mapBuilder.put(TITLE_META_KEY, binary.getName());
            }

            if (metaDataKeyFilter.test(PATH_META_KEY)) {
                mapBuilder.put(PATH_META_KEY, binary.getAbsolutePath());
            }

            if (metaDataKeyFilter.test(LENGTH_META_KEY)) {
                mapBuilder.put(LENGTH_META_KEY, binary.length());
            }

            if (metaDataKeyFilter.test(CONTENT_TYPE_META_KEY)) {
                mapBuilder.put(CONTENT_TYPE_META_KEY, MimeTypeUtils.getMimeType(binary));
            }

            mapBuilder.put(MOD_DATE_META_KEY, System.currentTimeMillis());
            mapBuilder.put(SHA226_META_KEY,   Try.of(()->FileUtil.sha256toUnixHash(binary)).getOrElse("unknown"));
        }

        return mapBuilder.build();
    }

    @Override
    public Map<String, Object> generateFullMetaData(final File binary, final Predicate<String> metaDataKeyFilter,
                                                    final int maxLength) {

        final TreeMap<String, Object> metadataMap = new TreeMap<>(Comparator.naturalOrder());

        try {

            final TikaUtils tikaUtils = new TikaUtils();

            metadataMap.putAll(this.generateBasicMetaData(binary, metaDataKeyFilter));
            final Map<String, Object> tikaMetaDataMap = tikaUtils.getForcedMetaDataMap(binary, maxLength);
            for (final Map.Entry<String, Object> entry : tikaMetaDataMap.entrySet()) {

                if(metaDataKeyFilter.test(entry.getKey())) {

                    metadataMap.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (DotDataException e) {

            return Collections.emptyMap();
        }

        return new ImmutableSortedMap.Builder<String, Object>(Comparator.naturalOrder()).putAll(metadataMap).build();
    }

    @Override
    public Map<String, Object> generateMetaData(final File binary,
                                                final GenerateMetaDataConfiguration generateMetaDataConfiguration) {

        Map<String, Object> metadataMap    = Collections.emptyMap();
        final Optional<File> metadataFile  = generateMetaDataConfiguration.getMetaDataFileSupplier().get();

        this.checkOverride(metadataFile, generateMetaDataConfiguration);
        if (!this.exists(metadataFile)) {

            if (this.validBinary(binary)) {

                final int maxLength = generateMetaDataConfiguration.getMaxLength();
                metadataMap         = generateMetaDataConfiguration.isFull()?
                                        this.generateFullMetaData (binary, generateMetaDataConfiguration.getMetaDataKeyFilter(), maxLength):
                                        this.generateBasicMetaData(binary, generateMetaDataConfiguration.getMetaDataKeyFilter()):


                if (generateMetaDataConfiguration.isStore() && this.exists(metadataFile)) {

                    // todo: write on file system
                }
            }
        } else {

            // todo: see if can read from file system
        }


        if (generateMetaDataConfiguration.isCache()) {

            // todo: put in case
        }

        return metadataMap;
    }

    private boolean validBinary (final File binary) {

        return null != binary && binary.exists() && binary.canRead();
    }

    private boolean exists (final Optional<File> metadataFile) {

        return metadataFile.isPresent() && metadataFile.get().exists();
    }

    private void checkOverride (final Optional<File> metadataFile,
                                final GenerateMetaDataConfiguration generateMetaDataConfiguration) {

        if (generateMetaDataConfiguration.isOverride() && this.exists(metadataFile)) {

            try {
                metadataFile.get().delete();
            } catch (Exception e) {
                Logger.error(this.getClass(),
                        String.format("Unable to delete existing metadata file [%s] [%s]",
                                metadataFile.get().getAbsolutePath(), e.getMessage()), e);
            }
        }
    }
}
