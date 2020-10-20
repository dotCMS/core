package com.dotcms.rendering.velocity.directive;

import java.io.File;
import java.io.Writer;
import java.util.Optional;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.util.ConversionUtils;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;


public class DotParse extends DotDirective {

    private static final long serialVersionUID = 1L;

    private final String hostIndicator = "//";
    private final String EDIT_ICON =
                    "<div data-dot-object='vtl-file' data-dot-inode='%s' data-dot-url='%s' data-dot-can-read='%s' data-dot-can-edit='%s'></div>";

    @Override
    public final String getName() {

        return "dotParse";
    }


    private static final String CONTENT_LANGUAGE_ID = "_contentlanguageid";

    /**
     * In case than an upper component needs to overrides the language to get the version of the
     * content, you can use this method to do it in advance. note: the change will just affect the calls
     * to dotParse at request scope.
     *
     * @param request {@link HttpServletRequest}
     * @param identifier {@link String} contentlet identifier
     * @param languageId {@link Long} language identifier
     */
    public static void setContentLanguageId(final HttpServletRequest request, final String identifier, final long languageId) {

        final String contentLanguageId = identifier + CONTENT_LANGUAGE_ID;
        request.setAttribute(contentLanguageId, languageId);
    }

    private static long getContentLanguageId(final HttpServletRequest request, final String identifier,
                    final long defaultLanguageId) {

        final String contentLanguageId = identifier + CONTENT_LANGUAGE_ID;
        return null != request && null != request.getAttribute(contentLanguageId)
                        ? ConversionUtils.toLong(request.getAttribute(contentLanguageId), defaultLanguageId)
                        : defaultLanguageId;
    }


