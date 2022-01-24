package com.dotcms.content.model.hydration;

import static com.dotcms.content.model.hydration.HydrationUtils.findLinkedBinary;
import static com.dotcms.util.ReflectionUtils.setValue;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.storage.FileMetadataAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Little reusable component meant to populate a field in the FieldValue Object through the Builder
 */
public class MetadataDelegate implements HydrationDelegate {

    final Set<String> filter = ImmutableSet.of("sha256","name","contentType","isImage");

    @Override
    public FieldValueBuilder hydrate(final FieldValueBuilder builder, final Field field,
            final Contentlet contentlet, String propertyName)
            throws DotDataException, DotSecurityException {

        Map<String, Serializable> metadataMap = null;
        try{
            metadataMap = getMetadataMap(field, contentlet);
        } finally {
            //We know it is safe to set null as a fallback cuz we marked the attribute nullable
            setValue(builder, propertyName, metadataMap);
        }
        return builder;
    }

    /**
     * This must be solid we dont want a NPE here since it could break the entire start-up process
     * @param field
     * @param contentlet
     * @return
     */
    private Map<String, Serializable> getMetadataMap(final Field field, final Contentlet contentlet) {
        final FileMetadataAPI fileMetadataAPI = APILocator.getFileMetadataAPI();
        Map<String, Serializable> metadataMap = null;
        try {
            if (field instanceof BinaryField) {
                final File file = (File) contentlet.get(field.variable());
                metadataMap = fileMetadataAPI.getFullMetadataNoCache(file, null).getFieldsMeta();
            } else {
                if (field instanceof ImageField) {
                    final Optional<Contentlet> fileAsContentOptional = findLinkedBinary(
                            contentlet, (ImageField) field);
                    if (fileAsContentOptional.isPresent()) {
                        final Contentlet fileAsset = fileAsContentOptional.get();
                        metadataMap = fileMetadataAPI
                                .getOrGenerateMetadata(fileAsset, "fileAsset").getFieldsMeta();
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(MetadataDelegate.class, "error calculating metadata ", e);
        }
        return (metadataMap == null || metadataMap.isEmpty() ? null : filterMetadataFields(metadataMap));
    }

    /**
     * Remove unwanted extra attributes
     * @param originalMap
     * @return
     */
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
