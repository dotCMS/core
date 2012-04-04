package com.dotmarketing.cms.virtualtour.factories;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


public class VirtualTourFactory {
	static int thumbW = 94;
	static int thumbH = 94;
	static int photoW = 350;
	static int photoH = 350;

    public static String getBuildingTypesXML() {
        Structure st = StructureFactory.getStructureByType(Config.getStringProperty("V_TOUR_BUILDINGS_STRUCT"));
        Field f = FieldFactory.getFieldByName(st.getInode(), "Building Type");
        String values = f.getValues();
        String[] types = values.split("\n");
        StringBuffer xml = new StringBuffer();

        String buildingListPath = Config.getStringProperty("V_TOUR_BUILDING_LIST_PATH");
        
        xml.append("<?xml version='1.0'?>\n");
        xml.append("<xmlPath>\n");
        int i = 0;
        xml.append("    <myPath categoryName=\"All\" xmlFile=\"" + buildingListPath + "\" />\n");
        for (String type : types) {
            String[] typeSplitted = type.split("\\|");
            if (typeSplitted.length == 2) {
                xml.append("    <myPath categoryName=\"" + typeSplitted[0].trim() + "\" xmlFile=\"" + 
                        buildingListPath + "?type=" + typeSplitted[1].trim() + "\" />\n");
            }
            i++;
        }
        xml.append("</xmlPath>\n");
        return xml.toString();
    }
    
    public static String getBuildingListXML(String type) {
        
        Logger.debug(VirtualTourFactory.class, "getBuildingListXML: type = " + type);
        String pathToDir = Config.CONTEXT.getRealPath("/WEB-INF/velocity/static/vt/");
        File dirFile = new File(pathToDir);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        String fileName = "";
        if (UtilMethods.isSet(type)) {
            fileName = "buildings-" + type + ".xml";
        } else {
            fileName = "buildings.xml";
        }
        String path = dirFile.getAbsolutePath() + File.separator + fileName;
        Logger.debug(VirtualTourFactory.class, "getBuildingListXML: file path = " + path);
        File file = new File(path);
        String text = "";
        if (file.exists()) {
            Logger.debug(VirtualTourFactory.class, "getBuildingListXML: file path exits!");
            long fileModification = file.lastModified();
            if (tooOld(fileModification)) {
                Logger.debug(VirtualTourFactory.class, "getBuildingListXML: file path exists but it's too old rebuilding the file!");
                file.delete();
                FileWriter fw = null;
                try {
                    boolean success = file.createNewFile();
                    if (success) {
                        fw = new FileWriter (file);
                        text = buildBuildingListXML(type);
                        fw.write(text);
                    }
                } catch (IOException e) {
                    Logger.error(VirtualTourFactory.class, "Error trying to generate the index xml file for the buildings", e);
                } finally {
                    if (fw != null)
                        try {
                            fw.close();
                        } catch (IOException e) {
                            Logger.error(VirtualTourFactory.class, "Error trying to close the file writer", e);
                        }
                }
            } else {
                Logger.debug(VirtualTourFactory.class, "getBuildingListXML: file path exists reading the file!");
                FileReader fr = null;
                try {
                    fr = new FileReader(file);
                    int filechar = fr.read();
                    while (filechar != -1) {
                        text += (char) filechar;
                        filechar = fr.read();
                    }
                } catch (IOException e) {
                    Logger.error(VirtualTourFactory.class, "Error trying to read the index xml file for the buildings", e);
                } finally {
                    if (fr != null)
                        try {
                            fr.close();
                        } catch (IOException e) {
                            Logger.error(VirtualTourFactory.class, "Error trying to close the file reader", e);
                        }
                }
            }
        } else {
            Logger.debug(VirtualTourFactory.class, "getBuildingListXML: file path doesn't exist, building the file");
            boolean success;
            FileWriter fw = null;
            try {
                success = file.createNewFile();
                fw = new FileWriter (file);
                if (success) {
                    text = buildBuildingListXML(type);
                    fw.write(text);
                }
            } catch (IOException e) {
                Logger.error(VirtualTourFactory.class, "Error trying to generate the index xml file for the buildings", e);
            } finally {
                if (fw != null)
                    try {
                        fw.close();
                    } catch (IOException e) {
                        Logger.error(VirtualTourFactory.class, "Error trying to close the file writer", e);
                    }
            }
        }
        
        return text;
	}

