package com.dotcms.publishing.manifest;

import com.dotcms.integritycheckers.IntegrityType;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotmarketing.util.UtilMethods;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Util class to read a Manifest file
 */
public interface ManifestReader {

    Collection<ManifestInfo> getIncludedAssets();
    Collection<ManifestInfo> getExcludedAssets();
    Collection<ManifestInfo> getAssets(final ManifestReason manifestReason);
    Collection<ManifestInfo> getAssets();
    String getMetadata(final String name);

    default List<PublishQueueElement> getPublishQueueElement(){
        final String OperationAsString = getMetadata(CSVManifestBuilder.OPERATION_METADATA_NAME);
        final Operation operation = UtilMethods.isSet(OperationAsString) ?
                Operation.valueOf(OperationAsString) : null;

        return getAssets().stream()
                .map(manifestInfo -> {
                    final PublishQueueElement publishQueueElement = new PublishQueueElement();
                    publishQueueElement.setAsset(UtilMethods.isSet(manifestInfo.inode()) ? manifestInfo.inode() : manifestInfo.id());

                    if (UtilMethods.isSet(operation)) {
                        publishQueueElement.setOperation(operation.ordinal());
                    }

                    publishQueueElement.setBundleId(
                            getMetadata(CSVManifestBuilder.BUNDLE_ID_METADATA_NAME));
                    publishQueueElement.setType(manifestInfo.objectType());
                    return publishQueueElement;
                })
                .collect(Collectors.toList());
    }
}
