package com.dotmarketing.startup.runonce;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.InodeUtils;

public class Task03100HTMLPageAsContentChanges implements StartupTask {

    @Override
    public boolean forceRun() {
        Structure st=StructureCache.getStructureByVelocityVarName(HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME);
        return st==null || !InodeUtils.isSet(st.getInode());
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Structure st=new Structure();
        st.setDefaultStructure(false);
        st.setDescription(HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_DESCRIPTION);
        st.setName(HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_NAME);
        st.setVelocityVarName(HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME);
        st.setStructureType(Structure.STRUCTURE_TYPE_HTMLPAGE);
        st.setFixed(true);
        st.setHost(Host.SYSTEM_HOST);
        st.setFolder(FolderAPI.SYSTEM_FOLDER);
        st.setOwner(APILocator.getUserAPI().getSystemUser().getUserId());
        
        StructureFactory.saveStructure(st, HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
        
        APILocator.getHTMLPageAssetAPI().createHTMLPageAssetBaseFields(st);
    }

}
