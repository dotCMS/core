package com.dotcms.publishing.manifest;

import com.dotcms.integritycheckers.IntegrityType;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotmarketing.util.UtilMethods;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface ManifestReader {

    Collection<ManifestInfo> getIncludedAssets();
    Collection<ManifestInfo> getExcludedAssets();
    Collection<ManifestInfo> getAssets(final ManifestReason manifestReason);
    Collection<ManifestInfo> getAssets();
    String getMetadata(final String name);

    default List<PublishQueueElement> getPublishQueueElement(){
        final IntegrityType integrityType = IntegrityType.valueOf(getMetadata(CSVManifestBuilder.OPERATION_METADATA_NAME));

        return getAssets().stream()
                .map(manifestInfo -> {
                    final PublishQueueElement publishQueueElement = new PublishQueueElement();
                    publishQueueElement.setAsset(UtilMethods.isSet(manifestInfo.inode()) ? manifestInfo.inode() : manifestInfo.id());
                    publishQueueElement.setOperation(integrityType.ordinal());
                    publishQueueElement.setBundleId(
                            getMetadata(CSVManifestBuilder.BUNDLE_ID_METADATA_NAME));
                    publishQueueElement.setType(manifestInfo.objectType());
                    return publishQueueElement;
                })
                .collect(Collectors.toList());
    }
}
