package com.dotmarketing.beans;

import com.dotcms.api.tree.Parentable;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import io.vavr.control.Try;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * This is just a wrapper class over a contentlet, it just offers nice methods to access host content specific fields like the host name but
 * it underneath is just a piece of content
 *
 * @author David H Torres
 */
public class Host extends Contentlet implements Permissionable,Treeable,Parentable {

    public static final String SYSTEM_HOST_SITENAME = "System Host";
    /**
     *
     */
	private static final long serialVersionUID = 1L;

	public Host() {
		map.put(SYSTEM_HOST_KEY, false);
		Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
		this.map.put(STRUCTURE_INODE_KEY, st.getInode());
		setDefault(false);
		setSystemHost(false);
	}

	public Host(Contentlet c) {
		super();
		this.map = c.getMap();
	}

	public static final String HOST_NAME_KEY = "hostName";

	public static final String IS_DEFAULT_KEY = "isDefault";

	public static final String ALIASES_KEY = "aliases";

	public static final String SYSTEM_HOST_KEY = "isSystemHost";

	public static final String HOST_THUMB_KEY = "hostThumbnail";

	public static final String SYSTEM_HOST = "SYSTEM_HOST";

	public static final String SYSTEM_HOST_NAME = com.dotmarketing.util.StringUtils.camelCaseLower(Host.SYSTEM_HOST);

	public static final String TAG_STORAGE = "tagStorage";

    public static final String HOST_VELOCITY_VAR_NAME = "Host";

    public static final String EMBEDDED_DASHBOARD = "embeddedDashboard";

    /**
     * Velocity variable name for the {@code parentHost} {@link com.dotcms.contenttype.model.field.HostFolderField}
     * that enables nestable hosts. A blank/null value means the host is a top-level host.
     */
    public static final String PARENT_HOST_KEY = "parentHost";

	@Override
	public String getInode() {
		return super.getInode();
	}

	@Override
	public String getName() {
		return getTitle();
	}

	@Override
	public boolean isParent() {
		return true;
	}

