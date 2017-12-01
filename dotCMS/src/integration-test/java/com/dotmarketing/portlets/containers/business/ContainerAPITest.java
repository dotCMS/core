package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ContainerAPITest extends ContentletBaseTest {


    @Test
    public void save() throws Exception {
    	HibernateUtil.startTransaction();
        Container c = new Container();
        c.setFriendlyName("test container");
        c.setTitle("this is the title");
        c.setMaxContentlets(5);
        c.setPreLoop("preloop code");
        c.setPostLoop("postloop code");

        Container cc = new Container();
        BeanUtils.copyProperties(cc, c);

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(contentTypeAPI.find("host").inode());
        cs.setCode("this is the code");
        csList.add(cs);

        cc = containerAPI.save(cc, csList, defaultHost, user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        cc = containerAPI.getWorkingContainerById(cc.getIdentifier(), user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        List<ContainerStructure> csListCopy = containerAPI.getContainerStructures(cc);
        ContainerStructure csCopy = csListCopy.get(0);
        assertTrue(csCopy.getCode().equals(cs.getCode()));
        assertTrue(cc.getFriendlyName().equals(c.getFriendlyName()));
        assertTrue(cc.getTitle().equals(c.getTitle()));
        assertTrue(cc.getMaxContentlets()==c.getMaxContentlets());
        assertTrue(cc.getPreLoop().equals(c.getPreLoop()));
        assertTrue(cc.getPostLoop().equals(c.getPostLoop()));
        HibernateUtil.closeAndCommitTransaction();
    }

    @Test
    public void saveWithExistingIds() throws Exception {
        Container c = new Container();
        c.setFriendlyName("test container for existing inode/identifier");
        c.setTitle("this is the title for existing inode/identifier");
        c.setMaxContentlets(5);
        c.setPreLoop("preloop code");
        c.setPostLoop("postloop code");

        // here comes the existing inode/identifier
        String existingInode=UUIDGenerator.generateUuid();
        String existingIdentifier=UUIDGenerator.generateUuid();
        c.setInode(existingInode);
        c.setIdentifier(existingIdentifier);

        Container cc = new Container();
        BeanUtils.copyProperties(cc, c);

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(contentTypeAPI.find("host").inode());
        cs.setCode("this is the code");
        csList.add(cs);

        cc = containerAPI.save(cc, csList, defaultHost, user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        cc = containerAPI.getWorkingContainerById(cc.getIdentifier(), user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        // existing inode/identifier should match
        assertEquals(cc.getInode(),existingInode);
        assertEquals(cc.getIdentifier(), existingIdentifier);

        assertEquals(cc.getCode(),c.getCode());
        assertEquals(cc.getFriendlyName(),c.getFriendlyName());
        assertEquals(cc.getTitle(),c.getTitle());
        assertEquals(cc.getMaxContentlets(),c.getMaxContentlets());
        assertEquals(cc.getPreLoop(),c.getPreLoop());
        assertEquals(cc.getPostLoop(),c.getPostLoop());

        // now an update with existing inode
        String newInode=UUIDGenerator.generateUuid();
        cc.setPreLoop("new preloop");
        cc.setInode(newInode);
        cc = containerAPI.save(cc, csList, defaultHost, user, false);
        assertEquals(newInode, cc.getInode());
        assertEquals(existingIdentifier, cc.getIdentifier());
        cc = containerAPI.getWorkingContainerById(cc.getIdentifier(), user, false);
        assertEquals(newInode, cc.getInode());
        assertEquals(existingIdentifier, cc.getIdentifier());
        assertEquals(cc.getPreLoop(),"new preloop");
    }

    @Test
    public void delete() throws Exception {
        Container container = LocalTransaction.wrapReturnWithListeners(() -> {
            final Container saved = createContainer();

            if (containerAPI.delete(saved, user, false))
                return saved;
            else
                throw new DotDataException("An error occurred deleting container");
        });

        AssetUtil.assertDeleted(container.getInode(), container.getIdentifier(), Inode.Type.CONTAINERS.getValue());
    }

    @Test
    public void testCopy() throws Exception {
        LocalTransaction.wrap(() -> {
            try {
                copyContainer();
            } catch (DotSecurityException e) {
                throw new DotDataException(e);
            }
        });
    }

    private void copyContainer() throws DotSecurityException, DotDataException {
        final Container source = createContainer();

        final Container target = containerAPI.copy(source, defaultHost, user, false);

        assertNotNull(target);
        assertNotNull(target.getTitle());
        assertNotNull(target.getInode());
        assertTrue(target.getTitle().contains(source.getTitle()));
        assertNotEquals(source.getInode(), target.getInode());
        containerAPI.delete(source, user, false);
        containerAPI.delete(target, user, false);
    }

    @Test
    public void testFindContainersUnder() throws DotDataException {
        final List<Container> results = containerAPI.findContainersUnder(defaultHost);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    public void testFindAllContainers() throws DotDataException, DotSecurityException {
        final List<Container> results = containerAPI.findAllContainers(user, false);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    public void testFindContainers() throws DotDataException, DotSecurityException {
        final List<Container> results = containerAPI
                .findContainers(user, false, null, defaultHost.getIdentifier(), null, null, null, 0,
                        -1, null);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    private Container createContainer() throws DotSecurityException, DotDataException {
        Container container = new Container();
        container.setFriendlyName("test container");
        container.setTitle("this is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");
        container.setOwner("container's owner");

        final List<ContainerStructure> csList = new ArrayList<>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(contentTypeAPI.find("host").inode());
        cs.setCode("this is the code");
        csList.add(cs);
        return containerAPI.save(container, csList, defaultHost, user, false);
    }
}
