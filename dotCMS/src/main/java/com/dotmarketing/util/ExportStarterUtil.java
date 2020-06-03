package com.dotmarketing.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Clickstream404;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionReference;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.calendar.model.CalendarReminder;
import com.dotmarketing.portlets.cmsmaintenance.util.AssetFileNameFilter;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.util.RulesImportExportUtil;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.ejb.ImageLocalManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class ExportStarterUtil {

    

    final File outputDirectory;
    
    public ExportStarterUtil() {
        
        outputDirectory = new File(ConfigUtils.getBackupPath() + File.separator + "backup_" + System.currentTimeMillis());
        outputDirectory.mkdirs();
       
        
    }
    
    public File createStarterWithAssets() throws FileNotFoundException, IOException {
        
        moveAssetsToBackupDir();
        return createStarterData() ;
        
    }
    
    public File createStarterData() {
        createXMLFiles() ;
        
        return outputDirectory;
        
        
    }
    
    
    /**
     * This method will pull a list of all tables /classed being managed by
     * hibernate and export them, one class per file to the backupTempFilePath
     * as valid XML. It uses XStream to write the xml out to the files.
     *
     * @throws ServletException
     * @throws IOException
     * @author Will
     * @throws DotDataException
     */
    @CloseDBIfOpened
    private void createXMLFiles()  {

        Logger.info(this, "Starting createXMLFiles into " + outputDirectory);

        Set<Class> _tablesToDump = new HashSet<Class>();
        try {

            /* get a list of all our tables */
            //Including Identifier.class and Language.class because it is not mapped with Hibernate anymore
            _tablesToDump.add(Identifier.class);
            _tablesToDump.add(Language.class);
            _tablesToDump.add(Relationship.class);
            _tablesToDump.addAll(HibernateUtil.getSession().getSessionFactory().getAllClassMetadata().keySet());

            _tablesToDump.removeIf(c->c.equals(Inode.class));
            _tablesToDump.removeIf(c->c.equals(Clickstream.class));
            _tablesToDump.removeIf(c->c.equals(ClickstreamRequest.class));
            _tablesToDump.removeIf(c->c.equals(Clickstream404.class));
            _tablesToDump.removeIf(c->c.equals(Structure.class));
            _tablesToDump.removeIf(c->c.equals(Field.class));
            _tablesToDump.removeIf(c->c.equals(WorkflowHistory.class));
            _tablesToDump.removeIf(c->c.equals(PermissionReference.class));
            
            _tablesToDump.removeIf(t->t.getName().startsWith("Dashboard"));
            _tablesToDump.removeIf(t->t.getName().contains("HBM"));


            HibernateUtil _dh = null;
            DotConnect dc = null;
            List _list = null;
            File _writing = null;


            for (Class clazz : _tablesToDump) {


                /*
                 * String _shortClassName =
                 * clazz.getName().substring(clazz.getName().lastIndexOf("."),clazz.getName().length());
                 * xstream.alias(_shortClassName, clazz);
                 */
                int i= 0;
                int step = 1000;
                int total =0;
                java.text.NumberFormat formatter = new java.text.DecimalFormat("0000000000");
                /* we will only export 10,000,000 items of any given type */
                for(i=0;i < 10000000;i=i+step){

                    _dh = new HibernateUtil(clazz);
                    _dh.setFirstResult(i);
                    _dh.setMaxResults(step);

                    //This line was previously like;
                    //_dh.setQuery("from " + clazz.getName() + " order by 1,2");
                    //This caused a problem when the database is Oracle because Oracle causes problems when the results are ordered
                    //by an NCLOB field. In the case of dot_containers table, the second field, CODE, is an NCLOB field. Because of this,
                    //ordering is done only on the first field for the tables, which is INODE
                    if(com.dotmarketing.beans.Tree.class.equals(clazz)){
                        _dh.setQuery("from " + clazz.getName() + " order by parent, child, relation_type");
                    }
                    else if(MultiTree.class.equals(clazz)){
                        _dh.setQuery("from " + clazz.getName() + " order by parent1, parent2, child, relation_type");
                    }
                    else if(TagInode.class.equals(clazz)){
                        _dh.setQuery("from " + clazz.getName() + " order by inode, tag_id");
                    }
                    else if(Tag.class.equals(clazz)){
                        _dh.setQuery("from " + clazz.getName() + " order by tag_id, tagname");
                    }
                    else if(CalendarReminder.class.equals(clazz)){
                        _dh.setQuery("from " + clazz.getName() + " order by user_id, event_id, send_date");
                    }
                    else if(Identifier.class.equals(clazz)){
                        dc = new DotConnect();
                        dc.setSQL("select * from identifier order by parent_path, id")
                                .setStartRow(i).setMaxRows(step);
                    }
                    else if (Language.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM language order by id")
                                .setStartRow(i).setMaxRows(step);
                    }
                    else if (Relationship.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM relationship order by inode")
                                .setStartRow(i).setMaxRows(step);
                    }
                    else {
                        _dh.setQuery("from " + clazz.getName() + " order by 1");
                    }

                    if(Identifier.class.equals(clazz)){
                        _list = TransformerLocator
                                .createIdentifierTransformer(dc.loadObjectResults()).asList();
                    } else if (Language.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createLanguageTransformer(dc.loadObjectResults()).asList();
                    } else if (Relationship.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createRelationshipTransformer(dc.loadObjectResults()).asList();
                    } else {
                        _list = _dh.list();
                    }
                    if(_list==null ||_list.isEmpty()) {
                        break;
                    }
                    if(_list.get(0) instanceof Comparable){
                        try {
                        java.util.Collections.sort(_list);
                        }
                        catch(java.lang.IllegalArgumentException e) {
                            throw e;
                        }
                        
                    }

                    _writing = new File(outputDirectory,  clazz.getName() + "_" + formatter.format(i) + ".xml");

                    BundlerUtil.objectToXML(_list, _writing);
                    
                    
                    total = total + _list.size();

                    Thread.sleep(10);


                    
                    
                }
                Logger.info(this, "writing : " + total + " records for " + clazz.getName());
            }


            /* Run Liferay's Tables */
            /* Companies */
            _list = ImmutableList.of(APILocator.getCompanyAPI().getDefaultCompany());

            _writing = new File(outputDirectory,  Company.class.getName() + ".xml");
            
            BundlerUtil.objectToXML(_list, _writing);
            


            /* Users */
            _list = APILocator.getUserAPI().findAllUsers();
            _list.add(APILocator.getUserAPI().getDefaultUser());

            _writing = new File(outputDirectory,  User.class.getName() + ".xml");
            BundlerUtil.objectToXML(_list, _writing);

            /* users_roles */
            dc = new DotConnect();

            /* counter */
            dc.setSQL("select * from counter");
            _list = dc.getResults();

            _writing = new File(outputDirectory, "/Counter.xml");
            BundlerUtil.objectToXML(_list, _writing);


            /* image */
            _list = ImageLocalManagerUtil.getImages();

            /*
             * The changes in this part were made for Oracle databases. Oracle has problems when
             * getString() method is called on a LONG field on an Oracle database. Because of this,
             * the object is loaded from liferay and DotConnect is not used
             * http://jira.dotmarketing.net/browse/DOTCMS-1911
             */


            _writing = new File(outputDirectory, "/Image.xml");
            BundlerUtil.objectToXML(_list, _writing);

            /* portlet */

            /*
             * The changes in this part were made for Oracle databases. Oracle has problems when
             * getString() method is called on a LONG field on an Oracle database. Because of this,
             * the object is loaded from liferay and DotConnect is not used
             * http://jira.dotmarketing.net/browse/DOTCMS-1911
             */
            dc.setSQL("select * from portlet");
            _list = dc.getResults();
            _writing = new File(outputDirectory,"/Portlet.xml");
            BundlerUtil.objectToXML(_list, _writing);

            
            //backup content types
            File file = new File(outputDirectory,  "ContentTypes-" + ContentTypeImportExportUtil.CONTENT_TYPE_FILE_EXTENSION);
            new ContentTypeImportExportUtil().exportContentTypes(file);

            file = new File(outputDirectory, "/WorkflowSchemeImportExportObject.json");
            WorkflowImportExportUtil.getInstance().exportWorkflows(file);

            file = new File(outputDirectory, "/RuleImportExportObject.json");
            RulesImportExportUtil.getInstance().export(file);


        } catch (Exception e) {
            Logger.error(this,e.getMessage(),e);
            throw new DotRuntimeException(e);
        }
    }
    
    
    private void moveAssetsToBackupDir() throws FileNotFoundException, IOException{
        String assetDir = ConfigUtils.getAbsoluteAssetsRootPath();

        Logger.info(this, "Moving assets to back up directory: " + outputDirectory);

        FileUtil.copyDirectory(assetDir, outputDirectory + File.separator + "asset", new AssetFileNameFilter());

    }
    
    
    
    
}
