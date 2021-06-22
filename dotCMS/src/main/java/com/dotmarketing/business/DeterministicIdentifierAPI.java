package com.dotmarketing.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.util.function.Supplier;


/**
 * A Deterministic identifier is One that can be predicted based on certain components of the Type we're saving
 * So that we minimize conflicts on distributed nodes.
 * The idea is that the same piece of content will lead to the same database id.
 */
public interface DeterministicIdentifierAPI {

    /**
     * Entry point for (Contentlets, Host, Persona, Templates, Folders, FileAsset)
     * @param asset
     * @param parent
     * @return
     */
    String generateDeterministicIdBestEffort(Versionable asset, Treeable parent);

    /**
     * Entry point for Content-Types
     * @param contentType CT to generate the id for
     * @param contentTypeVarName sometimes the var-name isn't set on the contentType
     * which is immutable in such cases we need to rely on this supplier.
     * @return generated deterministic id
     */
    String generateDeterministicIdBestEffort(ContentType contentType, Supplier<String> contentTypeVarName);

    /**
     * Given a Language this will evaluate the code and country code if any then generate a sha256 and finally will hash it out into a long val
     * @param lang
     * @return
     */
    long generateDeterministicIdBestEffort(Language lang);

    /**
     * This should tell you we're looking at a deterministic id
     * @param id
     * @return
     */
    boolean isDeterministicId(String id);

}
