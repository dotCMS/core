package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the template tool bound as {@code $crypt}.
 *
 * <p>Encrypting is open to every template. Decrypting runs only when the author of the code being
 * rendered may use the restricted view tools, as decided by
 * {@link RestrictedViewToolAccess#renderingAuthorHasAccess(HttpServletRequest)} for the request
 * the tool was initialised with.
 */
class CryptToolTest {

    private static final String PLAIN = "plain-value";
    private static final String ENCRYPTED = "ZW5jcnlwdGVkLXZhbHVl";

    private MockedStatic<PublicEncryptionFactory> encryption;
    private HttpServletRequest request;
    private CryptTool tool;

    @BeforeEach
    void setUp() {
        encryption = mockStatic(PublicEncryptionFactory.class);
        encryption.when(() -> PublicEncryptionFactory.decryptString(ENCRYPTED)).thenReturn(PLAIN);
        encryption.when(() -> PublicEncryptionFactory.encryptString(PLAIN)).thenReturn(ENCRYPTED);

        request = mock(HttpServletRequest.class);
        final ViewContext context = mock(ViewContext.class);
        when(context.getRequest()).thenReturn(request);
        tool = new CryptTool();
        tool.init(context);
    }

    @AfterEach
    void tearDown() {
        encryption.close();
    }

    /**
     * Method to test: {@link CryptTool#decrypt(String)}
     * Given: a tool initialised with a request whose rendering author has access
     * When: decrypting a value
     * Should: return the plain text the instance key yields
     */
    @Test
    void decrypt_authorWithAccess_returnsPlainText() {
        try (MockedStatic<RestrictedViewToolAccess> gate = mockStatic(RestrictedViewToolAccess.class)) {
            gate.when(() -> RestrictedViewToolAccess.renderingAuthorHasAccess(request)).thenReturn(true);

            assertEquals(PLAIN, tool.decrypt(ENCRYPTED));
        }
    }

    /**
     * Method to test: {@link CryptTool#decrypt(String)}
     * Given: a tool initialised with a request whose rendering author has no access
     * When: decrypting a value
     * Should: return null without decrypting anything
     */
    @Test
    void decrypt_authorWithoutAccess_returnsNullAndDoesNotDecrypt() {
        try (MockedStatic<RestrictedViewToolAccess> gate = mockStatic(RestrictedViewToolAccess.class)) {
            gate.when(() -> RestrictedViewToolAccess.renderingAuthorHasAccess(any())).thenReturn(false);

            assertNull(tool.decrypt(ENCRYPTED));
            encryption.verify(() -> PublicEncryptionFactory.decryptString(anyString()), never());
        }
    }

    /**
     * Method to test: {@link CryptTool#decrypt(String)}
     * Given: a tool initialised with a given request
     * When: decrypting a value
     * Should: ask about the author of that same request
     */
    @Test
    void decrypt_asksAboutTheRequestTheToolWasInitialisedWith() {
        try (MockedStatic<RestrictedViewToolAccess> gate = mockStatic(RestrictedViewToolAccess.class)) {
            gate.when(() -> RestrictedViewToolAccess.renderingAuthorHasAccess(any())).thenReturn(false);

            tool.decrypt(ENCRYPTED);

            gate.verify(() -> RestrictedViewToolAccess.renderingAuthorHasAccess(request));
        }
    }

    /**
     * Method to test: {@link CryptTool#decrypt(String)}
     * Given: a tool initialised with something that is not a view context, so it holds no request
     * When: decrypting a value
     * Should: return null without decrypting anything
     */
    @Test
    void decrypt_withoutRequest_returnsNull() {
        final CryptTool withoutRequest = new CryptTool();
        withoutRequest.init(new Object());

        assertNull(withoutRequest.decrypt(ENCRYPTED));
        encryption.verify(() -> PublicEncryptionFactory.decryptString(anyString()), never());
    }

    /**
     * Method to test: {@link CryptTool#decrypt(String)}
     * Given: an author with access and a value the instance key cannot decrypt
     * When: decrypting it
     * Should: return null instead of failing the render
     */
    @Test
    void decrypt_undecryptableValue_returnsNull() {
        encryption.when(() -> PublicEncryptionFactory.decryptString("not-encrypted"))
                .thenThrow(new DotRuntimeException("decryption Failed"));
        try (MockedStatic<RestrictedViewToolAccess> gate = mockStatic(RestrictedViewToolAccess.class)) {
            gate.when(() -> RestrictedViewToolAccess.renderingAuthorHasAccess(request)).thenReturn(true);

            assertNull(tool.decrypt("not-encrypted"));
        }
    }

    /**
     * Method to test: {@link CryptTool#crypt(String)}
     * Given: a tool whose rendering author has no access
     * When: encrypting a value
     * Should: return the encrypted value; encrypting is open to every template
     */
    @Test
    void crypt_authorWithoutAccess_stillEncrypts() {
        try (MockedStatic<RestrictedViewToolAccess> gate = mockStatic(RestrictedViewToolAccess.class)) {
            gate.when(() -> RestrictedViewToolAccess.renderingAuthorHasAccess(any())).thenReturn(false);

            assertEquals(ENCRYPTED, tool.crypt(PLAIN));
        }
    }

    /**
     * Method to test: {@link CryptTool#crypt(String)}
     * Given: encryption fails
     * When: encrypting a value
     * Should: return null instead of failing the render
     */
    @Test
    void crypt_encryptionFails_returnsNull() {
        encryption.when(() -> PublicEncryptionFactory.encryptString("failing"))
                .thenThrow(new DotRuntimeException("Encryption Failed"));

        assertNull(tool.crypt("failing"));
    }
}