    private static String buildBuildingListXML(String type) {
        Structure st = StructureFactory.getStructureByType(Config.getStringProperty("V_TOUR_BUILDINGS_STRUCT"));
        String luceneQuery = "+structureInode: " + st.getInode() + " +type:content +live:true +deleted:false +languageId:1 -text2:tour";
        if (UtilMethods.isSet(type))
            luceneQuery = "+structureInode: " + st.getInode() + " +type:content +live:true +deleted:false +languageId:1 +text2:" + type.toLowerCase();
        
        Logger.debug(VirtualTourFactory.class, "buildBuildingListXML: lucene query = " + luceneQuery);
        
        ContentletAPI conAPI = APILocator.getContentletAPI();

		UserAPI userAPI = APILocator.getUserAPI();
        
   
        	List<Contentlet> hits= new ArrayList <Contentlet>();
			try {
				hits = conAPI.findByStructure(st, userAPI.getSystemUser(), false, -1,0);
			} catch (DotDataException e) {
				 Logger.debug(VirtualTourFactory.class, "Error retrieving contentlets" );	
			} catch (DotSecurityException e) {
				Logger.debug(VirtualTourFactory.class, "User do not have required permission" );
			}
      
        
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version='1.0'?>\n");
        xml.append("<database>\n");
        if(hits != null){
	        Logger.debug(VirtualTourFactory.class, "buildBuildingListXML: hits.getTotal() = " + hits.size());
	        for (Contentlet cont: hits) {
	            
	            String name = cont.getTitle();
	            String identifier = cont.getIdentifier();
	            Map map=cont.getMap();
	            List <Field> fields=st.getFields();
	            String xcoordvarname="";
	            String ycoordvarname="";
	            for(Field field :fields){
	            	if(field.getFieldContentlet().equals("integer6")){
	            		xcoordvarname=field.getVelocityVarName();
	            	}
	            	else if(field.getFieldContentlet().equals("integer7")){
	            		ycoordvarname=field.getVelocityVarName();
	            	}
	            }
	            long xCoord = (Long)Long.parseLong(map.get(xcoordvarname).toString());
	            long yCoord = (Long)Long.parseLong(map.get(ycoordvarname).toString());
	            String refPage = Config.getStringProperty("V_TOUR_BUILDING_PATH") + "?id=" + 
	                identifier;
	            if (xCoord > 0 && yCoord > 0) {
	                xml.append("<Index myListN=\"" + name + "\" myXcor=\"" + xCoord + "\" myYcor=\"" + yCoord + 
	                        "\" myStop=\"" + refPage + "\"></Index>\n");
	            }
	        }
        }
        xml.append("</database>\n");
        return xml.toString();
    }
    public static String getBuildingDetailXML(String buildingIdentifier) throws DotStateException, DotDataException, DotSecurityException 
    {
    	return getBuildingDetailXML(buildingIdentifier,thumbW,thumbH,photoW,photoH);
    }

    public static String getBuildingDetailXML(String buildingIdentifier,int thumbW,int thumbH,int photoW,int photoH) throws DotStateException, DotDataException, DotSecurityException
    {
        String pathToDir = Config.CONTEXT.getRealPath("/WEB-INF/velocity/static/vt/");
        File dirFile = new File(pathToDir);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        if (!InodeUtils.isSet(buildingIdentifier))
            return "";
        String fileName = "building-" + buildingIdentifier + ".xml";
        String path = dirFile.getAbsolutePath() + File.separator + fileName;
        File file = new File(path);
        String text = "";
        if (file.exists()) {
            long fileModification = file.lastModified();
            if (tooOld(fileModification)) {
                file.delete();
                FileWriter fw = null;
                try {
                    boolean success = file.createNewFile();
                    if (success) {
                        fw = new FileWriter (file);
                        text = buildBuildingDetailXML(buildingIdentifier,thumbW,thumbH,photoW,photoH);
                        fw.write(text);
                    }
                } catch (IOException e) {
                    Logger.error(VirtualTourFactory.class, "Error trying to generate the index xml file for the buildings", e);
                } finally {
                    if (fw != null)
                        try {
                            fw.close();
                        } catch (IOException e) {
                            Logger.error(VirtualTourFactory.class, "Error trying to close the file writer", e);
                        }
                }
            } else {
                FileReader fr = null;
                try {
                    fr = new FileReader(file);
                    int filechar = fr.read();
                    while (filechar != -1) {
                        text += (char) filechar;
                        filechar = fr.read();
                    }
                } catch (IOException e) {
                    Logger.error(VirtualTourFactory.class, "Error trying to read the index xml file for the buildings", e);
                } finally {
                    if (fr != null)
                        try {
                            fr.close();
                        } catch (IOException e) {
                            Logger.error(VirtualTourFactory.class, "Error trying to close the file reader", e);
                        }
                }
            }
        } else {
            boolean success;
            FileWriter fw = null;
            try {
                success = file.createNewFile();
                fw = new FileWriter (file);
                if (success) {
                    text = buildBuildingDetailXML(buildingIdentifier,thumbW,thumbH,photoW,photoH);
                    fw.write(text);
                }
            } catch (IOException e) {
                Logger.error(VirtualTourFactory.class, "Error trying to generate the index xml file for the buildings", e);
            } finally {
                if (fw != null)
                    try {
                        fw.close();
                    } catch (IOException e) {
                        Logger.error(VirtualTourFactory.class, "Error trying to close the file writer", e);
                    }
            }
        }
        
        return text;
    }

