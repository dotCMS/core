package com.dotcms.rest.api.v1.page;

import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.liferay.portal.model.User;
import java.time.Instant;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutablePageRenderParams.class)
@JsonDeserialize(as = ImmutablePageRenderParams.class)
@Value.Immutable
public interface PageRenderParams {

    /**
     * The original request that was made to the server. This is the request that was made to the server before any
     * @return the original {@link HttpServletRequest} object
     */
    HttpServletRequest originalRequest();

    /**
     * The decorated {@link HttpServletRequest} object, which includes
     * special handling in some of its properties.
     * @return the decorated {@link HttpServletRequest} object
     */
    HttpServletRequest request();

    /**
     * The {@link HttpServletResponse} object that will be used to send the response back to the client.
     * @return the {@link HttpServletResponse} object
     */
    HttpServletResponse response();

    /**
     * The {@link User} object that represents the user that is making the request.
     * @return the {@link User} object
     */
    User user();

    /**
     * The {@link Host} object that represents the host that the request is being made to.
     * @return the {@link Host} object
     */
    String uri();

    /**
     * The language id of the language that the request is being made in.
     * @return the language id
     */
    @Value.Default
    default String languageId(){
       return String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId());
    }

    /**
     * The current {@link PageMode} used to render the page.
     * @return the current {@link PageMode}
     */
    Optional<String> modeParam();

    /**
     * The {@link java.lang.String}'s inode to render the page. This is used
     * @return the {@link java.lang.String}'s inode
     */
    Optional<String> deviceInode();

    /**
     * If only the HTML PAge's metadata must be returned in the JSON
     * response, set this to {@code true}. Otherwise, if the rendered
     * Containers and page content must be returned as well, set it to
     * {@code false}.
     * @return {@code true} if only the HTML Page's metadata must be
     */
    @Value.Default
    default boolean asJson(){ return false; }

    /**
     * Time machine date to use for rendering the page. If this is set, the page will be rendered as if it was published at this date.
     * @return
     */
    Optional<Instant> timeMachineDate();
}
