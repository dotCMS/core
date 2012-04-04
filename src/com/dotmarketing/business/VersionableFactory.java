package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.ContentletLangVersionInfo;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;

/**
 * 
 * @author will
 * 
 */
public abstract class VersionableFactory {

	protected abstract Versionable findWorkingVersion(String id) throws DotDataException, DotStateException;

	protected abstract Versionable findLiveVersion(String id) throws DotDataException, DotStateException;

	protected abstract Versionable findDeletedVersion(String id) throws DotDataException, DotStateException;

	protected abstract List<Versionable> findAllVersions(String id) throws DotDataException, DotStateException;

	protected abstract void saveVersionInfo(VersionInfo info) throws DotDataException, DotStateException;

	protected abstract ContentletLangVersionInfo getContentletLangVersionInfo(String identifier, long lang) throws DotDataException,
			DotStateException;

	protected abstract VersionInfo getVersionInfo(String identifier) throws DotDataException, DotStateException;

	protected abstract ContentletVersionInfo getContentletVersionInfo(String identifier) throws DotDataException, DotStateException;

	protected abstract void saveContentletVersionInfo(ContentletVersionInfo cvInfo) throws DotDataException, DotStateException;

	protected abstract void saveContentletLangVersionInfo(ContentletLangVersionInfo contL) throws DotDataException, DotStateException;

	protected abstract VersionInfo createVersionInfo(Identifier identifier, String workingInode) throws DotStateException, DotDataException;

	protected abstract ContentletLangVersionInfo createContentletLangVersionInfo(Identifier identifier, String workingInode, long lang)
			throws DotStateException, DotDataException;

	protected abstract ContentletVersionInfo createContentletVersionInfo(Identifier identifier) throws DotStateException, DotDataException;
	
	protected abstract void deleteVersionInfo(String identifier) throws DotDataException;

    protected abstract void deleteContentletVersionInfo(String id, long lang) throws DotDataException;

}
