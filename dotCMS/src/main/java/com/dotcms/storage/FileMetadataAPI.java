package com.dotcms.storage;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.storage.model.ContentletMetadata;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;

/**
 * Encapsulates the generation of the contentlet metadata, in addition the interaction of the file system and cache to stores the metadata and the actual generation of the meta
 * is being done by the {@link FileStorageAPI}
 * @author jsanca
 */
public interface FileMetadataAPI {

    /**
     * This generation use an strategy to make the choice of which binary fields will be included on the generation of the full or basic metadata.
     * By default it takes the first indexable binary for full and all the rest will be used on the the basic.
     * @param contentlet {@link Contentlet}
     * @return ContentletMetadata
     */
    ContentletMetadata generateContentletMetadata (Contentlet contentlet)
            throws IOException, DotDataException;

    /**
     * Retrieves the basic metadata projection for the contentlet
     * @param contentlet  {@link Contentlet}
     * @param field       {@link Field}
     * @return Map
     */
    Metadata getMetadata(Contentlet contentlet, Field field) throws DotDataException;

    /**
     * Retrieves the basic metadata projection for the contentlet
     * @param contentlet
     * @param field
     * @param forceGenerate
     * @return
     * @throws DotDataException
     */
    Metadata getMetadata(final Contentlet contentlet, final Field field, final boolean forceGenerate) throws DotDataException;

    /**
     * Retrieves the basic metadata for the contentlet (a projection over the full MD)
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
    Metadata getMetadata(Contentlet contentlet, String fieldVariableName)
            throws DotDataException;

    /**
     * Retrieves the basic metadata for the contentlet (a projection over the full MD)
     * If the MD has never been generated this will force it's generation
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
     Metadata getMetadataForceGenerate(final Contentlet contentlet, final String fieldVariableName)
            throws DotDataException;

    /**
     * Retrieves the full metadata for the contentlet
     * When we specify that we must not perform a cache read it means we will be loading the FM stored in disc
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
    Metadata getFullMetadataNoCache(Contentlet contentlet, String fieldVariableName)
            throws DotDataException;

    /**
     * Retrieves the full metadata for the contentlet
     * if no MD exists this will force it's generation.
     * @param contentlet source content
     * @param fieldVariableName field
     * @return
     * @throws DotDataException
     */
    Metadata getFullMetadataNoCacheForceGenerate(final Contentlet contentlet,
            final String fieldVariableName) throws DotDataException;

    /**
     * Compiles all metadata for the contentlet returning a natural ordered map.
     * @param contentlet {@link Contentlet}
     * @return Map
     */
    Optional<Map<String, Metadata>> collectFieldsMetadata(Contentlet contentlet);

    /**
     * Removes metadata for a given Contentlet
     * @param contentlet
     * @return @{@link Map} with the info of the entries removed
     */
    Map<String, Set<String>> removeMetadata(Contentlet contentlet);

    /**
     * Given a binary file this will rely on the metadata generator to read the file and get the full md associated
     * As a fallback we can also pass the contentlet in case of read md failure
     * @param binary
     * @param fallbackContentlet
     * @return
     * @throws DotDataException
     */
    Metadata getFullMetadataNoCache(File binary, Supplier<Contentlet> fallbackContentlet)
            throws DotDataException;


    /**
     * This is meant to put custom attributes into the metadata storage associated with the first indexed binary
     * They're associated with the first indexed-binary since that's pretty much all we store for the full-metadata
     * They're also reflected in the basic cached metadata
     * @param contentlet the contentlet we want to associate the md with
     * @param customAttributesByField the additional attributes organized by binary field
     * @throws DotDataException
     */
    void putCustomMetadataAttributes(final Contentlet contentlet,
            final Map<String, Map<String, Serializable>> customAttributesByField) throws DotDataException;

    /**
     * Given a source contentlet this will grab all current meta and copy it into the destination contentlet
     * assuming both are of the same CT
     * @param source
     * @param destination
     */
    void copyMetadata(Contentlet source, Contentlet destination) throws DotDataException;

}
