package com.dotmarketing.plugins.custom.field;

import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;

public class CustomFieldPluginDeployer implements PluginDeployer {

	public boolean deploy() {
		
		// Structure to demo Custom Field functionality.
		Structure structure = new Structure();
	    structure.setDefaultStructure(false);
	    structure.setName("Custom Structure");
	    structure.setDescription("To Test Custom Field Functionality");
	    StructureFactory.saveStructure(structure);
	    String strInode = structure.getInode();
	   	        	    
	    // values actually to be entered in the code box of the custom field.
	    String value = "<script type=\"text/javascript\">"+
	    "dojo.require('dotcms.dojo.data.UsersReadStore');"+
	    "dojo.require('dijit.form.FilteringSelect');"+
	    "var assignUsersAssetInode = \"\";"+
	    "var assignUsersStore = new dotcms.dojo.data.UsersReadStore({ includeRoles: true, assetInode: assignUsersAssetInode, permission: \"2\" });"+
	    "</script>"+
	    "<select id=\"sel1\" name=\"sel1\" dojoType=\"dijit.form.FilteringSelect\""+
	    "store=\"assignUsersStore\" searchDelay=\"300\" pageSize=\"30\" labelAttr=\"name\""+
	    "invalidMessage=\"Invalid option selected\""+
	    "value=\"\" onChange=\"updateText1Value();\">" +
	    "</select>" +
	    "<script type=\"text/javascript\">" +
	    "function updateText1Value(){" +
	    "document.getElementById('selectUser').value = dijit.byId('sel1').attr('value'); " +
	    "}" +
	    "function updateUserPickerValue(){" +
	    "dijit.byId('sel1').attr('value',document.getElementById('selectUser').value);" +
	    "}" +
	    "setTimeout(\"updateUserPickerValue()\",100)  ;" +
	    "</script>";
	    
	    // User Picker as an Example Custom Filed. 
	    Field field = new Field();
	    field.setFieldName("Select User");
	    field.setFieldContentlet("text1");	    
	    field.setValues(value);
	    field.setFieldType("custom_field");
	    field.setVelocityVarName("selectUser");	    
	    field.setStructureInode(strInode);
	    field.setSortOrder(1);
	    	          
	    FieldFactory.saveField(field);
	    
	    
	    
	    // Text Field to Show on Listing
	    Field field2 = new Field();
	    field2.setFieldName("Label");
	    field2.setFieldType("text");
	    field2.setFieldContentlet("text2");
	    field2.setVelocityVarName("label");	    
	    field2.setStructureInode(strInode);
	    field2.setRequired(true);
	    field2.setListed(true);
	    field2.setSearchable(true);
	    field2.setSortOrder(2);
	   
	    FieldFactory.saveField(field2);
	        
	    return true;
	}

	public boolean redeploy(String version) {
		// TODO Auto-generated method stub
		return true;
	}

}
