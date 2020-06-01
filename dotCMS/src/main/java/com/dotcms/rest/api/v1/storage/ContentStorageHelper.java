package com.dotcms.rest.api.v1.storage;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.rest.InitDataObject;
import com.dotcms.util.DotPreconditions;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.Optional;
import java.util.function.Supplier;

public class ContentStorageHelper {

    private final ContentletAPI contentletAPI;

    public ContentStorageHelper() {
        this(APILocator.getContentletAPI());
    }

    @VisibleForTesting
    public ContentStorageHelper(final ContentletAPI contentletAPI) {
        this.contentletAPI = contentletAPI;
    }

    @CloseDBIfOpened
    public Contentlet getContentlet(final String inode,
                                     final String identifier,
                                     final long language,
                                     final Supplier<Long> sessionLanguage,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode) throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;
        PageMode mode         = pageMode;

        if(UtilMethods.isSet(inode)) {

            contentlet = this.contentletAPI.find
                    (inode, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.notNull(contentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);
        } else if (UtilMethods.isSet(identifier)) {

            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<ShortyId> shortyIdOptional = APILocator.getShortyAPI().getShorty(identifier);
            final String longIdentifier = shortyIdOptional.isPresent()? shortyIdOptional.get().longId:identifier;
            final Optional<Contentlet> currentContentlet =  language <= 0?
                    this.getContentletByIdentifier(longIdentifier, mode, initDataObject.getUser(), sessionLanguage):
                    this.contentletAPI.findContentletByIdentifierOrFallback
                            (longIdentifier, mode.showLive, language, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.isTrue(currentContentlet.isPresent(), ()-> "contentlet-was-not-found", DoesNotExistException.class);

            contentlet = currentContentlet.get();
        }

        DotPreconditions.notNull(contentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);

        return contentlet;
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
                Optional.ofNullable(contentlet);
    }


}
