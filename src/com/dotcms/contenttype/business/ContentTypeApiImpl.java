package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;


public class ContentTypeApiImpl implements ContentTypeApi, StructureAPI{

	
	
	
	@Override
	public void delete(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Structure find(String inode, User user) throws DotSecurityException, DotDataException, DotStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly) throws DotDataException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly, String condition, String orderBy,
			int limit, int offset, String direction) throws DotDataException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Structure findByVarName(String varName, User user) throws DotSecurityException, DotDataException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int countStructures(String condition) {
		// TODO Auto-generated method stub
		return 0;
	}

	public ContentType getContentType(){
		String user = "test";
		try {
			user=APILocator.getUserAPI().getSystemUser().getUserId();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ContentType c = ImmutableSimpleContentType.builder()
				.name("My Content")
				.velocityVarName("test")
				.owner(user).build();
		return c;
		
	}
	public void saveContentType(ContentType type, List<Field> fields){
		
	}
}
