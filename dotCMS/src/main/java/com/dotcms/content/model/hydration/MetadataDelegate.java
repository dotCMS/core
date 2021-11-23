package com.dotcms.content.model.hydration;

import static com.dotcms.util.ReflectionUtils.setValue;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Try;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataDelegate implements HydrationDelegate {

    final Set<String> filter = ImmutableSet.of("sha256","name","contentType","isImage");

    @Override
    public FieldValueBuilder hydrate(final FieldValueBuilder builder, final Field field,
            final Contentlet contentlet, String propertyName)
            throws DotDataException, DotSecurityException {
        Map<String, Serializable> metadataMap = null;
        if (field instanceof BinaryField) {
            metadataMap = Try.of(() -> contentlet.getBinaryMetadata(field).getMap()).getOrNull();
        } else {
            if (field instanceof ImageField) {
                Object fileAssetIdentifier = contentlet.get(field.variable());
                Optional<Contentlet> fileAsContentOptional = APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(fileAssetIdentifier.toString(),
                                Try.of(
                                        contentlet::isLive).getOrElseThrow(
                                        () -> new DotDataException(
                                                "can't determine if content is live."))
                                , contentlet.getLanguageId(),
                                APILocator.systemUser(), true);
                if (fileAsContentOptional.isPresent()) {
                    final Contentlet fileAsset = fileAsContentOptional.get();
                    //Currently this is the only way we have to generate MD for dotAssets
                    metadataMap = APILocator.getFileMetadataAPI()
                            .getFullMetadataNoCache((File) fileAsset.get("asset"), null)
                            .getFieldsMeta();
                }
            }
        }
        setValue(builder, propertyName, filterMetadataFields(metadataMap));
        return builder;
    }

    private Map<String, Serializable> filterMetadataFields(
            final Map<String, Serializable> originalMap) {
        if (null == originalMap) {
            return null;
        }
        return originalMap.entrySet().stream().filter(entry -> filter
                .contains(entry.getKey())).collect(
                Collectors.toMap(Entry::getKey, Entry::getValue));
    }

}
