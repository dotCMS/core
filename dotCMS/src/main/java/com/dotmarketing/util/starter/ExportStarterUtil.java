package com.dotmarketing.util.starter;

import static java.io.File.separator;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Clickstream404;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionReference;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.cmsmaintenance.util.AssetFileNameFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.util.RulesImportExportUtil;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TrashUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.ZipUtil;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.ejb.ImageLocalManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.TeeOutputStream;

/**
 * Utility class used to generate a bundle with assets and data (starter file)
 * The bundle will be a compressed file with assets and data modeled in JSON files
 * @author nollymarlonga
 */
public class ExportStarterUtil {

    

    final File outputDirectory;
    
    public ExportStarterUtil() {
        
        outputDirectory = new File(ConfigUtils.getBackupPath() + File.separator + "backup_" + System.currentTimeMillis());
        outputDirectory.mkdirs();
       
        
    }
    
    public File createStarterWithAssets() throws IOException {
        
        moveAssetsToBackupDir();
        return createStarterData() ;
        
    }
    
    public File createStarterData() {
        createJSONFiles() ;
        
        return outputDirectory;
        
        
    }

    private Set getTableSet() throws HibernateException {
        final Set<Class> _tablesToDump = new HashSet<>();
        _tablesToDump.add(Identifier.class);
        _tablesToDump.add(Language.class);
        _tablesToDump.add(Relationship.class);
        _tablesToDump.add(ContentletVersionInfo.class);
        _tablesToDump.add(Template.class);
        _tablesToDump.add(Contentlet.class);
        _tablesToDump.add(Category.class);
        _tablesToDump.add(Tree.class);
        _tablesToDump.add(MultiTree.class);
        _tablesToDump.add(Folder.class);

        //end classes no longer mapped with Hibernate
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

        return _tablesToDump;
    }
    
    /**
     * This method will pull a list of all tables /classed being managed by
     * hibernate and export them, one class per file to the backupTempFilePath
     * as valid JSON. It uses Jackson to write the json out to the files.
     *
     * @throws ServletException
     * @throws IOException
     * @author Will
     * @throws DotDataException
     */
    @CloseDBIfOpened
    private void createJSONFiles()  {

        Logger.info(this, "Starting createJSONFiles into " + outputDirectory);


        try {

            /* get a list of all our tables */
            //Including classes that are no longer mapped with Hibernate anymore
            final Set<Class> _tablesToDump = getTableSet();

            HibernateUtil _dh = null;
            DotConnect dc = null;
            List _list = null;
            File _writing = null;
            java.text.NumberFormat formatter = new java.text.DecimalFormat("0000000000");

            for (Class clazz : _tablesToDump) {
                int i= 0;
                int step = 1000;
                int total =0;
                
                /* we will only export 10,000,000 items of any given type */
                for(i=0;i < 10000000;i=i+step) {

                    _dh = new HibernateUtil(clazz);
                    _dh.setFirstResult(i);
                    _dh.setMaxResults(step);

                    if (Tree.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM tree order by parent, child, relation_type")
                                .setStartRow(i).setMaxRows(step);
                    } else if (MultiTree.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL(
                                        "SELECT * FROM multi_tree order by parent1, parent2, child, relation_type")
                                .setStartRow(i).setMaxRows(step);
                    } else if (TagInode.class.equals(clazz)) {
                        _dh.setQuery("from " + clazz.getName() + " order by inode, tag_id");
                    } else if (Tag.class.equals(clazz)) {
                        _dh.setQuery("from " + clazz.getName() + " order by tag_id, tagname");
                    } else if (Identifier.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("select * from identifier order by parent_path, id")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Language.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM language order by id")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Relationship.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM relationship order by inode")
                                .setStartRow(i).setMaxRows(step);
                    } else if (ContentletVersionInfo.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM contentlet_version_info ORDER BY identifier")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Template.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM template ORDER BY inode")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Contentlet.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL(
                                        "select contentlet.*, contentlet_1_.owner from contentlet join inode contentlet_1_ "
                                                + " on contentlet_1_.inode = contentlet.inode ORDER BY contentlet.inode")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Category.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM category ORDER BY inode")
                                .setStartRow(i).setMaxRows(step);
                    } else if (Folder.class.equals(clazz)) {
                        dc = new DotConnect();
                        dc.setSQL("SELECT * FROM folder ORDER BY inode")
                                .setStartRow(i).setMaxRows(step);
                    } else {
                        _dh.setQuery("from " + clazz.getName() + " order by 1");
                    }

                    if (Identifier.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createIdentifierTransformer(dc.loadObjectResults()).asList();
                    } else if (Language.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createLanguageTransformer(dc.loadObjectResults()).asList();
                    } else if (Relationship.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createRelationshipTransformer(dc.loadObjectResults()).asList();
                    } else if (ContentletVersionInfo.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createContentletVersionInfoTransformer(dc.loadObjectResults())
                                .asList();
                    } else if (Template.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createTemplateTransformer(dc.loadObjectResults())
                                .asList();
                    } else if (Contentlet.class.equals(clazz)) {

                        final ContentletJsonAPI contentletJsonAPI = APILocator.getContentletJsonAPI();
                        final List<Contentlet> contentlets = TransformerLocator
                                .createContentletTransformer(dc.loadObjectResults())
                                .asList();
                        _list = contentlets.stream().map(contentlet ->
                                contentletJsonAPI.toImmutable(contentlet)).collect(Collectors.toList());
                    } else if (Category.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createCategoryTransformer(dc.loadObjectResults())
                                .asList();
                    } else if(Folder.class.equals(clazz)) {
                        _list = TransformerLocator
                                .createFolderTransformer(dc.loadObjectResults())
                                .asList();
                    } else if (Tree.class.equals(clazz)){
                        _list = TransformerLocator.createTreeTransformer(dc.loadObjectResults()).asList();
                    } else if (MultiTree.class.equals(clazz)){
                        _list = TransformerLocator.createMultiTreeTransformer(dc.loadObjectResults()).asList();
                    } else {
                        _list = _dh.list();
                    }
                    if(_list==null ||_list.isEmpty()) {
                        break;
                    }
                    if(_list.get(0) instanceof Comparable){
                        java.util.Collections.sort(_list);
                    }

                    _writing = new File(outputDirectory,  clazz.getName() + "_" + formatter.format(i) + ".json");

                    //We use a different serializer for ImmutableContentlets
                    if (Contentlet.class.equals(clazz)) {
                        ContentletJsonHelper.INSTANCE.get().writeContentletListToFile(_list, _writing);
                    } else{
                        BundlerUtil.objectToJSON(_list, _writing);
                    }

                    total = total + _list.size();

                    Thread.sleep(50);

                }
                Logger.info(this, "writing : " + total + " records for " + clazz.getName());
            }


