package com.dotmarketing.business;

import static com.dotmarketing.util.UUIDUtil.isUUID;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;


public class DeterministicIdentifierAPIImpl implements DeterministicIdentifierAPI {

    static final String GENERATE_DETERMINISTIC_IDENTIFIERS = "GENERATE_DETERMINISTIC_IDENTIFIERS";

    private static final int MAX_ATTEMPTS = 3;

    static final String NON_DETERMINISTIC_IDENTIFIER = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    private final FolderAPI folderAPI;
    private final HostAPI hostAPI;
    private final ContentTypeAPI contentTypeAPI;
    private final CategoryAPI categoryAPI;
    private final Supplier<String> uuidSupplier;
    private final Function<String, String> hashFunction;

    DeterministicIdentifierAPIImpl() {
        this(APILocator.getFolderAPI(),
             APILocator.getHostAPI(),
             APILocator.getContentTypeAPI(APILocator.systemUser()),
             APILocator.getCategoryAPI(),
             UUIDGenerator::generateUuid,
             DigestUtils::sha256Hex);
    }

    @VisibleForTesting
    DeterministicIdentifierAPIImpl(final FolderAPI folderAPI, final HostAPI hostAPI, final ContentTypeAPI contentTypeAPI, final CategoryAPI categoryAPI, final Supplier<String> uuidSupplier, final Function<String, String> hashFunction) {
        this.folderAPI = folderAPI;
        this.hostAPI = hostAPI;
        this.contentTypeAPI = contentTypeAPI;
        this.categoryAPI = categoryAPI;
        this.uuidSupplier = uuidSupplier;
        this.hashFunction = hashFunction;
    }

    /**
     * Resolves the name used to generate a seed for the given params
     * @param asset
     * @return
     */
    @VisibleForTesting
    String resolveAssetName(final Versionable asset) {

        if (asset instanceof Contentlet) {

            final Contentlet contentlet = (Contentlet) asset;

            try {

                if (contentlet.isHost()) {
                    return contentlet.getStringProperty(Host.HOST_NAME_KEY);
                }

                if (contentlet.isFileAsset()) {
                    return contentlet.getBinary(FileAssetAPI.BINARY_FIELD).getName();
                }

                if (contentlet.isHTMLPage()) {
                    return contentlet.getStringProperty(HTMLPageAssetAPI.URL_FIELD);
                }

                if (contentlet.isPersona()) {
                    return contentlet.getStringProperty(PersonaAPI.KEY_TAG_FIELD);
                }

                //This should handle cases like a multi-binary contentlets

                final List<String> binaryNames = contentlet.getContentType().fields(BinaryField.class)
                        .stream()
                        .map(field -> Try.of(() -> contentlet.getBinary(field.variable()).getName())
                                .getOrNull()).filter(Objects::nonNull).collect(Collectors
                                .toList());
                if(!binaryNames.isEmpty()) {
                    final StringBuilder builder = new StringBuilder();
                    final Iterator<String> iterator = binaryNames.iterator();
                    while(iterator.hasNext()){
                       builder.append(iterator.next());
                       if(iterator.hasNext()){
                         builder.append(StringPool.COLON);
                       }
                    }
                    if (builder.length() > 0) {
                        return builder.toString();
                    }
                }


            } catch (Exception e) {
                Logger.error(DeterministicIdentifierAPIImpl.class, e);
                throw new DotRuntimeException(String.format(
                        "Failed resolving asset name for contentlet with title `%s` and id `%s` ",
                        contentlet.getTitle(), contentlet.getIdentifier()), e);
            }
        } else {
            if (asset instanceof WebAsset) {
                return ((WebAsset) asset).getTitle();
            }
            if (asset instanceof Folder) {
                return ((Folder) asset).getName();
            }
        }
        return uuidSupplier.get();

    }

    /**
     * Resolves the name used to generate a seed for the given params
     * @param asset
     * @return
     */
    @VisibleForTesting
    String resolveAssetType(final Versionable asset) {

        if (asset instanceof Contentlet) {
            final Contentlet contentlet = (Contentlet) asset;
            return contentlet.getContentType().variable();
        }

        return asset.getClass().getSimpleName();

    }

