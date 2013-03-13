package com.dotmarketing.portlets.structure.business;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public interface StructureAPI {
    void delete(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException;
    
    
    Structure find(String inode, User user) throws DotSecurityException, DotDataException, DotStateException;
}
