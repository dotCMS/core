package com.dotcms.rest.api.v1.storage;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.storage.ContentletMetadata;
import com.dotcms.storage.ContentletMetadataAPI;
import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StoragePersistenceProvider.INSTANCE;
import com.dotcms.storage.StorageType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.ws.rs.BadRequestException;

/**
 * Helper for the {@link ContentStorageResource}
 * @author jsanca
 */
class ContentStorageHelper {

    private final ContentletAPI contentletAPI;
    private final ShortyIdAPI shortyIdAPI;
    private final FileStorageAPI fileStorageAPI;
    private final ContentletMetadataAPI contentletMetadataAPI;

    ContentStorageHelper() {
        this(APILocator.getContentletAPI(),APILocator.getShortyAPI(), APILocator.getFileStorageAPI(), APILocator.getContentletMetadataAPI());
    }

    @VisibleForTesting
    ContentStorageHelper(final ContentletAPI contentletAPI,final ShortyIdAPI shortyIdAPI, final FileStorageAPI fileStorageAPI,  final ContentletMetadataAPI contentletMetadataAPI) {
        this.contentletAPI = contentletAPI;
        this.shortyIdAPI = shortyIdAPI;
        this.fileStorageAPI = fileStorageAPI;
        this.contentletMetadataAPI = contentletMetadataAPI;
    }

   public Map<String,Object> push(final List<File> files) {

       final ImmutableMap.Builder<String, Object> bodyResultBuilder = new ImmutableMap.Builder<>();
       final StoragePersistenceProvider storagePersistenceProvider = INSTANCE.get();
       final StoragePersistenceAPI storage = storagePersistenceProvider.getStorage();
       for (final File file : files) {
           bodyResultBuilder.put(file.getName(),
                   storage.pushFile("files", File.separator + file.getName(), file, Collections.emptyMap()));
       }
       return bodyResultBuilder.build();
   }

    @CloseDBIfOpened
    Optional<Contentlet> getContentlet(final String inode,
            final String identifier,
            final long language,
            final Supplier<Long> sessionLanguage,
            final User user,
            final PageMode pageMode) throws DotDataException, DotSecurityException {

        PageMode mode = pageMode;

        if (UtilMethods.isSet(inode)) {

            final Contentlet contentlet = this.contentletAPI.find(inode, user, mode.respectAnonPerms);
            if (null == contentlet) {
                throw new DoesNotExistException("contentlet-was-not-found");
            }
            return Optional.of(contentlet);

        } else if (UtilMethods.isSet(identifier)) {

            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<ShortyId> shortyIdOptional = shortyIdAPI.getShorty(identifier);
            final String longIdentifier =
                    shortyIdOptional.isPresent() ? shortyIdOptional.get().longId : identifier;

            return language <= 0 ?
                    getContentletByIdentifier(longIdentifier, mode, user, sessionLanguage) :
                    contentletAPI.findContentletByIdentifierOrFallback
                            (longIdentifier, mode.showLive, language, user, mode.respectAnonPerms);
        }
        return Optional.empty();
    }

    ContentletMetadata generateContentletMetadata(final String inode,
            final String identifier,
            final long languageId,
            final Supplier<Long> sessionLanguage,
            final User user,
            final PageMode pageMode) throws DotSecurityException, DotDataException, IOException {

        final Optional<Contentlet> optional = getContentlet(inode, identifier, languageId,
                sessionLanguage, user, pageMode);
        if (!optional.isPresent()) {
            throw new DoesNotExistException("contentlet-was-not-found");
        }
        return contentletMetadataAPI.generateContentletMetadata(optional.get());

    }

    private Optional<Contentlet> getContentletByIdentifier(final String identifier,
                                                          final PageMode mode,
                                                          final User user,
                                                          final Supplier<Long> sessionLanguageSupplier) throws DotDataException {

        Contentlet contentlet = null;
        final long sessionLanguage  = sessionLanguageSupplier.get();

        if(sessionLanguage > 0) {

            contentlet = Try.of(()->this.contentletAPI.findContentletByIdentifier
                    (identifier, mode.showLive, sessionLanguage, user, mode.respectAnonPerms)).getOrNull();
        }

        if (null == contentlet) {

            final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            if (defaultLanguage != sessionLanguage) {

                contentlet =  Try.of(()->this.contentletAPI.findContentletByIdentifier
                        (identifier, mode.showLive, defaultLanguage, user, mode.respectAnonPerms)).getOrNull();
            }
        }

        return null == contentlet?
                Optional.ofNullable(this.contentletAPI.findContentletByIdentifierAnyLanguage(identifier)):
                Optional.of(contentlet);
    }


    Map<String, Object> getMetadata(final String inode,
            final String identifier,
            final long languageId,
            final Supplier<Long> sessionLanguage,
            final User user,
            final PageMode pageMode, final MetadataForm metadataForm)
            throws DotSecurityException, DotDataException {
        final Optional<Contentlet> optional = getContentlet(inode, identifier, languageId,
                sessionLanguage, user, pageMode);
        if (!optional.isPresent()) {
            throw new DoesNotExistException("contentlet-was-not-found");
        }
        final Contentlet contentlet = optional.get();
        final Map<String, Field> fieldMap = contentlet.getContentType().fieldMap();

        if (!fieldMap.containsKey(metadataForm.getField())) {
            throw new BadRequestException(
                    "Field variable sent, is not valid for the contentlet: " + identifier);
        }

        return
                !metadataForm.isCache() ?
                        this.contentletMetadataAPI
                                .getMetadataNoCache(contentlet, metadataForm.getField()) :
                        this.contentletMetadataAPI.getMetadata(contentlet, metadataForm.getField());
    }


}
