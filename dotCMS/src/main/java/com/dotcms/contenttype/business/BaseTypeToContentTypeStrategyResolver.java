package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Subscribe Strategies to resolve from base types a content type
 * and get the strategy for a set of arguments if applies
 * @author jsanca
 */
public class BaseTypeToContentTypeStrategyResolver {

    private volatile Map<BaseContentType, BaseTypeToContentTypeStrategy> strategiesMap = this.getDefaultStrategies();

    private  Map<BaseContentType, BaseTypeToContentTypeStrategy> getDefaultStrategies() {

        final ImmutableMap.Builder<BaseContentType, BaseTypeToContentTypeStrategy> builder =
                new ImmutableMap.Builder<>();

        builder.put(BaseContentType.DOTASSET, new DotAssetBaseTypeToContentTypeStrategyImpl());

        return builder.build();
    }


    public synchronized void subscribe (final BaseContentType baseContentType, final BaseTypeToContentTypeStrategy strategy) {

        if (null != baseContentType && null != strategy) {

            final ImmutableMap.Builder<BaseContentType, BaseTypeToContentTypeStrategy> builder =
                    new ImmutableMap.Builder<>();

            builder.putAll(this.strategiesMap);
            builder.put(BaseContentType.DOTASSET, strategy);

            this.strategiesMap = builder.build();
        }
    }


    private static class SingletonHolder {
        private static final BaseTypeToContentTypeStrategyResolver INSTANCE = new BaseTypeToContentTypeStrategyResolver();
    }

    /**
     * Get the instance.
     * @return BaseTypeToContentTypeStrategyResolver
     */
    public static BaseTypeToContentTypeStrategyResolver getInstance() {

        return BaseTypeToContentTypeStrategyResolver.SingletonHolder.INSTANCE;
    } // getInstance.


    /**
     * Get a strategy if applies
     * @param baseContentType {@link BaseContentType}
     * @return Optional BaseTypeToContentTypeStrategy
     */
    public Optional<BaseTypeToContentTypeStrategy> get(final BaseContentType baseContentType) {

        return this.strategiesMap.containsKey(baseContentType)?
                Optional.of(this.strategiesMap.get(baseContentType)):
                Optional.empty();
    }
    /////////////
    private class DotAssetBaseTypeToContentTypeStrategyImpl  implements BaseTypeToContentTypeStrategy {

        @Override
        public Optional<ContentType> apply(final BaseContentType baseContentType, final Map<String, Object> contextMap) {

            final User user              = (User)contextMap.get("user");
            final Host currentHost       = (Host)contextMap.get("host");
            final List<File> binaryFiles = (List<File>) contextMap.getOrDefault("binaryFiles", Collections.emptyList());
            final Map<String, Object> contentletMap = (Map<String, Object>) contextMap.get("contentletMap");
            final List<String>        accessingList = (List<String>)contextMap.get("accessingList");

            final File file = this.getBinary(binaryFiles, contentletMap, accessingList);
            if (null != file && file.exists() && file.canRead()) {

                try {

                    return APILocator.getDotAssetAPI().tryMatch(file, currentHost, user);
                } catch (DotDataException | DotSecurityException e) {
                    return Optional.empty();
                }
            }

            return Optional.empty();
        }

        private File getBinary (final List<File> binaryFiles, final Map<String, Object> contentletMap,
                                final List<String> accessingList) {

            File file = null;
            if (contentletMap.containsKey(DotAssetContentType.ASSET_FIELD_VAR)) {

                final Object assetValue = contentletMap.get(DotAssetContentType.ASSET_FIELD_VAR);
                if (assetValue instanceof  File) {

                    file = (File)assetValue;
                } else {

                    final Optional<DotTempFile>  tempFile = APILocator.getTempFileAPI()
                            .getTempFile(accessingList, assetValue.toString()); // lets try by temporal file id.
                    if (tempFile.isPresent()) {

                        file = tempFile.get().file;
                    }
                }
            } else if (!binaryFiles.isEmpty()) {

                file = binaryFiles.get(0); // we try with the first one, on dotAsset is only one.
            }

            return file;
        }
    } // DotAssetBaseTypeToContentTypeStrategyImpl.


}
