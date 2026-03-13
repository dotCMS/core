package com.dotcms.rest.api.v1.folder;

import com.dotmarketing.beans.Host;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the host-type metadata added to {@link FolderView}.
 *
 * <p>These tests verify that {@link FolderView} correctly distinguishes nested-host nodes
 * ({@code isHost == true}) from regular folder nodes ({@code isHost == false}), and that all
 * host-related fields are populated as expected from the constructor.</p>
 *
 * <p>No dotCMS infrastructure is required — only plain Java.</p>
 */
public class FolderViewHostNodeTest {

    // -------------------------------------------------------------------------
    // Folder constructor — isHost must be false
    // -------------------------------------------------------------------------

    @Test
    public void folderConstructor_isHostFalse() {
        final com.dotmarketing.portlets.folders.model.Folder folder =
                buildFolder("folder-id-1", "myFolder", "host-1");
        final FolderView view = new FolderView(folder, Collections.emptyList());

        assertFalse("Regular folder node must have isHost==false", view.isHost());
        assertNull("Regular folder node must have null hostname", view.getHostname());
    }

    @Test
    public void folderConstructor_preservesSubFolders() {
        final com.dotmarketing.portlets.folders.model.Folder parent =
                buildFolder("parent-id", "parent", "host-1");
        final com.dotmarketing.portlets.folders.model.Folder child =
                buildFolder("child-id", "child", "host-1");
        final FolderView childView = new FolderView(child, Collections.emptyList());

        final FolderView parentView = new FolderView(parent, List.of(childView));

        assertEquals(1, parentView.getSubFolders().size());
        assertFalse(parentView.getSubFolders().get(0).isHost());
    }

    // -------------------------------------------------------------------------
    // Host constructor — isHost must be true, hostname must be set
    // -------------------------------------------------------------------------

    @Test
    public void hostConstructor_isHostTrue() {
        final Host nestedHost = buildHost("nested-id", "nested.example.com");
        final FolderView view = new FolderView(nestedHost, "parent-host-id", "/", Collections.emptyList());

        assertTrue("Nested host node must have isHost==true", view.isHost());
    }

    @Test
    public void hostConstructor_hostnameEqualsHostname() {
        final Host nestedHost = buildHost("nested-id", "nested.example.com");
        final FolderView view = new FolderView(nestedHost, "parent-host-id", "/", Collections.emptyList());

        assertEquals("nested.example.com", view.getHostname());
    }

    @Test
    public void hostConstructor_nameEqualsHostname() {
        final Host nestedHost = buildHost("nested-id", "nested.example.com");
        final FolderView view = new FolderView(nestedHost, "parent-host-id", "/", Collections.emptyList());

        assertEquals("nested.example.com", view.getName());
    }

    @Test
    public void hostConstructor_identifierEqualsHostIdentifier() {
        final Host nestedHost = buildHost("nested-id", "nested.example.com");
        final FolderView view = new FolderView(nestedHost, "parent-host-id", "/", Collections.emptyList());

        assertEquals("nested-id", view.getIdentifier());
    }

    @Test
    public void hostConstructor_hostIdEqualsParentHostId() {
        final Host nestedHost = buildHost("nested-id", "nested.example.com");
        final FolderView view = new FolderView(nestedHost, "parent-host-id", "/", Collections.emptyList());

        assertEquals("parent-host-id", view.getHostId());
    }

    @Test
    public void hostConstructor_pathEqualsNodePath() {
        final Host nestedHost = buildHost("nested-id", "nested.example.com");
        final FolderView view = new FolderView(nestedHost, "parent-host-id", "/myFolder/", Collections.emptyList());

        assertEquals("/myFolder/", view.getPath());
    }

    @Test
    public void hostConstructor_typeEqualsHostVelocityVarName() {
        final Host nestedHost = buildHost("nested-id", "nested.example.com");
        final FolderView view = new FolderView(nestedHost, "parent-host-id", "/", Collections.emptyList());

        assertEquals(Host.HOST_VELOCITY_VAR_NAME, view.getType());
    }

    @Test
    public void hostConstructor_preservesChildSubFolders() {
        final Host nestedHost = buildHost("nested-id", "nested.example.com");
        final com.dotmarketing.portlets.folders.model.Folder childFolder =
                buildFolder("child-id", "childFolder", "nested-id");
        final FolderView childView = new FolderView(childFolder, Collections.emptyList());

        final FolderView hostView = new FolderView(nestedHost, "parent-host-id", "/", List.of(childView));

        assertEquals(1, hostView.getSubFolders().size());
        assertFalse("Child folder of a host node is still a plain folder", hostView.getSubFolders().get(0).isHost());
    }

    @Test
    public void hostConstructor_nestedSubHostPreservedWithIsHostTrue() {
        final Host parent = buildHost("parent-id", "parent.example.com");
        final Host child = buildHost("child-id", "child.example.com");

        final FolderView childHostView = new FolderView(child, "parent-id", "/", Collections.emptyList());
        final FolderView parentHostView = new FolderView(parent, "grandparent-id", "/", List.of(childHostView));

        assertEquals(1, parentHostView.getSubFolders().size());
        assertTrue("Sub-host node must have isHost==true",
                parentHostView.getSubFolders().get(0).isHost());
        assertEquals("child.example.com", parentHostView.getSubFolders().get(0).getHostname());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static com.dotmarketing.portlets.folders.model.Folder buildFolder(
            final String identifier, final String name, final String hostId) {
        final com.dotmarketing.portlets.folders.model.Folder f =
                new com.dotmarketing.portlets.folders.model.Folder();
        f.setIdentifier(identifier);
        f.setName(name);
        f.setHostId(hostId);
        f.setPath("/" + name + "/");
        f.setInode(identifier + "-inode");
        return f;
    }

    private static Host buildHost(final String identifier, final String hostname) {
        // Use Host(Contentlet) copy-constructor to avoid the CacheLocator dependency
        // present in the no-arg Host() constructor.
        final com.dotmarketing.portlets.contentlet.model.Contentlet c =
                new com.dotmarketing.portlets.contentlet.model.Contentlet();
        // Pre-populate the title key to avoid getTitle() falling through to CDI/container code.
        c.getMap().put(com.dotmarketing.portlets.contentlet.model.Contentlet.TITTLE_KEY, hostname);
        final Host h = new Host(c);
        h.setIdentifier(identifier);
        h.setHostname(hostname);
        h.setInode(identifier + "-inode");
        return h;
    }
}