    /**
     * Generates the seed to the deterministic id for the given set of params
     * @param asset
     * @param parent
     * @return
     */
    private String deterministicIdSeed(final Versionable asset, final Treeable parent) {

        final Host parentHost = (parent instanceof Host) ? (Host) parent : Try.of(
                () -> hostAPI
                        .find(((Folder) parent).getHostId(), APILocator.systemUser(), false))
                .getOrElseThrow(DotRuntimeException::new);


        final Folder parentFolder = (parent instanceof Folder) ? (Folder) parent
                : Try.of(folderAPI::findSystemFolder)
                        .getOrElseThrow(DotRuntimeException::new);

        final String assetType = resolveAssetType(asset);
        final String assetName = resolveAssetName(asset);
        final String deterministicId = (assetType + StringPool.COLON + parentHost.getHostname() + StringPool.FORWARD_SLASH + parentFolder.getPath() + assetName).toLowerCase();

        Logger.debug(DeterministicIdentifierAPIImpl.class,
                String.format(" assetType: %s, assetName: %s,  deterministicId: %s", assetType, assetName,
                        deterministicId));

        return deterministicId;

    }

    /**
     * calculates a name used to compute the basic seed for a given CT
     * @param contentType the ContentType
     * @return the name
     */
    @VisibleForTesting
    String resolveAssetType(final ContentType contentType) {
        final BaseContentType baseContentType = contentType.baseType();
        if(UtilMethods.isNotSet(baseContentType.getAlternateName())){
            return baseContentType.getClass().getSimpleName();
        }
        return baseContentType.getAlternateName();
    }

    /**
     * This method is used by ContentTypes if the CT does not have a predefined variable-name installed on it we rely on the supplier.
     * @param contentType
     * @param contentTypeVarName
     * @return
     */
    @VisibleForTesting
    String resolveName(final ContentType contentType, final Supplier<String>contentTypeVarName) {
        String name = contentType.variable();
        if(UtilMethods.isNotSet(name)){
           name = contentTypeVarName.get();
        }
        return name;
    }

    /**
     * Generates the seed to the deterministic id for the given set of params
     * @param contentType
     * @param contentTypeVarName
     * @return
     */
    private String deterministicIdSeed(final ContentType contentType, final Supplier<String>contentTypeVarName) {
       final String assetType = resolveAssetType(contentType);
       final String name = resolveName(contentType, contentTypeVarName);
       final String seedId = String.format("%s:%s",assetType, name).toLowerCase();

       Logger.debug(DeterministicIdentifierAPIImpl.class, String.format(" contentType: %s, assetName: %s,  seedId: %s", assetType, name, seedId));
       return seedId;
    }

    /**
     * This method is used by Fields if the Field does not have a predefined variable-name installed on it we rely on the supplier.
     * @param field
     * @param fieldVarName
     * @return
     */
    @VisibleForTesting
    String resolveName(final Field field, final Supplier<String> fieldVarName) {
        String name = fieldVarName.get();
        if(UtilMethods.isNotSet(name)){
            name = field.variable();
        }
        return name;
    }

    /**
     * Generates the seed to the deterministic id for the given set of params
     * @param contentType
     * @param field
     * @param fieldVarName
     * @return
     */
    private String deterministicIdSeed(final ContentType contentType, final Field field, final Supplier<String>fieldVarName) {
        final String assetType = deterministicIdSeed(contentType, contentType::variable);
        final String name = resolveName(field, fieldVarName);
        final String seedId = String.format("%s:%s", assetType, name).toLowerCase();

        Logger.debug(DeterministicIdentifierAPIImpl.class, String.format(" contentType: %s, assetName: %s,  seedId: %s", assetType, name, seedId));
        return seedId;
    }

    /**
     * Generates the seed to the deterministic id
     * @param lang
     * @return
     */
    @VisibleForTesting
    String deterministicIdSeed(final Language lang){
       return  String.format("%s:%s:%s", Language.class.getSimpleName(),lang.getLanguageCode(),lang.getCountryCode()).trim();
    }

