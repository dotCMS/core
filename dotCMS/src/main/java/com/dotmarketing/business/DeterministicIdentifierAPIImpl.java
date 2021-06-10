package com.dotmarketing.business;

import static com.dotmarketing.util.UUIDUtil.isUUID;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotRuntimeException;
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
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;

public class DeterministicIdentifierAPIImpl implements DeterministicIdentifierAPI {

    static final String GENERATE_DETERMINISTIC_IDENTIFIERS = "GENERATE_DETERMINISTIC_IDENTIFIERS";

    private static final int MAX_ATTEMPTS = 3;

    private static final long PRIME = 1125899906842597L;

    static final String NON_DETERMINISTIC_IDENTIFIER = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    private final FolderAPI folderAPI;
    private final HostAPI hostAPI;
    private final Supplier<String> uuidSupplier;
    private final Function<String, String> hashFunction;

    DeterministicIdentifierAPIImpl() {
        this(APILocator.getFolderAPI(), APILocator.getHostAPI(),
             UUIDGenerator::generateUuid,
             DigestUtils::sha256Hex);
    }

    @VisibleForTesting
    DeterministicIdentifierAPIImpl(final FolderAPI folderAPI, final HostAPI hostAPI, final Supplier<String> uuidSupplier, final Function<String, String> hashFunction) {
        this.folderAPI = folderAPI;
        this.hostAPI = hostAPI;
        this.uuidSupplier = uuidSupplier;
        this.hashFunction = hashFunction;
    }

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

    @VisibleForTesting
    String resolveAssetType(final Versionable asset) {

        if (asset instanceof Contentlet) {
            final Contentlet contentlet = (Contentlet) asset;
            return contentlet.getContentType().variable();
        }

        return asset.getClass().getSimpleName();

    }

    @CloseDBIfOpened
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

    @VisibleForTesting
    String resolveAssetType(final ContentType contentType) {
        final BaseContentType baseContentType = contentType.baseType();
        if(UtilMethods.isNotSet(baseContentType.getAlternateName())){
            return baseContentType.getClass().getSimpleName();
        }
        return baseContentType.getAlternateName();
    }

    @VisibleForTesting
    String resolveName(final ContentType contentType, final Supplier<String>contentTypeVarName) {
        String name = contentType.variable();
        if(UtilMethods.isNotSet(name)){
           name = contentTypeVarName.get();
        }
        return name;
    }

    /**
     * Generates the seed to the deterministic id
     * @param contentType
     * @param contentTypeVarName
     * @return
     */
    private String deterministicIdSeed(final ContentType contentType, final Supplier<String>contentTypeVarName) {
       final String assetType = resolveAssetType(contentType);
       final String name = resolveName(contentType, contentTypeVarName);
       final String deterministicId = String.format("%s:%s",assetType, name).toLowerCase();

       Logger.debug(DeterministicIdentifierAPIImpl.class, String.format(" contentType: %s, assetName: %s,  deterministicId: %s", assetType, name, deterministicId));
       return deterministicId;
    }

    /**
     * Generates the seed to the deterministic id
     * @param lang
     * @return
     */
    private String deterministicIdSeed(final Language lang){
       return  String.format("%s:%s:%s", Language.class.getClass().getSimpleName(),lang.getLanguageCode(),lang.getCountryCode());
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

    @CloseDBIfOpened
    private String bestEffortDeterministicId(final String hash) {

        final IdentifierFactory identifierFactory = FactoryLocator.getIdentifierFactory();
        String candidateId = hash;

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            if (!identifierFactory.isIdentifier(candidateId)) {
                return candidateId;
            }
            candidateId = uuidSupplier.get();
        }
        Logger.warn(DeterministicIdentifierAPIImpl.class,String.format("Attempt to generate id has failed instead a non deterministic id (%s) was returned.",candidateId));
        return candidateId;
    }

    @CloseDBIfOpened
    private String bestEffortDeterministicContentId(final String hash) {

        String candidateId = hash;

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            if (!isContentTypeInode(candidateId)) {
                return candidateId;
            }
            candidateId = uuidSupplier.get();
        }
        Logger.warn(DeterministicIdentifierAPIImpl.class,String.format("Attempt to generate content-type id has failed instead a non deterministic id (%s) was returned.",candidateId));
        return candidateId;
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
     * Entry point for (Contentlets, Host, Persona, Templates, Folders, FileAsset)
     * @param asset
     * @param parent
     * @return
     */
    @Override
    public String generateDeterministicIdBestEffort(final Versionable asset,
            final Treeable parent) {
        return enabled() ? bestEffortDeterministicId(hash(deterministicIdSeed(asset, parent)))
                : uuidSupplier.get();
    }

    /**
     * Entry point for Content-Types
     * @param contentType CT to generate the id for
     * @param contentTypeVarName sometimes the var-name isn't set on the contentType
     * which is immutable in such cases we need to rely on this supplier.
     * @return generated deterministic id
     */
    @Override
    public String generateDeterministicIdBestEffort(final ContentType contentType,
            final Supplier<String> contentTypeVarName) {
        return enabled() ? bestEffortDeterministicContentId(
                hash(deterministicIdSeed(contentType, contentTypeVarName)))
                : uuidSupplier.get();
    }

    /**
     * Given a Language this will evaluate the code and country code if any then generate a sha256 and finally will hash it out into a long val
     * @param lang
     * @return
     */
    @Override
    public long generateDeterministicIdBestEffort(final Language lang) {
        //You never know.. better to be safe than sorry
        if("US".equals(lang.getCountryCode()) &&  "en".equals(lang.getLanguageCode())){
          return 1;
        }
        return enabled() ? bestEffortDeterministicLanguageId(
                longHash(hash(deterministicIdSeed(lang)))) : nextLangId();
    }

    @CloseDBIfOpened
    private long bestEffortDeterministicLanguageId(final long hash) {

        long candidateId = hash;
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            if (!isLanguageId(candidateId)) {
                return candidateId;
            }
            candidateId = nextLangId();
        }
        Logger.warn(DeterministicIdentifierAPIImpl.class,String.format("Attempt to generate language id has failed instead a non deterministic id (%s) was returned.",candidateId));
        return candidateId;
    }

    private synchronized long nextLangId(){
        return System.currentTimeMillis();
    }

    /**
     * Test if the calculated hash has already been used
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
     * basically takes a string and hash it into a long number
     * Used to create a deterministic long representation out of a sha256
     * @param string
     * @return
     */
    private long longHash(final String string) {
        final int len = string.length();
        long hash = PRIME;
        for (int i = 0; i < len; i++) {
            hash = 31 * hash + string.charAt(i);
        }
        return Math.abs(hash);
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
   public boolean enabled(){
       return (Config.getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true));
   }

}
