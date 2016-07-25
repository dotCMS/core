package com.dotmarketing.portlets.structure.business;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.FromStructureTransformer;
import com.dotcms.contenttype.transform.contenttype.ToContentTypeTransformer;
import com.dotcms.contenttype.transform.contenttype.ToStructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

/**
 * This API exposes useful methods to access and modify information related to
 * Content Types in dotCMS. Please note that the term Structure is deprecated,
 * and it's referred to as Content Type now.
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Feb 11, 2013
 *
 */
public class StructureAPIImpl implements StructureAPI {

	@Override
	public void delete(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException {
		APILocator.getContentTypeAPI2().delete(new FromStructureTransformer(st).from(), user);
	}
	
	@Override
	public void save(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException {
		APILocator.getContentTypeAPI2().saveContentType(new FromStructureTransformer(st).from(), new LegacyFieldTransformer(st.getFields()).asList(), user);

	}
	@Override
	public Structure find(String inode, User user) throws DotSecurityException, DotDataException, DotStateException {

		return new ToStructureTransformer(APILocator.getContentTypeAPI2().find(inode, user)).from();

	}

	@Override
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly) throws DotDataException {

		return new ToStructureTransformer(APILocator.getContentTypeAPI2().findAll(user, respectFrontendRoles)).asList();

	}

	@Override
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly, String condition, String orderBy,
			int limit, int offset, String direction) throws DotDataException {

		return new ToStructureTransformer(APILocator.getContentTypeAPI2().find(condition, orderBy, limit,
				offset, direction,user, respectFrontendRoles)).asList();

	}

	@Override
	public Structure findByVarName(String varName, User user) throws DotSecurityException, DotDataException {

		return new ToStructureTransformer(APILocator.getContentTypeAPI2().findByVarName(varName, user)).from();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dotmarketing.portlets.structure.business.StructureAPI#countStructures
	 * (java.lang.String)
	 */
	@Override
	public int countStructures(String condition) {
		try {
			return APILocator.getContentTypeAPI2().countContentType(condition);
		} catch (DotDataException dde) {
			throw new DotStateException(dde.getMessage(), dde);
		}
	}

}
