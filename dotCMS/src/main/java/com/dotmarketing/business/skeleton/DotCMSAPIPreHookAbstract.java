package com.dotmarketing.business.skeleton;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

public abstract class DotCMSAPIPreHookAbstract implements DotCMSAPIPreHook {

	public List<Inode> findAll(int offset, int limit) throws DotDataException {
		// TODO Auto-generated method stub
		return null;
	}

	public Inode findByInode(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	public Inode findByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> findByIdentifiers(String[] identifiers, boolean live, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> findByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> findByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	public Inode copy(Inode node, Inode folderOrHost, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> search(String condition, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> search(String condition, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,
			int requiredPermission) throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Map<String, Object>> getReferences(Inode node, User user, boolean respectFrontendRoles) throws DotSecurityException,
			DotDataException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isEqual(Inode node1, Inode node2, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		// TODO Auto-generated method stub
		return false;
	}

	public void archive(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub

	}

	public void delete(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub

	}

	public void delete(Inode node, User user, boolean respectFrontendRoles, boolean allVersions) throws DotDataException,
			DotSecurityException, DotStateException {
		// TODO Auto-generated method stub

	}

	public void publish(Inode node, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException,
			DotStateException, DotStateException, DotStateException {
		// TODO Auto-generated method stub

	}

	public void publish(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException,
			DotStateException, DotStateException {
		// TODO Auto-generated method stub

	}

	public void unpublish(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub

	}

	public void unpublish(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub

	}

	public void archive(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub

	}

	public void unarchive(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub

	}

	public void unarchive(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub

	}

	public void deleteAllVersionsandBackup(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException {
		// TODO Auto-generated method stub

	}

	public void delete(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub

	}

	public void delete(List<Inode> nodes, User user, boolean respectFrontendRoles, boolean allVersions) throws DotDataException,
			DotSecurityException, DotStateException {
		// TODO Auto-generated method stub

	}

	public void unlock(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub

	}

	public void lock(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotStateException {
		// TODO Auto-generated method stub

	}

	public Inode checkout(String nodeInode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> checkout(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> checkoutByCondition(String condition, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> checkoutByCondition(String condition, User user, boolean respectFrontendRoles, int offset, int limit)
			throws DotDataException, DotSecurityException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public Inode checkin(Inode node, List<Permission> permissions, User user, boolean respectFrontendRoles)
			throws IllegalArgumentException, DotDataException, DotSecurityException, DotStateException, DotValidationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Inode checkin(Inode node, User user, boolean respectFrontendRoles) throws IllegalArgumentException, DotDataException,
			DotSecurityException, DotStateException, DotValidationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Inode checkinWithoutVersioning(Inode node, List<Permission> permissions, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotStateException, DotValidationException {
		// TODO Auto-generated method stub
		return null;
	}

	public void restoreVersion(Inode node, User user, boolean respectFrontendRoles) throws DotSecurityException, DotStateException,
			DotDataException {
		// TODO Auto-generated method stub

	}

	public List<Inode> findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles) throws DotSecurityException,
			DotDataException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles) throws DotSecurityException,
			DotDataException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName(Inode node, User user, boolean respectFrontendRoles) throws DotSecurityException, DotStateException,
			DotDataException {
		// TODO Auto-generated method stub
		return null;
	}

	public Inode copyFromMap(Inode node, Map<String, Object> properties) throws DotStateException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	public void validate(Inode node) throws DotValidationException {
		// TODO Auto-generated method stub

	}

	public int deleteOld(Date deleteFrom, int offset) throws DotDataException {
		// TODO Auto-generated method stub
		return 0;
	}

	public long count() throws DotDataException {
		// TODO Auto-generated method stub
		return 0;
	}

	public long identifierCount() throws DotDataException {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles) throws ValidationException,
			DotDataException {
		// TODO Auto-generated method stub
		return null;
	}

	public void UpdateWithSystemHost(String hostIdentifier) throws DotDataException {
		// TODO Auto-generated method stub

	}

	public void removeUserReferences(String userId) throws DotDataException {
		// TODO Auto-generated method stub

	}

	public void deleteVersion(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub

	}

	public Inode saveDraft(Inode node, List<Permission> permissions, User user, boolean respectFrontendRoles)
			throws IllegalArgumentException, DotDataException, DotSecurityException, DotStateException, DotValidationException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> searchByIdentifier(String identifier, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Inode> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user,
			boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

}
