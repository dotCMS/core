package com.dotcms.rendering.velocity.viewtools;

import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * Template tool bound as {@code $crypt}: encrypts and decrypts text with the key of this dotCMS
 * instance.
 *
 * <p>Encrypting is available to every template. Decrypting runs only when the code being
 * rendered was written by a user who may use the restricted view tools, the same rule
 * {@code $sql} and {@code $dotsecrets} follow (see
 * {@link RestrictedViewToolAccess#renderingAuthorHasAccess(HttpServletRequest)}). Otherwise it
 * returns {@code null}, which {@code $!crypt.decrypt(...)} renders as nothing.
 *
 * <p>The tool is bound per request, because that is where the renderer records who wrote the
 * code being run. Java code that needs the same operations without a template can keep using
 * {@link CryptWebAPI} or {@link PublicEncryptionFactory}.
 */
public class CryptTool implements ViewTool {

    /** The request the tool was initialised with; it carries the author of the rendered code. */
    private HttpServletRequest request;

    /**
     * Keeps the request of the render this tool belongs to. Anything other than a view context
     * leaves the tool without a request, and then it never decrypts.
     *
     * @param initData the view context Velocity hands to request-scoped tools
     */
    @Override
    public void init(final Object initData) {
        this.request = initData instanceof ViewContext
                ? ((ViewContext) initData).getRequest()
                : null;
    }

    /**
     * Encrypts a text with the instance key.
     *
     * @param text the text to encrypt
     * @return the encrypted text, or {@code null} when it cannot be encrypted
     */
    public String crypt(final String text) {
        try {
            return PublicEncryptionFactory.encryptString(text);
        } catch (final Exception e) {
            Logger.debug(this, () -> "Unable to encrypt the given text: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decrypts a text encrypted with the instance key, when the author of the code being rendered
     * may do so.
     *
     * @param text the encrypted text
     * @return the plain text, or {@code null} when the author of the rendered code may not
     * decrypt or the text cannot be decrypted
     */
    public String decrypt(final String text) {
        if (!RestrictedViewToolAccess.renderingAuthorHasAccess(this.request)) {
            return null;
        }
        try {
            return PublicEncryptionFactory.decryptString(text);
        } catch (final Exception e) {
            Logger.debug(this, () -> "Unable to decrypt the given text: " + e.getMessage());
            return null;
        }
    }
}
