package com.dotmarketing.portlets.contentratings.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Rating;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentratings.factories.ContentRatingsFactory;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portlet.ActionResponseImpl;

public class GenerateContentRatingsReportAction extends PortletAction {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

	
    public void processAction(
            ActionMapping mapping, ActionForm form, PortletConfig config,
            ActionRequest req, ActionResponse res)
        throws Exception {
        Logger.debug(this, "Running GenerateContentRatingsReportAction!!");
        
        String structureInode = req.getParameter("structure_type");
        
        String reportName = "Content Ratings Report";
        Date reportDate = new Date();
        
        Structure structure = StructureFactory.getStructureByInode(structureInode);
        
        String reportComments = "Content Ratings Submitted for " + structure.getName();

        String dateString = (new java.text.SimpleDateFormat("M-d-yyyy")).format(reportDate);
        String reportFileName = structure.getName().replaceAll(" ", "") + "Report-" + dateString + ".xls";

        List<String> reportHeaders = new ArrayList<String>();

        reportHeaders.add("Structure Inode");
        reportHeaders.add("Structure Name");
        reportHeaders.add("Content Inode");
        reportHeaders.add("Content Title");
        reportHeaders.add("Content Rating");
        reportHeaders.add("Date");
        reportHeaders.add("User ID");
        reportHeaders.add("Session ID");
            
        List<List<String>> reportData = new ArrayList<List<String>>();
        
        List<Rating> list = ContentRatingsFactory.getContentRatingsByStructure(structure.getInode());
        List<String> row;
        
        for (Rating rating : list) {
            
            try {
                row = new ArrayList<String>();
                Identifier id = APILocator.getIdentifierAPI().find(rating.getIdentifier());
                Contentlet content = conAPI.findContentletByIdentifier(id.getInode(), false, langAPI.getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), true);
                String conTitle = conAPI.getName(content, APILocator.getUserAPI().getSystemUser(), true);
                row.add("" + structure.getInode());
                row.add(UtilMethods.isSet(structure.getName()) ? structure.getName() : "");
                row.add("" + content.getInode());
                row.add(UtilMethods.isSet(conTitle) ? conTitle : "");
                row.add("" + rating.getRating());
                row.add(UtilMethods.isSet(rating.getRatingDate()) ? UtilMethods.dateToHTMLDate(rating.getRatingDate()) : "");
                row.add(UtilMethods.isSet(rating.getUserId()) ? rating.getUserId() : "");
                row.add(UtilMethods.isSet(rating.getSessionId()) ? rating.getSessionId() : "");
                
                reportData.add(row);
            } catch (Exception e) {
                Logger.error(this,e.getMessage(),e);
            }
        }
        
        _writeReport(reportName, reportHeaders, reportData, reportDate, reportFileName, reportComments, res);
    }

    private void _writeReport (String reportName, List reportHeaders, List reportData, Date reportDate, 
            String reportFileName, String reportComments, ActionResponse res) throws IOException {

        HttpServletResponse response = ((ActionResponseImpl)res).getHttpServletResponse();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + reportFileName + "\"");

        ServletOutputStream out = response.getOutputStream();
        
        int columns = 1;
        
        int dataSize = reportData.size();
        int dataColums = 0;
        if (dataSize > 0)
            dataColums = ((List)reportData.get(0)).size();

        if (dataSize > 0 && dataColums > 0) {
            columns = ((List)reportData.get(0)).size();
        } else if (reportHeaders.size()>0) {
            columns = reportHeaders.size();
        }
        out.println("<table border=\"1\">");

        if (UtilMethods.isSet(reportComments)) { 
            out.println("<tr>");
            out.println("<td colspan=\""+columns+"\"><font color=\"navy\"><b>"+reportComments+"</b></font></td>");
            out.println("</tr>");
        } 

        out.println("<tr>");
        out.println("<td colspan=\""+columns+"\"></td>");
        out.println("</tr>");

        if (reportHeaders != null) {
            Iterator it = reportHeaders.iterator();
            out.println("<tr>");
            while (it.hasNext()) {
                String header = (String)it.next();
                out.println("<td bgcolor=\"blue\"><b><font color=\"white\">"+header+"</font></b></td>");
            }
            out.println("</tr>");
        }

        if (reportData.size() == 0) {
            out.println("<tr>");
            out.println("<td colspan=\"" + columns + "\" align=\"center\">No Records Found</td>");
            out.println("</tr>");
            
        }
        
        Iterator it = reportData.iterator();
        while (it.hasNext()) {
            Object row = (Object) it.next();
            
            Iterator it2 = null;
            
            it2 = ((List)row).iterator();
                
            out.println("<tr>");
            while (it2.hasNext()) {
                String value = (String)it2.next();
                out.println("<td>"+value+"</td>");
            }
            out.println("</tr>");
        }
        out.println("</table>");
    
        out.flush();
        out.close();
    }
}