	@Override
	public List<Treeable> getChildren(User user, boolean live, boolean working, boolean archived, boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {
		return APILocator.getTreeableAPI().loadAssetsUnderHost(this,user,live,working, archived, respectFrontEndPermissions);
	}

	@JsonIgnore
	public String getVersionType() {
		return new String("host");
	}

	public String getAliases() {
		return (String) map.get(ALIASES_KEY);
	}

	public void setAliases(String aliases) {
		map.put(ALIASES_KEY, aliases);
	}

	public String getHostname() {
		return (String) map.get(HOST_NAME_KEY);
	}

	public void setHostname(String hostname) {
		map.put(HOST_NAME_KEY, hostname);
	}

	public File getHostThumbnail() {
		return (File) map.get(HOST_THUMB_KEY);
	}

	public void setHostThumbnail(File thumbnailInode) {
		map.put(HOST_THUMB_KEY, thumbnailInode);
	}

	public boolean isDefault() {
		return (Boolean) map.getOrDefault(IS_DEFAULT_KEY, Boolean.FALSE);
	}

	public void setDefault(boolean isDefault) {
		map.put(IS_DEFAULT_KEY, isDefault);
	}

	public String getStructureInode() {
		Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
		return (String) st.getInode();
	}



	public boolean isSystemHost() {
		Object isSystemHost = map.get(SYSTEM_HOST_KEY);
		if(isSystemHost!=null) {
			if (isSystemHost instanceof Boolean) {
				return (Boolean) isSystemHost;
			}
			return Integer.parseInt(isSystemHost.toString()) == 1 ? true
					: false;
		}
		return false;
	}

	public void setSystemHost(boolean isSystemHost) {
		map.put(SYSTEM_HOST_KEY, isSystemHost);
	}

	public void setStructureInode(String structureInode) {
		// No structure inode can be set different then the host structure inode
		// set by the constructor
	}

	@JsonIgnore
	public Map<String, Object> getMap() {
		Map<String, Object> hostMap = super.getMap();
		// Legacy property referenced as 'hostname' while really is 'hostName'
		hostMap.put("hostname", hostMap.get("hostName"));
		hostMap.put("type", "host");

		return hostMap;
	}

	/**
	 * @author David H Torres
	 */
	@Override
	@JsonIgnore
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("add-children", "add-children-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

	@JsonIgnore
	@Override
	public Permissionable getParentPermissionable() throws DotDataException {
		if (this.isSystemHost())
			return null;
		try {
			// For nested hosts, the Identifier.hostId points to the parent host.
			// If it is not the System Host, return that parent host so that
			// permissions propagate through the host hierarchy.
			final String myId = this.getIdentifier();
			if (UtilMethods.isSet(myId)) {
				final Identifier identifier = APILocator.getIdentifierAPI().find(myId);
				if (identifier != null
						&& UtilMethods.isSet(identifier.getHostId())
						&& !Host.SYSTEM_HOST.equals(identifier.getHostId())) {
					final Host parentHost = APILocator.getHostAPI()
							.find(identifier.getHostId(), APILocator.systemUser(), false);
					if (parentHost != null && UtilMethods.isSet(parentHost.getIdentifier())) {
						return parentHost;
					}
				}
			}
			return APILocator.getHostAPI().findSystemHost();
		} catch (final DotSecurityException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	public String getTagStorage() {
		Host host = Try.of(()->
				APILocator.getHostAPI().find(map.get(TAG_STORAGE).toString(), APILocator.systemUser(), false)).getOrNull();

		if(UtilMethods.isSet(()->host.getIdentifier())) {
			return host.getIdentifier();
		}

		return Host.SYSTEM_HOST;
	}

	public void setTagStorage(String tagStorageId) {
		map.put(TAG_STORAGE, tagStorageId);
	}

    /**
     * Returns the identifier of the parent host or folder for this host, or {@code null} /
     * empty string when this is a top-level host.
     *
     * @return parent host/folder identifier, or {@code null}
     */
    public String getParentHost() {
        final Object val = map.get(PARENT_HOST_KEY);
        return val != null ? val.toString() : null;
    }

    /**
     * Sets the parent host or folder identifier for this host.  Pass {@code null} or an empty
     * string to make this a top-level host.
     *
     * @param parentHostId identifier of the parent host or folder, or {@code null}
     */
    public void setParentHost(final String parentHostId) {
        map.put(PARENT_HOST_KEY, parentHostId);
    }

    /**
     * Returns the fully-qualified absolute base URL for this host.
     *
     * <p>For a <strong>top-level</strong> host (one whose {@code Identifier.hostId} equals
     * {@link #SYSTEM_HOST}), this returns {@code "https://" + getHostname()}.
     *
     * <p>For a <strong>nested</strong> host the method climbs the ancestor chain via the
     * {@code Identifier.hostId} links until it reaches the top-level host, accumulating the
     * path segment contributed by each intermediate host along the way. The top-level host
     * provides the domain name; every nested level prepends its folder path (from
     * {@code Identifier.parentPath}) and its own hostname to the path.
     *
     * <p>Cycle detection: if the same identifier UUID is encountered twice during the traversal
     * a {@link DotRuntimeException} is thrown to prevent an infinite loop caused by corrupted
     * database state.
     *
     * <p>Examples:
     * <pre>
     *   Top-level host (dotcms.com)
     *     → https://dotcms.com
     *
     *   nestedHost directly under dotcms.com  (parentPath = /)
     *     → https://dotcms.com/nestedHost
     *
     *   nestedHost2 under folder /en/ in dotcms.com  (parentPath = /en/)
     *     → https://dotcms.com/en/nestedHost2
     *
     *   nestedHost2 under nestedHost1 under dotcms.com  (each parentPath = /)
     *     → https://dotcms.com/nestedHost1/nestedHost2
     * </pre>
     *
     * @return fully-qualified URL prefix for this host, never {@code null}
     * @throws DotRuntimeException if a data-access error occurs or a cycle is detected in the
     *                             ancestor chain
     */
    @JsonIgnore
    public String getAbsoluteBaseUrl() {
        if (isSystemHost()) {
            return "";
        }

        final String myId = this.getIdentifier();
        if (!UtilMethods.isSet(myId)) {
            return "https://" + getHostname();
        }

        try {
            final Identifier myIdentifier = APILocator.getIdentifierAPI().find(myId);
            if (myIdentifier == null || !UtilMethods.isSet(myIdentifier.getId())) {
                return "https://" + getHostname();
            }

            if (!UtilMethods.isSet(myIdentifier.getHostId())
                    || Host.SYSTEM_HOST.equals(myIdentifier.getHostId())) {
                // This is already a top-level host.
                return "https://" + getHostname();
            }

            // Nested host: traverse the ancestor chain, collecting the path segment that
            // each level contributes.  We walk from THIS host upward; for each nested
            // level we prepend (parentPath-without-leading-slash + hostname + "/") to the
            // deque.  When we arrive at the top-level host we consume the deque to build
            // the final URL.
            final Deque<String> pathSegments = new ArrayDeque<>();
            final Set<String> visited = new HashSet<>();

            Host current = this;
            Identifier currentIdent = myIdentifier;

            while (true) {
                final String currentHostId = current.getIdentifier();
                if (!visited.add(currentHostId)) {
                    throw new DotRuntimeException(
                            "Cycle detected in host ancestor chain at host '"
                                    + current.getHostname() + "' (id=" + currentHostId + ")");
                }

                final String parentId = currentIdent.getHostId();
                if (!UtilMethods.isSet(parentId) || Host.SYSTEM_HOST.equals(parentId)) {
                    // 'current' is the top-level host — use its hostname as the domain.
                    final StringBuilder url = new StringBuilder("https://")
                            .append(current.getHostname());
                    if (!pathSegments.isEmpty()) {
                        url.append('/');
                        for (final String seg : pathSegments) {
                            url.append(seg);
                        }
                        // Remove the trailing slash added by the innermost segment.
                        if (url.charAt(url.length() - 1) == '/') {
                            url.setLength(url.length() - 1);
                        }
                    }
                    return url.toString();
                }

                // Accumulate this host's contribution: strippedParentPath + hostname + "/"
                final String pPath = currentIdent.getParentPath();
                final String stripped = (pPath != null && pPath.startsWith("/"))
                        ? pPath.substring(1)
                        : (pPath != null ? pPath : "");
                pathSegments.addFirst(stripped + current.getHostname() + "/");

                // Move up to the direct parent host.
                final Host parentHost = APILocator.getHostAPI()
                        .find(parentId, APILocator.systemUser(), false);
                if (parentHost == null || !UtilMethods.isSet(parentHost.getIdentifier())) {
                    throw new DotRuntimeException(
                            "Cannot resolve parent host for id '" + parentId
                                    + "' while building absolute base URL for host '"
                                    + getHostname() + "'");
                }
                final Identifier parentIdent =
                        APILocator.getIdentifierAPI().find(parentHost.getIdentifier());
                if (parentIdent == null || !UtilMethods.isSet(parentIdent.getId())) {
                    throw new DotRuntimeException(
                            "Cannot load identifier for parent host '"
                                    + parentHost.getHostname() + "' (id=" + parentId + ")");
                }
                current = parentHost;
                currentIdent = parentIdent;
            }

        } catch (final DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(
                    "Failed to compute absolute base URL for host '" + getHostname() + "': "
                            + e.getMessage(), e);
        }
    }

	@Override
	public String toString() {
		return this.getHostname();
	}
}
