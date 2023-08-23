/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.velocity;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.enterprise.license.LicenseLevel;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class InlineEditLineDirective extends DotDirective {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "dotedit";
	}

	public int getType() {
		return LINE;
	}

	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException,
			ResourceNotFoundException, ParseErrorException, MethodInvocationException {

		HttpServletRequest req = (HttpServletRequest) context.get("request");
		

		boolean EDIT_MODE = PageMode.get(req) == PageMode.EDIT_MODE;


		//if (!EDIT_MODE || !allowExecution()) {
		if(true) {
			writer.write(node.jjtGetChild(1).value(context).toString());
			return true;
		}
		
		String inode = node.jjtGetChild(0).literal();
		String fieldVar = node.jjtGetChild(1).literal();
		
		if(!fieldVar.contains("$")){
			if (EDIT_MODE)
				writer.write("Invalid call to #dotedit <br> <br>The second parameter is the same as you would use to display the value of the field, e.g. $body or $URLMapContent.body, etc...<br><pre>    #dotedit($content.inode, $content.body)</pre>");
			return true;
		}
		
		
		
		
		/**** We are in edit mode **/

		
		ContentletAPI conAPI = APILocator.getContentletAPI();
		UserWebAPI userAPI = WebAPILocator.getUserWebAPI();

		User user = null;
		try {
			user = userAPI.getLoggedInUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);

		}


		if(inode.contains("$")){
			try{
				inode=	(node.jjtGetChild(0).value(context)).toString();
			}
			catch(NullPointerException npe){
				node.jjtGetChild(1).render(context, writer);
				return true;
			}
		}
		
		inode = inode.replaceAll("\\s", "").replaceAll("\\$", "").replaceAll("\\\"", "").replaceAll("!", "").replace("{", "").replace("}", "");
		fieldVar = fieldVar.replaceAll("\\s", "").replace("$", "").replaceAll("\\\"", "").replaceAll("!", "").replace("{", "").replace("}", "");
		if(fieldVar.indexOf(".") > -1){
			fieldVar = fieldVar.substring(fieldVar.indexOf(".")+1, fieldVar.length());
		}
		Contentlet con = null;

		try {
			con = conAPI.find(inode, user, false);
			//http://jira.dotmarketing.net/browse/DOTCMS-6368
			if(EDIT_MODE && UtilMethods.isSet(con.getIdentifier()) && con.isLive()){
				Contentlet working = null;
				try{
				   working = conAPI.findContentletByIdentifier(con.getIdentifier(), false, con.getLanguageId(),user, false);
				}catch(Exception e){
					Logger.debug(this.getClass(), "Could not find working version for contentlet " + con.getIdentifier() + " : " + e.getMessage());
				}
				if(working!=null && UtilMethods.isSet(working.getInode())){
					con = working;
				}
			}
		} catch (Exception e) {
			Logger.error(InlineEditLineDirective.class, e.getMessage(), e);
		}
		if(con == null){
			node.jjtGetChild(1).render(context, writer);
			return true;
		}
		try {
			if (!APILocator.getPermissionAPI().doesUserHavePermission(con, PermissionAPI.PERMISSION_EDIT, user)) {
				writer.write(node.jjtGetChild(2).value(context).toString());
				return true;
			}
		} catch (DotDataException e) {
			Logger.warn(this.getClass(), "Data error getting permssions :" + e.getMessage());
		}

		boolean editable = false;

		Structure structure = null;

		structure = con.getStructure();
		Field field = null;
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
		for (Field f : fields) {
			if (f.getVelocityVarName().equalsIgnoreCase(fieldVar)) {
				field = f;
				break;
			}
		}

		if (field.getFieldType().equals(Field.FieldType.TEXT.toString())
				|| field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString())
				|| field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
			editable = true;
		}
		

		if (editable) {

			writer.write("<span class='dotContentletInlineEditSpan dotContentletInline" + con.getInode() +"' id='editable-" + con.getInode() + fieldVar +"' title='" + fieldVar
					+ "'  contenteditable=\"true\" onkeydown=\"parent._dotChangeEditContentEditControl('" + con.getInode()
					+ "', '"+fieldVar+"')\"  onfocus=\"parent._dotChangeEditContentEditControl('" + con.getInode()
					+ "', '"+fieldVar+"')\">");

		}

		writer.write(con.get(fieldVar).toString());

		if (editable) {
			writer.write("</span>");
		}

		return true;
	}

	@Override
	protected int[] getAllowedVersions() {
		return new int[] { LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level };
	}
}