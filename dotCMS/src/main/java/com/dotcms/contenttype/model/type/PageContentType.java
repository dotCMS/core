package com.dotcms.contenttype.model.type;

import java.util.ArrayList;
import java.util.List;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
@JsonSerialize(as = ImmutablePageContentType.class)
@JsonDeserialize(as = ImmutablePageContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class PageContentType extends ContentType{



	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentType baseType() {
		return  BaseContentType.HTMLPAGE;
	}
	public abstract static class Builder implements ContentTypeBuilder {}
	
	
	
	public  List<Field> requiredFields(){
		int i=0;
		
		List<Field> fields = new ArrayList<Field>();
		fields.add(
				ImmutableCustomField.builder()
				.name("Title")
				.dataType(DataTypes.TEXT)
				.variable("title")
				.fixed(true)
				.indexed(true)
				.values("$velutil.mergeTemplate('/static/htmlpage_assets/title_custom_field.vtl')")
				.required(true)
				.sortOrder(i++)
				.listed(true)
				.build()
		);
		fields.add(
				ImmutableTextField.builder()
				.name("Url")
				.dataType(DataTypes.TEXT)
				.variable("url")
				.indexed(true)
				.searchable(true)
				.required(true)
				.sortOrder(i++)
				.fixed(true)
				.listed(true)
				.build()
		);
		fields.add(
				ImmutableHostFolderField.builder()
				.name("Site or Folder")
				.dataType(DataTypes.SYSTEM)
				.variable("hostFolder")
				.sortOrder(i++)
				.required(true)
				.fixed(true)
				.indexed(true)
				.build()
		);
		fields.add(
				ImmutableCustomField.builder()
				.name("Template")
				.dataType(DataTypes.TEXT)
				.variable("template")
				.indexed(true)
				.required(true)
				.fixed(true)
				.values("$velutil.mergeTemplate('/static/htmlpage_assets/template_custom_field.vtl')")
				.sortOrder(i++)
				.searchable(true)
				.build()
		);
		fields.add(
				ImmutableCheckboxField.builder()
				.name("Show on Menu")
				.dataType(DataTypes.TEXT)
				.variable("showOnMenu")
				.values("|true")
				.defaultValue("false")
				.fixed(true)
				.indexed(true)
				.sortOrder(i++)
				.build()
		);
		
		fields.add(
				ImmutableTextField.builder()
				.name("Sort Order")
				.dataType(DataTypes.INTEGER)
				.variable("sortOrder")
				.required(true)
				.fixed(true)
				.searchable(true)
				.defaultValue("0")
				.indexed(true)
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableCustomField.builder()
				.name("Cache TTL")
				.dataType(DataTypes.TEXT)
				.variable("cachettl")
				.indexed(true)
				.fixed(true)
				.required(true)
				.values("$velutil.mergeTemplate('/static/htmlpage_assets/cachettl_custom_field.vtl')")
				.sortOrder(i++)
				.listed(true)
				.build()
		);
		fields.add(
				ImmutableTextField.builder()
				.name("Friendly Name")
				.dataType(DataTypes.TEXT)
				.variable("friendlyname")
				.indexed(true)
				.fixed(true)
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableTabDividerField.builder()
				.name("Advanced Properties")
				.variable("advancedtab")
				.sortOrder(i++)
				.build()
		);


		fields.add(
				ImmutableCustomField.builder()
				.name("Redirect URL")
				.variable("redirecturl")
				.values("$velutil.mergeTemplate('/static/htmlpage_assets/redirect_custom_field.vtl')")
				.dataType(DataTypes.TEXT)
				.fixed(true)
				.listed(true)
				.sortOrder(i++)
				.build()
		);
		
		

		

		fields.add(
				ImmutableCheckboxField.builder()
				.name("HTTPS Required")
				.dataType(DataTypes.TEXT)
				.variable("httpsreq")
				.fixed(true)
				.values("|true")
				.defaultValue("false")
				.sortOrder(i++)
				.build()
		);
		
		fields.add(
				ImmutableTextAreaField.builder()
				.name("SEO Description")
				.dataType(DataTypes.LONG_TEXT)
				.variable("seodescription")
				.indexed(true)
				.fixed(true)
				.sortOrder(i++)
				.build()
		);
		
		fields.add(
				ImmutableTextAreaField.builder()
				.name("SEO Keywords")
				.dataType(DataTypes.LONG_TEXT)
				.variable("seokeywords")
				.indexed(true)
				.fixed(true)
				.sortOrder(i++)
				.build()
		);
		
		fields.add(
				ImmutableTextAreaField.builder()
				.name("Page Metadata")
				.dataType(DataTypes.LONG_TEXT)
				.variable("pagemetadata")
				.fixed(true)
				.sortOrder(i++)
				.build()
		);
		
		return ImmutableList.copyOf(fields);
	}
}
