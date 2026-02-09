package com.dotmarketing.portlets.browser;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.util.StringUtils.builder;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Provide util method to with folder and files path
 */
public class BrowserUtil {

    private static final String LAST_SELECTED_FOLDER_ID = "LAST_SELECTED_FOLDER_ID";
    private static final String HOST_INDICATOR = "//";
    final static List<DefaultPathResolver> defaultPathResolver;

    static {
        defaultPathResolver = list(
                (contentlet, field, user) -> resolveWithCurrentValue(contentlet, field),
                (contentlet, field, user) -> resolveWithFieldVariable(field, user),
                (contentlet, field, user) -> resolveWithFolderHostField(contentlet, user),
                (contentlet, field, user) -> resolveWithLastSelectedFolder()
        );
    }

    private static Optional<Folder> resolveWithLastSelectedFolder() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpSession session = request.getSession(false);

        final Object attribute = session.getAttribute(LAST_SELECTED_FOLDER_ID);
        try {
            if (UtilMethods.isSet(attribute)){
                final String lastSelectedFolderId = (String) attribute;

                    return Optional.ofNullable(
                            APILocator.getFolderAPI()
                                .find(lastSelectedFolderId, APILocator.systemUser(), false)
                    );
            } else {
                return Optional.empty();
            }
        } catch (DotSecurityException | DotDataException e) {
           return Optional.empty();
        }
    }

    private static Host getCurrentHost(final User user)
            throws DotDataException, DotSecurityException {

        final String  currentHostId = WebAPILocator.getHostWebAPI().getCurrentHost().getIdentifier();
        return APILocator.getHostAPI().find(currentHostId, user, false);
    }

    private static Optional<Folder> resolveWithFolderHostField(
            final Contentlet contentlet, final User user) {

        final List<com.dotcms.contenttype.model.field.Field> fields = contentlet.getContentType()
                .fields().stream()
                .filter(field -> HostFolderField.class.getName().equalsIgnoreCase(field.typeName()))
                .limit(1)
                .collect(Collectors.toList());

        if (UtilMethods.isSet(fields)) {
            final com.dotcms.contenttype.model.field.Field hostFolderField = fields.get(0);
            final String hostFolderValue = contentlet.getStringProperty(hostFolderField.variable());
            try {
                return Optional.ofNullable(
                    APILocator.getFolderAPI().find(hostFolderValue, user, false)
                );
            } catch (DotSecurityException | DotDataException e) {
                return Optional.empty();
            }
        } else  {
            return Optional.empty();
        }
    }

    private static Optional<Folder> resolveWithFieldVariable(final Field field, final User user) {

        try {
            List<FieldVariable> fieldVariables = APILocator.getFieldAPI()
                    .getFieldVariablesForField(field.id(), user, true);

            final Optional<FieldVariable> defaulPathVariable = fieldVariables.stream()
                    .filter(fieldVariable -> "defaultPath".equals(fieldVariable.getKey()))
                    .findFirst();

            if (defaulPathVariable.isPresent()) {
                final FieldVariable defaultPathVariable = defaulPathVariable.get();
                final String fieldVariableValue = defaultPathVariable.getValue();

                if (fieldVariableValue.startsWith(HOST_INDICATOR)) {
                    final String hostName = fieldVariableValue.substring(HOST_INDICATOR.length())
                            .split("/")[0];
                    final Host host = APILocator.getHostAPI()
                            .findByName(hostName, user, false);

                    if (!UtilMethods.isSet(host)) {
                        return Optional.empty();
                    }

                    final String folderPath = fieldVariableValue.substring(
                            HOST_INDICATOR.length() + hostName.length());

                    final Folder folderByPath = APILocator.getFolderAPI()
                            .findFolderByPath(folderPath, host.getIdentifier(),
                                    APILocator.systemUser(), false);
                    return UtilMethods.isSet(folderByPath) ?
                            Optional.of(folderByPath) : Optional.empty();
                } else {
                    final String currentHost = WebAPILocator.getHostWebAPI().getCurrentHost()
                            .getIdentifier();

                    final Folder folderByPath = APILocator.getFolderAPI()
                            .findFolderByPath(fieldVariableValue, currentHost,
                                    APILocator.systemUser(), false);

                    return UtilMethods.isSet(folderByPath) && UtilMethods.isSet(folderByPath.getIdentifier())
                        ? Optional.of(folderByPath) : Optional.empty();
                }
            } else {
                Logger.debug(BrowserUtil.class, () -> "defaultPath variable not exists for field " + field.name());
                return Optional.empty();
            }
        } catch (DotSecurityException | DotDataException e) {
            return Optional.empty();
        }
    }

    private static Optional<Folder> resolveWithCurrentValue(
            final Contentlet contentlet, final Field field) {
        final String value = WysiwygField.class.equals(field.type())
                ? getValueFromWysiwygField(contentlet, field)
                : contentlet.getStringProperty(field.variable());

        if (UtilMethods.isSet(value)) {
            final Identifier identifier;
            try {
                identifier = APILocator.getIdentifierAPI().find(value);

                return Optional.of(
                        APILocator.getFolderAPI()
                                .findFolderByPath(identifier.getParentPath(), identifier.getHostId(), APILocator.systemUser(), false)
                );
            } catch (Exception e) {
                Logger.debug(BrowserUtil.class,e.getMessage(),e);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private static String getValueFromWysiwygField(final Contentlet contentlet, final Field field) {
        if (!WysiwygField.class.equals(field.type()) ) {
            return null;
        }

        final String value = contentlet.getStringProperty(field.variable());

        if (UtilMethods.isSet(value) && value.contains("<img")) {
            final String[] valueSplit = value.split(StringPool.SPACE);

            final List<String> atributes = Arrays.stream(valueSplit)
                    .filter(split -> split.startsWith("data-identifier"))
                    .collect(Collectors.toList());

            final String dataIdentifier = atributes.isEmpty() ? StringPool.BLANK : atributes.get(0);

            final String[] dataIdentifierSplit = dataIdentifier.split("=");
            return dataIdentifierSplit.length > 1
                    ? dataIdentifierSplit[1].replace("\"", StringPool.BLANK)
                    : null;
        } else {
            return null;
        }
    }

    private BrowserUtil(){}

    /**
     * Return tru if a Psth is absolute.
     * 
     * A absolute path has the follow sintax:
     * 
     * <code>//{host_name}/parent_folder_1/parent_folder_2/.../</code>
     *
     * for example:
     *
     * <code>
     * //demo.dotcms.com/folder_1/folder_2 //absolute path
     * /folder_1/folder_2 //relative path is should be resolve using the current host
     * </code>
     * 
     * @param path
     * @return
     */
    public static boolean isFullPath(final String path) {
        return path != null && path.startsWith(HOST_INDICATOR);
    }

    /**
     * Return a full path from a Host name and a relative path
     * 
     * @param hostName
     * @param relativePath
     * @return
     */
    public static String getFullPath(final String hostName, final String relativePath) {
        return builder(hostName, HOST_INDICATOR, relativePath).toString();
    }

    /**
     * Return the folder's absolute path
     * 
     * @param folder
     * @return
     * 
     * @see {@link BrowserUtil#isFullPath(String)}
     */
    public static String getFullPath(final Folder folder) {
        return builder(
                folder.getHost().getHostname(),
                HOST_INDICATOR,
                folder.getPath()
        ).toString();
    }

    /**
     * Return the current selected folder in the Edit contentlet File Browser
     *
     * @param httpServletRequest
     * @param folderId
     * @throws NotFoundInDbException
     */
    public static void setSelectedLastFolder(
            HttpServletRequest httpServletRequest, String folderId) throws NotFoundInDbException {

        try {
            final Folder folder = APILocator.getFolderAPI()
                    .find(folderId, APILocator.systemUser(), false);

            if (UtilMethods.isSet(folder)) {
                final HttpSession session = httpServletRequest.getSession(false);
                session.setAttribute(LAST_SELECTED_FOLDER_ID, folderId);
            } else {
                throw  new NotFoundInDbException("Folder not found :" + folderId);
            }
        } catch (DotSecurityException | DotDataException e) {
            throw new NotFoundInDbException("Folder not found :" + folderId, e);
        }
    }

    /**
     * Function to resolve the Contentlet's defaultPath
     * 
     * @see {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     */
    @FunctionalInterface
    interface DefaultPathResolver {
        Optional<Folder> resolve(final Contentlet contentlet, final Field field, final User user);
    }

    /**
     * Return the <pre>defaultPath</pre>.
     *
     * it is calculated by the follow steps
     *
     * - If {@code contentlet} has a value for {@code field} and it is a File or Image then this value
     * is the defaultPath.
     * - If {@code field} has defaultPath {@link FieldVariable} then the {@link FieldVariable}'s value is
     * the defaultPath..
     * - If {@code contentlet} has a site or folder field, the value of this field is the defaultPath value.
     * - If nothing, then default to the last selected folder, {@link BrowserUtil#setSelectedLastFolder(HttpServletRequest, String)}.
     * - If that is not set, then teh defaultPath is the current Host
     *
     * @param contentlet
     * @param field
     * @param user to check permission
     *
     * @return
     */
    public static Optional<Folder> getDefaultPathFolder(
            final Contentlet contentlet, final Field field, final User user) {

        for (final DefaultPathResolver pathResolver : defaultPathResolver) {
            final Optional<Folder> defaultPathFolder = pathResolver.resolve(contentlet, field, user);

            if (defaultPathFolder.isPresent()) {
                return Optional.of(defaultPathFolder.get());
            }
        }

        return Optional.empty();
    }

    private static FolderPath getFolderPathIds(final Folder folder){
        final List<Folder> folders = new ArrayList<>();
        final Host host = folder.getHost();

        folders.add(folder);

        try {
            Object currentParent = folder.getParentPermissionable();

            while (Folder.class.isInstance(currentParent)) {
                folders.add((Folder) currentParent);
                currentParent = ((Folder) currentParent).getParentPermissionable();
            }

            Collections.reverse(folders);
        } catch (DotDataException e) {
            folders.clear();
        }

        return new FolderPath(folders, host);
    }

    public static List<String> getDefaultPathFolderPathIds(
            final Contentlet contentlet, final Field field, final User user){
        final Optional<Folder> defaultPathFolderOptional = getDefaultPathFolder(contentlet, field, user);

        try {
            final FolderPath folderPathIds = defaultPathFolderOptional.isPresent() ?
                    getFolderPathIds(defaultPathFolderOptional.get()) :
                    new FolderPath(Collections.emptyList(), getCurrentHost(user));

            return folderPathIds.getIds();

        } catch (DotDataException | DotSecurityException e) {
            return list(APILocator.systemHost().getIdentifier());
        }

    }

    /**
     * It contains all the {@link Folder} in a Folder's path
     */
    private static class FolderPath {
        private List<Folder> pathFolders;
        private Host host;

        public FolderPath(List<Folder> pathFolders, Host host) {
            this.pathFolders = pathFolders;
            this.host = host;
        }

        /**
         * Return a {@link List} with all the ids in a Folder's path starting in the host's id
         * 
         * For example:
         * If we have the follow folder path: <pre>//default/folder_1/folder_2/folder_3</pre>
         * then this method is going to return a list with:
         * [default host id, folder_1's id, folder_2's id, folder_3's id]
         * 
         * @return
         */
        public List<String> getIds(){
            final List<String> ids = pathFolders.stream()
                    .filter(folder -> !folder.isSystemFolder())
                    .map(folder -> folder.getInode())
                    .collect(Collectors.toList());

            ids.add(0, host.getIdentifier());
            return ids;
        }
    }
}
