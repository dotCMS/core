/**
 *  DotCMSHelloCMIS is a simple class demonstrating 
 *  how to access dotCMS through CMIS using Atom binding.
 *  
 *  Right now dotCMS through CMIS supports 
 *  
 *  Reading Hosts, Folders, Structures, Contents, Files and HTMLPages,
 *  
 *  Reading Folders/Files by path,
 *  
 *  Adding Folders and Files as Contents,
 *  
 *  Querying Contents based on structure name.
 *  
 *  Updating and Deleting is not supported due to security measures.
 *   
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;

public class DotCMSHelloCMIS {
	
    public static void main(String args[]) {

    	System.out.println("Welcome to dotCMS through CMIS Getting Started...");
    	
        String dotCMSCMISURL = "";
        String userEmail = "";
        String password = "";
        final String EOL = System.getProperty("line.separator");
    	try{

    		System.out.println("Enter URL to access dotCMS through CMIS. Example dotCMS CMIS URL ...");
            System.out.println("http://demo.dotcms.com/cmis or http://localhost:8090/cmis");
    	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
    	    dotCMSCMISURL  = bufferRead.readLine();
    	    
    	    System.out.println("Enter dotCMS EmailId to Login  : ");
    	    bufferRead = new BufferedReader(new InputStreamReader(System.in));
    	    userEmail  = bufferRead.readLine();
    	    
        	System.out.println("Enter password : ");
        	bufferRead = new BufferedReader(new InputStreamReader(System.in));
        	password  = bufferRead.readLine();
        	
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    	
        // Create a SessionFactory and set up the SessionParameter map
        SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();

        // user credentials - 
        parameter.put(SessionParameter.USER, userEmail);
        parameter.put(SessionParameter.PASSWORD, password);

        // connection settings - substitute your own URL
        parameter.put(SessionParameter.ATOMPUB_URL, dotCMSCMISURL);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

        System.out.println("Accessing ATOMPUB_URL: " + parameter.get(SessionParameter.ATOMPUB_URL)
                + " userid: " + parameter.get(SessionParameter.USER) 
                + " password: " + parameter.get(SessionParameter.PASSWORD) + EOL);

        // find all the repositories at this URL - there should only be one.
        List<Repository> repositories = new ArrayList<Repository>();
        repositories = sessionFactory.getRepositories(parameter);
        for (Repository r : repositories) {
            System.out.println("Found repository: " + r.getName());
        }

        // create session with the first (and only) repository
        Repository repository = repositories.get(0);
        parameter.put(SessionParameter.REPOSITORY_ID, repository.getId());
        Session session = sessionFactory.createSession(parameter);

        System.out.println("Got a connection to repository: " + repository.getName()
                + ", with id: " + repository.getId() + EOL);


        // Get everything in the root folder and print the names of the objects
        Folder root = session.getRootFolder();
        ItemIterable<CmisObject> children = root.getChildren();
        System.out.println("Found the following objects in the root folder:-");
        for (CmisObject o : children) {
            System.out.println(o.getName() + " which is of type " + o.getType().getDisplayName());
        }

        System.out.println(EOL + "File and Folders...");
        System.out.println("-------------------");

        Folder defaultHost = null;
        for (CmisObject o : root.getChildren()) {
        	defaultHost = (Folder) o;
            break;
        }

        // Add a new folder to the default host
        System.out.println("Creating 'CMISTest" + new java.util.Date().getTime() +"' to the default host : " + defaultHost.getName() + EOL);

        Map<String, String> newFolderProps = new HashMap<String, String>();
        newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "folder");
        newFolderProps.put(PropertyIds.NAME, "CMISTest" + new java.util.Date().getTime());
        Folder newFolder = defaultHost.createFolder(newFolderProps);

        // Did it work?
        children = root.getChildren();
        System.out.println("Now finding the following objects in the default host : " + defaultHost.getName());
        for (CmisObject o : children) {
        	defaultHost = (Folder) o;
        	ItemIterable<CmisObject> hostChildren = defaultHost.getChildren();
        	for (CmisObject obj : hostChildren) {
        		System.out.println(obj.getName());
        	}
        	break;
        }

        
        // Create a simple text document in the new folder under default host        
        // First, create the content stream
        final String textFileName = "test.txt";
        System.out.println(EOL + "Creating a simple text document : " + textFileName + " inside the above created new folder" + EOL);
        String mimetype = "text/plain; charset=UTF-8";
        String content = "This is some test content.";
        String filename = textFileName;

        byte[] buf = null;
        try {
            buf = content.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ByteArrayInputStream input = new ByteArrayInputStream(buf);
        ContentStream contentStream = session.getObjectFactory().createContentStream(filename,
                buf.length, mimetype, input);

        // Create the Document Object
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "fileasset");
        properties.put(PropertyIds.NAME, filename);
        ObjectId id = newFolder.createDocument(properties, contentStream, VersioningState.NONE);

        // Did it work?
        // Get the contents of the document by id
        System.out.println("Getting object by id : " + id.getId());
        Document doc = (Document) session.getObject(id);
        try {
            content = getContentAsString(doc.getContentStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Contents of " + doc.getName() + " are : " + content + EOL);

        // Get the contents of the document by path
        String path = newFolder.getPath() + textFileName;
        System.out.println("Getting object by path : " + path);
        doc = (Document) session.getObjectByPath(path);
        try {
            content = getContentAsString(doc.getContentStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Contents of " + doc.getName() + " are : " + content + EOL);

        // Types
        System.out.println("\nTypes...");
        System.out.println("--------");
        // Look at the type definition
        System.out.println("Getting type definition for doc");
        ObjectType objectType = session.getTypeDefinition(doc.getType().getId());
        System.out.println("doc is of type " + objectType.getDisplayName());
        System.out.println("isBaseType() returns " + (objectType.isBaseType() ? "true" : "false"));
        ObjectType baseType = objectType.getBaseType();
        if (baseType == null) {
            System.out.println("getBaseType() returns null");
        } else {
            System.out.println("getBaseType() returns " + baseType.getDisplayName());
        }
        ObjectType parentType = objectType.getParentType();
        if (parentType == null) {
            System.out.println("getParentType() returns null");
        } else {
            System.out.println("getParentType() returns " + parentType.getDisplayName());
        }
        System.out.println("Listing child types of " + objectType.getDisplayName());
        for (ObjectType o : objectType.getChildren()) {
            System.out.println("\t" + o.getDisplayName());
        }
        System.out.println("Getting immediate descendant types of " + objectType.getDisplayName());
        for (Tree<ObjectType> o : objectType.getDescendants(1)) {
            System.out.println("\t" + o.getItem().getDisplayName());
        }

        System.out.println("\nProperties...");
        System.out.println("-------------");

        // Look at all the properties of the document
        System.out.println(doc.getName() + " properties start");
        List<Property<?>> props = doc.getProperties();
        for (Property<?> p : props) {
            System.out.println(p.getDefinition().getDisplayName() + "=" + p.getValuesAsString());
        }
        System.out.println(doc.getName() + " properties end" + EOL);

        // get a property by id
        System.out.println("get property by property id");
        Property<?> someProperty = props.get(0);
        System.out.println(someProperty.getDisplayName() + " property on " + doc.getName()
                + " (by getPropertValue()) is " + doc.getPropertyValue(someProperty.getId()));

        // get a property by query name
        System.out.println("get property by query name");
        if (session.getRepositoryInfo().getCapabilities().getQueryCapability()
                .equals(CapabilityQuery.METADATAONLY)) {
            System.out.println("Full search not supported");
        } else {
            String query = "SELECT * FROM fileasset WHERE title = 'test.txt'";
            ItemIterable<QueryResult> queryResult = session.query(query, false);
            for (QueryResult item : queryResult) {
                System.out.println("property cmis:name on test.txt is "
                        + item.getPropertyByQueryName("cmis:name").getFirstValue());
                break;
            }
        }

        GregorianCalendar calendar = doc.getLastModificationDate();
        String DATE_FORMAT = "yyyyMMdd";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        System.out.println("Last modification for  " + doc.getName() + " is  on "
                + sdf.format(calendar.getTime()));

        System.out.println("\nQuery...");
        System.out.println("--------");
        // Query 1 - need full query capability for this
        if (session.getRepositoryInfo().getCapabilities().getQueryCapability()
                .equals(CapabilityQuery.METADATAONLY)) {
            System.out.println("Full search not supported");
        } else {
            String query = "SELECT * FROM News";// News content type
            ItemIterable<QueryResult> q = session.query(query, false);

            // Did it work?
            System.out.println("***results from query " + query);

            int i = 1;
            for (QueryResult qr : q) {
                System.out.println("--------------------------------------------\n" + i + " , "
                        + qr.getPropertyByQueryName("cmis:objectTypeId").getFirstValue() + " , "
                        + qr.getPropertyByQueryName("cmis:name").getFirstValue() + " , "
                        + qr.getPropertyByQueryName("cmis:objectId").getFirstValue() + " , ");
                i++;
            }

            // Query 2
            query = "SELECT * FROM fileasset WHERE title LIKE 'test%'";
            q = session.query(query, false);

            System.out.println(EOL + "***results from query " + query);

            i = 1;
            for (QueryResult qr : q) {
                System.out.println("--------------------------------------------\n" + i + " , "
                        + qr.getPropertyByQueryName("cmis:objectTypeId").getFirstValue() + " , "
                        + qr.getPropertyByQueryName("cmis:name").getFirstValue() + " , "
                        + qr.getPropertyByQueryName("cmis:objectId").getFirstValue() + " , "
                        + qr.getPropertyByQueryName("cmis:contentStreamFileName").getFirstValue()
                        + " , "
                        + qr.getPropertyByQueryName("cmis:contentStreamMimeType").getFirstValue()
                        + " , "
                        + qr.getPropertyByQueryName("cmis:contentStreamLength").getFirstValue());
                i++;
            }
        }

        // Capabilities
        System.out.println("\nCapabilities...");
        System.out.println("---------------");
        // Check what capabilities our repository supports
        System.out.println("Printing repository capabilities...");
        final RepositoryInfo repInfo = session.getRepositoryInfo();
        RepositoryCapabilities cap = repInfo.getCapabilities();
        System.out.println("\nNavigation Capabilities");
        System.out.println("-----------------------");
        System.out.println("Get descendants supported: "
                + (cap.isGetDescendantsSupported() ? "true" : "false"));
        System.out.println("Get folder tree supported: "
                + (cap.isGetFolderTreeSupported() ? "true" : "false"));
        System.out.println("\nObject Capabilities");
        System.out.println("-----------------------");
        System.out.println("Content Stream: " + cap.getContentStreamUpdatesCapability().value());
        System.out.println("Changes: " + cap.getChangesCapability().value());
        System.out.println("Renditions: " + cap.getRenditionsCapability().value());
        System.out.println("\nFiling Capabilities");
        System.out.println("-----------------------");
        System.out.println("Multifiling supported: "
                + (cap.isMultifilingSupported() ? "true" : "false"));
        System.out.println("Unfiling supported: " + (cap.isUnfilingSupported() ? "true" : "false"));
        System.out.println("Version specific filing supported: "
                + (cap.isVersionSpecificFilingSupported() ? "true" : "false"));
        System.out.println("\nVersioning Capabilities");
        System.out.println("-----------------------");
        System.out
                .println("PWC searchable: " + (cap.isPwcSearchableSupported() ? "true" : "false"));
        System.out.println("PWC Updatable: " + (cap.isPwcUpdatableSupported() ? "true" : "false"));
        System.out.println("All versions searchable: "
                + (cap.isAllVersionsSearchableSupported() ? "true" : "false"));
        System.out.println("\nQuery Capabilities");
        System.out.println("-----------------------");
        System.out.println("Query: " + cap.getQueryCapability().value());
        System.out.println("Join: " + cap.getJoinCapability().value());
        System.out.println("\nACL Capabilities");
        System.out.println("-----------------------");
        System.out.println("ACL: " + cap.getAclCapability().value());
        System.out.println("End of  repository capabilities");

        System.out.println("\nAllowable actions...");
        System.out.println("--------------------");
        // find the current allowable actions for the test.txt document
        System.out.println("Getting the current allowable actions for the " + doc.getName()
                + " document object...");
        for (Action a : doc.getAllowableActions().getAllowableActions()) {
            System.out.println("\t" + a.value());
        }

        System.out.println("End of Getting Started...");
    }

    /**
     * Helper method to get the contents of a stream
     * 
     * @param stream
     * @return
     * @throws IOException
     */
    private static String getContentAsString(ContentStream stream) throws IOException {
        InputStream in2 = stream.getStream();
        StringBuffer sbuf = null;
        sbuf = new StringBuffer(in2.available());
        int count;
        byte[] buf2 = new byte[100];
        while ((count = in2.read(buf2)) != -1) {
            for (int i = 0; i < count; i++) {
                sbuf.append((char) buf2[i]);
            }
        }
        in2.close();
        return sbuf.toString();
    }

}