    private static String buildBuildingDetailXML(String identifier,int thumbW,int thumbH,int photoW,int photoH) throws DotStateException, DotDataException, DotSecurityException 
    {
    	ContentletAPI conAPI = APILocator.getContentletAPI();
    	//Identifier id = (Identifier) InodeFactory.getInode(identifier, Identifier.class);
    	Identifier id = APILocator.getIdentifierAPI().find(identifier);
        com.dotmarketing.portlets.contentlet.business.Contentlet cont = (com.dotmarketing.portlets.contentlet.business.Contentlet)  APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(), false);
        Contentlet c = null;
        if (!InodeUtils.isSet(cont.getInode()))
            return "";
        String body = "";
		try {
			c = conAPI.convertFatContentletToContentlet(cont);
			body = (String)conAPI.getFieldValue(c, c.getStructure().getField("Short Description"));
		} catch (Exception e) {
			Logger.error(VirtualTourFactory.class,"Unable to get field value for body",e);
		}
        int imgTotal = 0;
        String[] imagesInodes = new String[5];
        for (int i = 0; i < 5; i++) {
            String imageInode = "";
			try {
				imageInode = (String)conAPI.getFieldValue(c, c.getStructure().getField("Building Image " + (i + 1)));
			} catch (Exception e) {
				Logger.error(VirtualTourFactory.class,"Unable to get field value for imageInode",e);
			}
            if (InodeUtils.isSet(imageInode)) { 
                imgTotal++;
                imagesInodes[i] = imageInode;
            }
        }
        StringBuffer xml = new StringBuffer ();
        xml.append("dummyVar=none&");
        xml.append("stopDescription=" + UtilMethods.encodeURL(body) + "&");
        xml.append("imgTotal=" + imgTotal + "&");
        for (int i = 0; i < 5; i++) {
            if (InodeUtils.isSet(imagesInodes[i])) {
                String url = "thumbfoto" + (i + 1) + "=" + UtilMethods.encodeURL("/thumbnail?inode=" + imagesInodes[i] + "&h=" + thumbH + "&w=" + thumbW + "");
                xml.append(url);
                xml.append("&");
            } else {
                String url = "thumbfoto" + (i + 1) + "=null";
                xml.append(url);
                xml.append("&");
            }
        }
        int imgCounter = imgTotal;
        for (int i = 0; i < 5; i++) {
            if (InodeUtils.isSet(imagesInodes[i])) {
                imgCounter--;
                String url = "foto" + (i + 1) + "=" + UtilMethods.encodeURL("/thumbnail?inode=" + imagesInodes[i] + "&h=" + photoH + "&w=" + photoW + "");
                xml.append(url);
                if (i < 4)
                    xml.append("&");
            } else {
                imgCounter--;
                String url = "foto" + (i + 1) + "=null";
                xml.append(url);
                if (i < 4)
                    xml.append("&");
            }
        }
        return xml.toString();
    }

    private static boolean tooOld(long fileModification) {
        long now = new Date().getTime();
        boolean returnValue = false;
        int hoursToLive = Config.getIntProperty("V_TOUR_REFRESH_TIME", 15);
        long millisToLive = hoursToLive * 60000;
        if ((now - fileModification) > millisToLive) {
            returnValue = true;
        }
        return returnValue;
    }

}
