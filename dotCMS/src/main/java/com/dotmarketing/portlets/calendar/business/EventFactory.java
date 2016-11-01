package com.dotmarketing.portlets.calendar.business;

import java.util.Date;
import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public abstract class EventFactory {

	abstract protected Event find(String id, boolean live, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	abstract protected Event findbyInode(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;

	abstract protected List<Event> find(Date fromDate, Date toDate, String[] tags, String[] keywords, List<Category> categories, boolean liveOnly, boolean archived, int offset,
			int limit, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	abstract protected List<Event> find(String hostId, Date fromDate, Date toDate, String[] tags, String[] keywords, List<Category> categories, boolean liveOnly, boolean archived, int offset,
			int limit, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	abstract protected Structure getBuildingStructure();

	abstract protected Structure getEventStructure();

	abstract protected Structure getLocationStructure();
	
	abstract protected Event convertToEvent (Contentlet cont) throws DotDataException, DotContentletStateException, DotSecurityException;

}
