package com.dotmarketing.factories;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;

public class MultiTreeAPIImpl implements MultiTreeAPI {

    VersionableAPI vers = APILocator.getVersionableAPI();
    ContentletAPI conAPI = APILocator.getContentletAPI();
    User sys = APILocator.systemUser();


    public void saveMultiTrees(String pageId, List<MultiTree> mTrees) throws DotDataException {
        MultiTreeFactory.saveMultiTrees(pageId, mTrees);
    }

    public void saveMultiTree(MultiTree multiTree) throws DotDataException {
        MultiTreeFactory.saveMultiTree(multiTree);
    }

    public Table<String, String, Set<String>>  getPageMultiTrees(final IHTMLPage page, final boolean liveMode) throws DotDataException, DotSecurityException {
        
        Table<String, String, Set<String>> pageContents = HashBasedTable.create();
        List<MultiTree> multis = MultiTreeFactory.getMultiTrees(page.getIdentifier());
        for (MultiTree t : multis) {
            Container container = (liveMode) ? (Container) vers.findLiveVersion(t.getContainer(), sys, false)
                    : (Container) vers.findWorkingVersion(t.getContainer(), sys, false);
            if(container==null && ! liveMode) {
               // MultiTreeFactory.deleteMultiTree(t);
                continue;
            }
            Contentlet contentlet = null;
            try {
                contentlet = conAPI.findContentletByIdentifier(t.getContentlet(), liveMode, -1, sys, false);
            }catch(Exception e){
                Logger.warn(this.getClass(), "invalid contentlet on multitree:" + t);
            }
            if(contentlet==null ) {
                // MultiTreeFactory.deleteMultiTree(t);
                 continue;
             };
            Set<String> myContents = pageContents.contains(t.getContainer(), t.getRelationType())
                    ? pageContents.get(t.getContainer(), t.getRelationType())
                    : new LinkedHashSet<>();
                    if(myContents.size()< container.getMaxContentlets()) {
                        myContents.add(t.getContentlet());
                    }
                    else {
                       // MultiTreeFactory.deleteMultiTree(t);
                    }

            pageContents.put(t.getContainer(), t.getRelationType(), myContents);

        }
        
        return pageContents;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
}
