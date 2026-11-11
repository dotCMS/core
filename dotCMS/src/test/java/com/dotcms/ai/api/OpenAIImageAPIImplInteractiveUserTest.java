package com.dotcms.ai.api;

import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The image helper hands a request it builds itself to the temp-file API, which resolves the acting user from
 * it. That user must be the one the helper was created for, including the built-in account.
 */
public class OpenAIImageAPIImplInteractiveUserTest {

    private HttpServletRequest previousRequest;

    @Before
    public void clearThreadRequest() {
        previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    @After
    public void restoreThreadRequest() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
    }

    private static User user(final String id) {
        final User user = new User();
        user.setUserId(id);
        return user;
    }

    /** Runs the helper once and returns the user id the temp-file API resolves from the request it received. */
    private static String resolvedByTempFileApi(final User actingUser) throws Exception {
        final AppConfig config = mock(AppConfig.class);
        when(config.getImageModel()).thenReturn(
                AIModel.builder().withType(AIModelType.IMAGE).withModelNames("some-image-model").build());
        when(config.getImageSize()).thenReturn("some-image-size");
        final TempFileAPI tempFileApi = mock(TempFileAPI.class);
        final AtomicReference<String> resolved = new AtomicReference<>();
        final File file = mock(File.class);
        when(file.getName()).thenReturn(UUIDGenerator.shorty());
        when(file.getPath()).thenReturn("/some/path/here");
        when(tempFileApi.createTempFileFromUrl(anyString(), any(), any(), anyInt())).thenAnswer(i -> {
            final User seen = PortalUtil.getUser((HttpServletRequest) i.getArgument(1));
            resolved.set(seen == null ? null : seen.getUserId());
            return new DotTempFile("some-id", file);
        });
        final AIImageRequestDTO.Builder dtoBuilder = mock(AIImageRequestDTO.Builder.class);
        final ImageAPI service = new OpenAIImageAPIImpl(config, actingUser, mock(HostAPI.class), tempFileApi) {
            @Override
            public String doRequest(final JSONObject json) {
                return "{\"data\":[{\"url\":\"http://localhost:8080\",\"value\":\"v\"}]}";
            }

            @Override
            public User getUser() {
                return actingUser;
            }

            @Override
            public AIImageRequestDTO.Builder getDtoBuilder() {
                return dtoBuilder;
            }
        };
        final JSONObject json = new JSONObject();
        json.put("prompt", "Hello World!");
        service.sendRequest(json);
        return resolved.get();
    }

    /** The built-in account the helper acts as is what the temp-file API resolves. */
    @Test
    public void temp_file_api_resolves_the_built_in_account_the_helper_acts_as() throws Exception {
        assertEquals(UserAPI.SYSTEM_USER_ID, resolvedByTempFileApi(user(UserAPI.SYSTEM_USER_ID)));
    }

    /** Control: a regular user is resolved unchanged. */
    @Test
    public void temp_file_api_resolves_a_regular_user_unchanged() throws Exception {
        assertEquals("dotcms.org.1", resolvedByTempFileApi(user("dotcms.org.1")));
    }
}
