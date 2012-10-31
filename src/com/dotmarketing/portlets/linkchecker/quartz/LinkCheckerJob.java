package com.dotmarketing.portlets.linkchecker.quartz;

import java.text.DateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadSafeSimpleDateFormat;

public class LinkCheckerJob implements Job {
    
    private static DateFormat luceneDateFormat=new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss");
    
    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        Logger.info(this, "Checking for broken links");
        
        Set<Structure> structures=new HashSet<Structure>();
        
        // get structures with wysiwyg fields
        for(Structure st : StructureFactory.getStructures())
            for(Field field : FieldsCache.getFieldsByStructureInode(st.getInode()))
                if(field.getType().equals(Field.FieldType.WYSIWYG.toString()))
                    structures.add(st);
        
        StringBuilder query=new StringBuilder();
        
        if(ctx.getPreviousFireTime()!=null) {
            // what's changed since our last run (if any)
            query.append("+modDate:[")
                 .append(luceneDateFormat.format(ctx.getPreviousFireTime()))
                 .append(" TO ")
                 .append(luceneDateFormat.format(ctx.getFireTime()))
                 .append("] ");
        }
        else
            Logger.info(this, "First run! Checking for bad links on all content with wysiwyg fields");
        
        // adding all relevan structures
        query.append("+(");
        for(Structure st : structures)
            query.append(" +structureName:").append(st.getVelocityVarName()).append(' ');
        query.append(") ");
        
        List<Contentlet> contents=null;
        int offset=0;
        final int pageSize=100;
        int processed=0;
        int badlinks=0;
        do {
            try {
                contents=APILocator.getContentletAPI().search(
                        query.toString(), pageSize, offset, "modDate", 
                        APILocator.getUserAPI().getSystemUser(), false);
                processed+=contents.size();
                for(Contentlet con : contents) {
                    
                    // maybe was edited without creating a new version
                    // lets delete what we did before just in case
                    APILocator.getLinkCheckerAPI().deleteInvalidLinks(con);
                    
                    for(Field field : FieldsCache.getFieldsByStructureInode(con.getStructureInode())) {
                        if(field.getType().equals(Field.FieldType.WYSIWYG.toString())) {
                            // getting invalid links over wysiwyg fields
                            List<InvalidLink> links=APILocator.getLinkCheckerAPI().findInvalidLinks(
                                    con.getStringProperty(field.getVelocityVarName()));
                            if(links.size()>0) {
                                APILocator.getLinkCheckerAPI().saveInvalidLinks(con, field, links);
                                badlinks+=links.size();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
            offset+=pageSize;
        } while(contents.size()>0);
        Logger.info(this, "Finished checking for broken links. Processed "+processed+" contentlets. Found "+badlinks+" borken links");
    }
    
}