    /**
     * This method is responsible for calling the hashFunction and trim such result down to the max identifier length that guarantees compatibility
     * @param stringToHash
     * @return
     */
    private String hash(final String stringToHash) {
        final String hashed = hashFunction.apply(stringToHash).substring(0, 32);
        Logger.info(DeterministicIdentifierAPIImpl.class,
                "hashing id: " + stringToHash + " to " + hashed);
        return hashed;
    }

    /**
     * best effort means we Test the generated identifier against a table to see if it already has been taken previously
     * if not we use it as the next identifier but if it has been already taken then we rely on the fallback function.
     * The fallback function basically does what it was done before the introduction of the deterministic identifier api.
     * @param hash candidate hash that aspires to become an id
     * @param testIdentifierFunction Function that tests if the candidate hash is already in use.
     * @param fallbackIdentifier if the candidate is found to be already in use then we relay on the fallback id generator
     * @param <T> hash data-type
     * @return the new identifier ready to be inserted on a db
     */
    private <T> T bestEffortDeterministicId(final T hash, final Function<T,Boolean> testIdentifierFunction, final Supplier<T> fallbackIdentifier) {
        T candidateId = hash;
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            if (!testIdentifierFunction.apply(candidateId)) {
                return candidateId;
            }
            candidateId = fallbackIdentifier.get();
        }
        Logger.warn(DeterministicIdentifierAPIImpl.class,String.format("Attempt to generate id has failed instead a non deterministic id (%s) was returned. ",candidateId));
        return candidateId;
    }


    /**
     * Entry point for (Contentlets, Host, Persona, Templates, Folders, FileAsset)
     * @param asset
     * @param parent
     * @return
     */
    @CloseDBIfOpened
    @Override
    public String generateDeterministicIdBestEffort(final Versionable asset,
            final Treeable parent) {

        final IdentifierFactory identifierFactory = FactoryLocator.getIdentifierFactory();
        final Function<String, Boolean> testIdentifierFunction = identifierFactory::isIdentifier;
        return isEnabled() ? bestEffortDeterministicId(hash(deterministicIdSeed(asset, parent)),
                testIdentifierFunction, uuidSupplier) : uuidSupplier.get();
    }

    /**
     * Entry point for Content-Types
     * @param contentType CT to generate the id for
     * @param contentTypeVarName sometimes the var-name isn't set on the contentType
     * which is immutable in such cases we need to rely on this supplier.
     * @return generated deterministic id
     */
    @CloseDBIfOpened
    @Override
    public String generateDeterministicIdBestEffort(final ContentType contentType,
            final Supplier<String> contentTypeVarName) {

        return isEnabled() ? bestEffortDeterministicId(
                hash(deterministicIdSeed(contentType, contentTypeVarName)),
                this::isContentTypeInode, uuidSupplier) : uuidSupplier.get();
    }

    /**
     * Entry point for field id generation
     * @param throwAwayField a field that might or might not has been initialized
     * @return generated deterministic id
     */
    @CloseDBIfOpened
    @Override
    public String generateDeterministicIdBestEffort(final Field throwAwayField,
            final Supplier<String> fieldVarName) {
        Preconditions.checkNotNull(throwAwayField.contentTypeId(), "contentTypeRequired");
        Preconditions.checkNotNull(fieldVarName.get(), "contentTypeVariableRequired");

        if(isEnabled()){
            final ContentType contentType = Try
                    .of(() -> contentTypeAPI.find(throwAwayField.contentTypeId()))
                    .getOrElseThrow(DotRuntimeException::new);

            return bestEffortDeterministicId(
                    hash(deterministicIdSeed(contentType, throwAwayField, fieldVarName)),
                    this::isFieldInode, uuidSupplier);

        }
        return uuidSupplier.get();

    }

    /**
     * Entry point for category id generation
     * @param category a category
     * @return generated deterministic id
     */
    @CloseDBIfOpened
    @Override
    public String generateDeterministicIdBestEffort(final Category category, final Category parent){
        return isEnabled() ? bestEffortDeterministicId(
                hash(deterministicIdSeed(category, parent)),
                this::isCategoryId, uuidSupplier) : uuidSupplier.get();
    }

    @VisibleForTesting
    String deterministicIdSeed(final Category category, final Category parent){
        String path = "";
        try {
            path = buildPath( category, parent );
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(DeterministicIdentifierAPIImpl.class, e);
        }
        return String.format("Category:{%s}",path);
    }

    private String buildPath(final Category category, final Category parent)
            throws DotDataException, DotSecurityException {
        String path = "";
        if (null != parent) {
            path = buildPath(parent, path);
            if (UtilMethods.isSet(path)) {
                path = path + " > " + category.getCategoryVelocityVarName();
            } else {
                path = category.getCategoryVelocityVarName();
            }
        } else {
            path = category.getCategoryVelocityVarName();
        }
        return path;
    }

    private String buildPath(final Category category, String path) throws DotDataException, DotSecurityException {

        final List<Category> parents = categoryAPI.getParents(category, APILocator.systemUser(), false);
        if(!parents.isEmpty()) {
            final Category parent = parents.get(0);
            path = parent.getCategoryVelocityVarName() + " > " + category.getCategoryVelocityVarName();
            path = buildPath(parent, path);
        } else {
            path = StringUtils.isSet(path) ? path : category.getCategoryVelocityVarName();
        }
        return path;
    }

    /**
     * Given a Language this will evaluate the code and country code if any then generate a sha256 and finally will hash it out into a long val
     * @param lang
     * @return generated deterministic id
     */
    @CloseDBIfOpened
    @Override
    public long generateDeterministicIdBestEffort(final Language lang) {

        return isEnabled() ? bestEffortDeterministicId(simpleHash(hash(deterministicIdSeed(lang))),
                this::isLanguageId,
                this::nextLangId) : nextLangId();
    }


    private synchronized long nextLangId(){
        return System.currentTimeMillis();
    }

    /**
     * Test if the calculated hash has already been used as a language identifier
     * @param hash
     * @return
     */
    private boolean isLanguageId(final long hash){
        return new DotConnect()
                .setSQL("select count(*) as test from language where id =?")
                .addParam(hash)
                .getInt("test")>0;
    }

    /**
     * Test if the calculated hash has already been used
     * @param hash
     * @return
     */
    private boolean isContentTypeInode(final String hash){
        return new DotConnect()
                .setSQL("select count(*) as test from structure s join inode i on s.inode = i.inode where s.inode =?")
                .addParam(hash)
                .getInt("test")>0;

    }

    /**
     * Test the calculated hash has already been used as a field identifier or inode
     * @param hash
     * @return
     */
    private boolean isFieldInode(final String hash){
        return new DotConnect()
                .setSQL("select count(*) as test from field f join inode i on f.inode = i.inode where i.inode =?")
                .addParam(hash)
                .getInt("test")>0;
    }

    /**
     * Test the calculated hash has already been used as a category inode
     * @param hash
     * @return
     */
    private boolean isCategoryId(final String hash){
        return new DotConnect()
                .setSQL("SELECT count(*) as test  FROM inode, category WHERE inode.inode = category.inode AND inode.type = 'category' AND inode.inode =?")
                .addParam(hash)
                .getInt("test")>0;
    }

    /**
     * Basically takes a string and hash it into a long number (not extremely long though).
     * As this is meant to be used as an id take into account that not every java long can be represented in javascript therefore this number can be excessively large
     * That is why this has is relatively simple
     * Used to create a deterministic long representation out of a sha256
     * @param string
     * @return
     */
     @VisibleForTesting
     long simpleHash(final String string) {
        final int len = string.length();
        long hash = 0;
        for (int i = 0; i < len; i++) {
            hash += 131L * (i + 1) * string.charAt(i);
        }
        return hash;
    }

    /**
     * This should tell you we're looking at a deterministic id
     * @param id
     * @return
     */
    public boolean isDeterministicId(final String id){
       return isUUID(id) && !id.matches(NON_DETERMINISTIC_IDENTIFIER);
    }

    /**
     * Tells us if this generator has been disabled
     * @return
     */
   public boolean isEnabled(){
       return (Config.getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true));
   }

}