            /* Run Liferay's Tables */
            /* Companies */
            _list = ImmutableList.of(APILocator.getCompanyAPI().getDefaultCompany());

            _writing = new File(outputDirectory,  Company.class.getName() + ".json");

            BundlerUtil.objectToJSON(_list, _writing);

            /* Users */
            _list = APILocator.getUserAPI().findAllUsers();
            _list.add(APILocator.getUserAPI().getDefaultUser());

            _writing = new File(outputDirectory,  User.class.getName() + ".json");
            BundlerUtil.objectToJSON(_list, _writing);

            /* users_roles */
            dc = new DotConnect();

            /* counter */
            dc.setSQL("select * from counter");
            _list = dc.getResults();

            _writing = new File(outputDirectory, "/Counter.json");
            BundlerUtil.objectToJSON(_list, _writing);


            /* image */
            _list = ImageLocalManagerUtil.getImages();

            /*
             * The changes in this part were made for Oracle databases. Oracle has problems when
             * getString() method is called on a LONG field on an Oracle database. Because of this,
             * the object is loaded from liferay and DotConnect is not used
             * http://jira.dotmarketing.net/browse/DOTCMS-1911
             */


            _writing = new File(outputDirectory, "/Image.json");
            BundlerUtil.objectToJSON(_list, _writing);

            /* portlet */

            /*
             * The changes in this part were made for Oracle databases. Oracle has problems when
             * getString() method is called on a LONG field on an Oracle database. Because of this,
             * the object is loaded from liferay and DotConnect is not used
             * http://jira.dotmarketing.net/browse/DOTCMS-1911
             */
            dc.setSQL("select * from portlet");
            _list = dc.getResults();
            _writing = new File(outputDirectory,"/Portlet.json");
            BundlerUtil.objectToJSON(_list, _writing);

            
            //backup content types
            File file = new File(outputDirectory,  "ContentTypes-" + ContentTypeImportExportUtil.CONTENT_TYPE_FILE_EXTENSION);
            new ContentTypeImportExportUtil().exportContentTypes(file);

            file = new File(outputDirectory, "/WorkflowSchemeImportExportObject.json");
            WorkflowImportExportUtil.getInstance().exportWorkflows(file);

            file = new File(outputDirectory, "/RuleImportExportObject.json");
            RulesImportExportUtil.getInstance().export(file);

            Logger.info(this, "Finished createJSONFiles into " + outputDirectory);
        } catch (Exception e) {
            Logger.error(this,e.getMessage(),e);
            throw new DotRuntimeException(e);
        }
    }
    
    
    private void moveAssetsToBackupDir() throws IOException{
        String assetDir = ConfigUtils.getAbsoluteAssetsRootPath();

        Logger.info(this, "Moving assets to back up directory: " + outputDirectory);

        FileUtil.copyDirectory(assetDir, outputDirectory + File.separator + "assets", new AssetFileNameFilter());
    }

    /**
     * Manipulates contents of a directory and put them through a given output stream not without first compressing them
     * to a generated zip file.
     *
     * @param output given output stream
     * @param withAssets flag telling to include assets or not
     * @param download flag telling to download as a file or as a stream
     * @return file zip file
     * @throws IOException
     */
    public Optional<File> zipStarter(final OutputStream output, final boolean withAssets, final boolean download)
            throws IOException {
        final File outputDir = withAssets
                ? new ExportStarterUtil().createStarterWithAssets()
                : new ExportStarterUtil().createStarterData();
        final File zipFile = new File(
                outputDir + UtilMethods
                        .dateToJDBC(new Date())
                        .replace(':', '-')
                        .replace(' ', '_') + ".zip");
        Logger.info(this, "Zipping up to file:" + zipFile.getAbsolutePath());

        try (final OutputStream outStream = download
                ? new TeeOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile.toPath())), output)
                : new BufferedOutputStream(Files.newOutputStream(zipFile.toPath()));
                final ZipOutputStream zipOutput = new ZipOutputStream(outStream)) {
            ZipUtil.zipDirectory(outputDir.getAbsolutePath(), zipOutput);
        }

        Logger.info(this, "Wrote starter to : " + zipFile.toPath());
        new TrashUtils().moveFileToTrash(outputDir, "starter");

        final File newLocation = new File(ConfigUtils.getAbsoluteAssetsRootPath()
                + separator
                + "bundles"
                + separator
                + zipFile.getName());
        FileUtils.moveFile(zipFile, newLocation);

        return Optional.of(newLocation);
    }
}
