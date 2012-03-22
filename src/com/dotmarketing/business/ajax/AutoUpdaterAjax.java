package com.dotmarketing.business.ajax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.util.ReleaseInfo;

public class AutoUpdaterAjax {
	
	
	public Map<String, Object> getLatestVersionInfo() throws IOException{
		
		int limit = 10;
		
		Map<String, Object> toReturn = new HashMap<String, Object>();

		String autoUpdaterFilesQuery =    "http://dotcms.com/JSONContent/?type=json&q="+
		                                   URLEncoder.encode("+structureName:AutoupdaterFiles +AutoupdaterFiles.released:true +AutoupdaterFiles.minor:*"+ReleaseInfo.getVersion()+ "* -AutoupdaterFiles.minor:autoupdater_* +deleted:false +working:true", "UTF-8")+
		                                  "&limit=1&orderBy=AutoupdaterFiles.releasedDate%20desc";
		String autoUpdaterVersionsQuery = "http://dotcms.com/JSONContent/?type=json&q="+
		                                   URLEncoder.encode("+structureName:AutoupdaterVersions +deleted:false +working:true", "UTF-8")+
		                                  "&limit="+limit+"&orderBy=AutoupdaterVersions.major%20desc";
		


		                            
		InputStream filesInputStream = null;
		InputStream versionsInputStream = null;
		InputStream versionsFilesQueryIs  = null;
		String major = "";
		String minor = "";
		try {
			filesInputStream = new URL(autoUpdaterFilesQuery).openStream();
			versionsInputStream = new URL(autoUpdaterVersionsQuery).openStream();
			BufferedReader filesReader = new BufferedReader(new InputStreamReader(filesInputStream, Charset.forName("UTF-8")));
			BufferedReader versionsReader = new BufferedReader(new InputStreamReader(versionsInputStream, Charset.forName("UTF-8")));
			String filesText = UtilMethods.getStringFromReader(filesReader);
			JSONObject filesJSON  = new JSONObject(filesText);
			String versionsText = UtilMethods.getStringFromReader(versionsReader);
			JSONObject versionsJSON  = new JSONObject(versionsText);
			JSONArray majorArr = versionsJSON.getJSONArray("contentlets");
			JSONArray minorArr = filesJSON.getJSONArray("contentlets");
			
			if(minorArr.size()>0){
				String versionsFilesQuery = "http://dotcms.com/JSONContent/?type=json&q="+
				URLEncoder.encode("+Parent_Versions-Child_Files:"+minorArr.getJSONObject(0).getString("identifier")+" +deleted:false +live:true","UTF-8")+
				"&limit=1&orderBy=AutoupdaterFiles.releasedDate%20desc";
				versionsFilesQueryIs = new URL(versionsFilesQuery).openStream();
				BufferedReader versionsFilesReader = new BufferedReader(new InputStreamReader(versionsFilesQueryIs, Charset.forName("UTF-8")));
				String versionsFilesText = UtilMethods.getStringFromReader(versionsFilesReader);
				JSONObject versionsFilesJSON  = new JSONObject(versionsFilesText);
				JSONArray versionsFilesArr = versionsFilesJSON.getJSONArray("contentlets");
				if(versionsFilesArr.size()>0){
					if(versionsFilesQueryIs!=null){
						versionsFilesQueryIs.close();
					}
					versionsFilesQuery = "http://dotcms.com/JSONContent/?type=json&q="+
					URLEncoder.encode("+Parent_Versions-Child_Files:"+versionsFilesArr.getJSONObject(0).getString("identifier")+" -AutoupdaterFiles.minor:autoupdater_* +AutoupdaterFiles.released:true +deleted:false +working:true", "UTF-8")+
					"&limit=1&orderBy=AutoupdaterFiles.releasedDate%20desc";
					versionsFilesQueryIs = new URL(versionsFilesQuery).openStream();
					versionsFilesReader = new BufferedReader(new InputStreamReader(versionsFilesQueryIs, Charset.forName("UTF-8")));
					versionsFilesText = UtilMethods.getStringFromReader(versionsFilesReader);
					versionsFilesJSON  = new JSONObject(versionsFilesText);
					versionsFilesArr = versionsFilesJSON.getJSONArray("contentlets");
					minor = versionsFilesArr.getJSONObject(0).getString("minor");
				}


			}
			if(majorArr.size()>0){
				for(int i=0;i<majorArr.size();i++){
					if(versionsFilesQueryIs!=null){
						versionsFilesQueryIs.close();
					}
					major = majorArr.getJSONObject(i).getString("major");
					String majorIdentifier = majorArr.getJSONObject(i).getString("identifier");
					String versionsFilesQuery = "http://dotcms.com/JSONContent/?type=json&q="+
					URLEncoder.encode("+Parent_Versions-Child_Files:"+majorIdentifier+" +AutoupdaterFiles.released:true -AutoupdaterFiles.minor:autoupdater_* +deleted:false +live:true","UTF-8")+
					"&limit=1&orderBy=AutoupdaterFiles.releasedDate%20desc";
					versionsFilesQueryIs = new URL(versionsFilesQuery).openStream();
					BufferedReader versionsFilesReader = new BufferedReader(new InputStreamReader(versionsFilesQueryIs, Charset.forName("UTF-8")));
					String versionsFilesText = UtilMethods.getStringFromReader(versionsFilesReader);
					JSONObject versionsFilesJSON  = new JSONObject(versionsFilesText);
					JSONArray versionsFilesArr = versionsFilesJSON.getJSONArray("contentlets");
					if(versionsFilesArr.size()>0){
						break;
					}else{
						major = "";
					}
				}
			}
			
		}catch(Exception e){
			Logger.error(this, "Could not get update info from dotcms.com");
		} finally {
			if(filesInputStream!=null){
				filesInputStream.close();
			}
			if(versionsInputStream!=null){
				versionsInputStream.close();
			}
			if(versionsFilesQueryIs!=null){
				versionsFilesQueryIs.close();
			}
		}
		boolean showUpdate = false;
		
		if(UtilMethods.isSet(minor)){
			showUpdate = UtilMethods.compareVersions(minor,ReleaseInfo.getVersion());
			if(!showUpdate){
				minor = "";
			}
		}
		
		if(UtilMethods.isSet(major)){
			if(!showUpdate){
			   showUpdate = UtilMethods.compareVersions(major,ReleaseInfo.getVersion());
			}else{
				if(!UtilMethods.compareVersions(major,ReleaseInfo.getVersion())){
					major = "";
				}
			}
		}
		
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		req.getSession().setAttribute("_autoupdater_showUpdate", showUpdate);
		req.getSession().setAttribute("_autoupdater_major", major);
		req.getSession().setAttribute("_autoupdater_minor", minor);
		req.getSession().setAttribute("_autoupdater_buildNumber", '0');
		toReturn.put("showUpdate", showUpdate);
		toReturn.put("major", major);
		toReturn.put("minor", minor);
		toReturn.put("buildNumber", '0');
		
		return toReturn;
		
	}


}
