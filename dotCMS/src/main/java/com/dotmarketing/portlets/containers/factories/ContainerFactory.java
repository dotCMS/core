package com.dotmarketing.portlets.containers.factories;

import com.dotcms.rendering.velocity.services.ContainerLoader;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;

import java.util.LinkedList;
import java.util.List;
/**
 *
 * @author  will
 */
public class ContainerFactory {

      public static java.util.List getActiveContainers() {
        HibernateUtil dh = new HibernateUtil(Container.class);
        List<Container> activeContainers = null;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.containers.model.Container where type='containers'");
			activeContainers = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}
        return activeContainers;
    }

     public static java.util.List getContainersByOrder(String orderby) {
        HibernateUtil dh = new HibernateUtil(Container.class);
        List<Container> containersByOrder = null ;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.containers.model.Container where type='containers' and working = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " or live = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " order by " + orderby);
			containersByOrder = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}

        return containersByOrder;
    }

    public static java.util.List getContainerByCondition(String condition) {
		HibernateUtil dh = new HibernateUtil(Container.class);
		List<Container> containers = null ;
		try {
			dh.setQuery("from inode in class  com.dotmarketing.portlets.containers.model.Container where type='containers' and " + condition + " order by title, sort_order");
			containers = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}
		return containers;
	}

    public static boolean existsContainer(String friendlyName) {
        HibernateUtil dh = new HibernateUtil(Container.class);
        List<Container> list = null ;
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.containers.model.Container where type='containers' and friendly_name = ?");
			dh.setParam(friendlyName);
			list = (java.util.List) dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}
        return list.size()>0;
    }

    public static Container getContainerByFriendlyName(String friendlyName) {
        HibernateUtil dh = new HibernateUtil(Container.class);
        Container container = null ;
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.containers.model.Container where type='containers' and friendly_name = ? and live=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue());
			dh.setParam(friendlyName);
			container = (Container) dh.load();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}
        return container;
    }

    public static Container copyContainer (Container currentContainer) throws DotDataException, DotStateException, DotSecurityException {

    	HostAPI hostAPI = APILocator.getHostAPI();

		//gets the new information for the template from the request object
		Container newContainer = new Container();

		newContainer.copy(currentContainer);
       	newContainer.setFriendlyName(currentContainer.getFriendlyName()
				+ " (COPY) ");
       	newContainer.setTitle(currentContainer.getTitle() + " (COPY) ");

        //Copy the structure
//        Structure st = CacheLocator.getContentTypeCache().getStructureByInode(currentContainer.getStructureInode());
//        newContainer.setStructureInode(st.getInode());



		//persists the webasset
		HibernateUtil.saveOrUpdate(newContainer);


		//Copy the host
		Host h;
		try {
			h = hostAPI.findParentHost(currentContainer, APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotSecurityException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
        //TreeFactory.saveTree(new Tree(h.getIdentifier(), newContainer.getInode()));

        //creates new identifier for this webasset and persists it
		Identifier newIdentifier = APILocator.getIdentifierAPI().createNew(newContainer, h);

		// save identifier id
		HibernateUtil.saveOrUpdate(newContainer);

		APILocator.getVersionableAPI().setWorking(newContainer);
		if(currentContainer.isLive())
		    APILocator.getVersionableAPI().setLive(newContainer);

		PermissionAPI perAPI = APILocator.getPermissionAPI();
		//Copy permissions
		perAPI.copyPermissions(currentContainer, newContainer);

		//saves to working folder under velocity
		new ContainerLoader().invalidate(newContainer);

		// issue-2093 Copying multiple structures per container
		if(currentContainer.getMaxContentlets()>0) {

			List<ContainerStructure> sourceCS = APILocator.getContainerAPI().getContainerStructures(currentContainer);
			List<ContainerStructure> newContainerCS = new LinkedList<ContainerStructure>();

			for (ContainerStructure oldCS : sourceCS) {
				ContainerStructure newCS = new ContainerStructure();
				newCS.setContainerId(newContainer.getIdentifier());
                newCS.setContainerInode(newContainer.getInode());
				newCS.setStructureId(oldCS.getStructureId());
				newCS.setCode(oldCS.getCode());
				newContainerCS.add(newCS);
			}

			APILocator.getContainerAPI().saveContainerStructures(newContainerCS);

		}

		return newContainer;
    }

}
