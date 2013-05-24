package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ContainerAPITest extends ContentletBaseTest {
    @Test
    public void save() throws Exception {
        Container c = new Container();
        c.setFriendlyName("test container");
        c.setTitle("his is the title");
        c.setMaxContentlets(5);
        c.setPreLoop("preloop code");
        c.setPostLoop("postloop code");

        Container cc = new Container();
        BeanUtils.copyProperties(cc, c);

        Structure st=StructureCache.getStructureByVelocityVarName("host");

        User user = APILocator.getUserAPI().getSystemUser();
        Host host = APILocator.getHostAPI().findDefaultHost(user, false);

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(st.getInode());
        cs.setCode("this is the code");
        csList.add(cs);

        cc = APILocator.getContainerAPI().save(cc, csList, host, user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        cc = APILocator.getContainerAPI().getWorkingContainerById(cc.getIdentifier(), user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        List<ContainerStructure> csListCopy = APILocator.getContainerAPI().getContainerStructures(cc);
        ContainerStructure csCopy = csListCopy.get(0);
        assertTrue(csCopy.getCode().equals(cs.getCode()));
        assertTrue(cc.getFriendlyName().equals(c.getFriendlyName()));
        assertTrue(cc.getTitle().equals(c.getTitle()));
        assertTrue(cc.getMaxContentlets()==c.getMaxContentlets());
        assertTrue(cc.getPreLoop().equals(c.getPreLoop()));
        assertTrue(cc.getPostLoop().equals(c.getPostLoop()));
    }

    @Test
    public void saveWithExistingIds() throws Exception {
        Container c = new Container();
        c.setFriendlyName("test container for existing inode/identifier");
        c.setTitle("his is the title for existing inode/identifier");
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

        Structure st=StructureCache.getStructureByVelocityVarName("host");

        User user = APILocator.getUserAPI().getSystemUser();
        Host host = APILocator.getHostAPI().findDefaultHost(user, false);

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(st.getInode());
        cs.setCode("this is the code");
        csList.add(cs);

        cc = APILocator.getContainerAPI().save(cc, csList, host, user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        cc = APILocator.getContainerAPI().getWorkingContainerById(cc.getIdentifier(), user, false);

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
        cc = APILocator.getContainerAPI().save(cc, csList, host, user, false);
        assertEquals(newInode, cc.getInode());
        assertEquals(existingIdentifier, cc.getIdentifier());
        cc = APILocator.getContainerAPI().getWorkingContainerById(cc.getIdentifier(), user, false);
        assertEquals(newInode, cc.getInode());
        assertEquals(existingIdentifier, cc.getIdentifier());
        assertEquals(cc.getPreLoop(),"new preloop");
    }

    @Test
    public void delete() throws Exception {
        Container container = new Container();
        container.setFriendlyName("test container");
        container.setTitle("his is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");

        Structure st=StructureCache.getStructureByVelocityVarName("host");

        User user = APILocator.getUserAPI().getSystemUser();
        Host host = APILocator.getHostAPI().findDefaultHost(user, false);

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(st.getInode());
        cs.setCode("this is the code");
        csList.add(cs);
        Container saved = APILocator.getContainerAPI().save(container, csList, host, user, false);

        String inode=saved.getInode();
        String identifier=saved.getIdentifier();

        assertTrue(APILocator.getContainerAPI().delete(saved, user, false));

        AssetUtil.assertDeleted(inode, identifier, "containers");
    }
}