    @Override
    String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params, final String[] arguments) {


        final String templatePath = arguments[0];

        final User user = params.user;
        final HttpServletRequest request = (HttpServletRequest) context.get("request");


        try {
            Optional<Tuple2<Identifier, String>> optIdAndField = resolveDotAsset(params, templatePath);

            if (!optIdAndField.isPresent()) {
                optIdAndField = resolveFileAsset(params, templatePath);
            }


            if (!optIdAndField.isPresent()) {
                throwNotResourceNotFoundException(params, templatePath);
            }


            final Tuple2<Identifier, String> idAndField = optIdAndField.get();

            final long languageId = getContentLanguageId(request, idAndField._1.getId(), params.language.getId());

            // Verify if we found a resource with the given path
            if (null == idAndField._1 || !UtilMethods.isSet(idAndField._1.getId())) {

                throwNotResourceNotFoundException(params, templatePath);
            }

            Optional<ContentletVersionInfo> contentletVersionInfo =
                            APILocator.getVersionableAPI().getContentletVersionInfo(idAndField._1.getId(), languageId);

            if (!contentletVersionInfo.isPresent()
                    || UtilMethods.isNotSet(contentletVersionInfo.get().getIdentifier())
                            || contentletVersionInfo.get().isDeleted()) {

                final long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
                if (defaultLang != languageId) {
                    contentletVersionInfo =
                                    APILocator.getVersionableAPI().getContentletVersionInfo(idAndField._1.getId(), defaultLang);
                }
            }

            if (!contentletVersionInfo.isPresent()
                    || UtilMethods.isNotSet(contentletVersionInfo.get().getIdentifier())) {
                throwNotResourceNotFoundException(params, templatePath);
            }

            final String inode =
                            params.mode.showLive ? contentletVersionInfo.get().getLiveInode()
                                    : contentletVersionInfo.get().getWorkingInode();

            // We found the resource but not the version we are looking for
            if (!UtilMethods.isSet(inode)) {
                throwNotResourceNotFoundException(params, templatePath);
            }

            final boolean respectFrontEndRolesForVTL =
                            params.mode.respectAnonPerms ? Config.getBooleanProperty("RESPECT_FRONTEND_ROLES_FOR_DOTPARSE", true)
                                            : params.mode.respectAnonPerms;
            final Contentlet contentlet = APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(),
                            respectFrontEndRolesForVTL);
            final File fileToServe = contentlet.getBinary(idAndField._2);
            
            
            

            // add the edit control if we have run through a page render
            if (!context.containsKey("dontShowIcon") && PageMode.EDIT_MODE == params.mode && context.containsKey("dotPageContent")) {
                final String editIcon = String.format(EDIT_ICON, contentlet.getInode(), idAndField._1.getAssetName(),
                                APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ,
                                                user),
                                APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT,
                                                user));
                writer.append(editIcon);
            }

            return UtilMethods.isSet(fileToServe) ? fileToServe.getAbsolutePath() : null;
        } catch (ResourceNotFoundException e) {
            Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: " + e.getMessage());
            if (e.getStackTrace().length > 0) {
                Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
            }
            throw e;
        } catch (DotSecurityException e) {
            Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: " + e.getMessage());
            if (e.getStackTrace().length > 0) {
                Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
            }
            throw new ResourceNotFoundException(e);
        } catch (Exception e) {
            Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: " + e.getMessage());
            if (e.getStackTrace().length > 0) {
                Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
            }
            throw new DotStateException(e);
        }
    }

    private void throwNotResourceNotFoundException(final RenderParams params, final String templatePath) {
        final String errorMessage = String.format("Not found %s version of [%s]", params.mode, templatePath);
        throw new ResourceNotFoundException(errorMessage);
    }



    private Optional<Tuple2<Identifier, String>> resolveDotAsset(final RenderParams params,
                    final String templatePath) throws DotDataException, DotSecurityException {

        // if we have a dotAsset
        if (!templatePath.startsWith("/dA")) {
            return Optional.empty();
        }

        final StringTokenizer tokens = new StringTokenizer(templatePath, StringPool.FORWARD_SLASH);

        if (tokens.countTokens() < 2) {
            final String errorMessage = String.format("No resource found for [%s]", templatePath);
            throw new ResourceNotFoundException(errorMessage);
        }

        tokens.nextToken();
        final String inodeOrIdentifier = tokens.nextToken();
        final String fieldVar = tokens.hasMoreTokens() ? tokens.nextToken() : DotAssetContentType.ASSET_FIELD_VAR;
        final Optional<ShortyId> shortOpt = APILocator.getShortyAPI().getShorty(inodeOrIdentifier);

        if (!shortOpt.isPresent()) {
            return Optional.empty();
        }

        final ShortyId shorty = shortOpt.get();
        final Optional<Contentlet> conOpt = (shorty.type == ShortType.IDENTIFIER)
                        ? APILocator.getContentletAPI().findContentletByIdentifierOrFallback(shorty.longId, params.mode.showLive,
                                        params.language.getId(), params.user, false)
                        : Optional.ofNullable(
                                        APILocator.getContentletAPI().find(shorty.longId, params.user, params.mode.respectAnonPerms));

        if (!conOpt.isPresent()) {
            return Optional.empty();
        }

        final Identifier identifier = APILocator.getIdentifierAPI().find(conOpt.get().getIdentifier());
        return Optional.of(Tuple.of(identifier, fieldVar));


    }

    private Optional<Tuple2<Identifier, String>> resolveFileAsset(final RenderParams params,
                    String templatePath) throws DotDataException, DotSecurityException {
        final User user = params.user;
        Host host = params.currentHost;
        // if we have a host
        if (templatePath.startsWith(hostIndicator)) {
            templatePath = templatePath.substring(hostIndicator.length(), templatePath.length());
            final String hostName = templatePath.substring(0, templatePath.indexOf('/'));
            templatePath = templatePath.substring(templatePath.indexOf('/'), templatePath.length());
            host = APILocator.getHostAPI().resolveHostName(hostName, user, params.mode.respectAnonPerms);
        }


        final Identifier identifier = APILocator.getIdentifierAPI().find(host, templatePath);
        return Optional.of(Tuple.of(identifier, FileAssetAPI.BINARY_FIELD));
    }

}

