<%@page import="com.dotcms.contenttype.model.field.DataTypes"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>
<%@page import="com.dotcms.contenttype.model.field.LegacyFieldTypes" %>

	var myData = {
		identifier: 'id',
		label: 'label',
		displayName: 'displayName',
		items: [
		{
			id: '<%= LegacyFieldTypes.BINARY.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Binary") %>',
			label: '<%= LanguageUtil.get(pageContext, "Binary") %>',
			show:['elementSelect','required','labelRow','hintText','dataTypeRow','radioBinary'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.binary")) %>',
			displayOrder: 9,
		},{
			id: '<%= LegacyFieldTypes.CATEGORY.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Category") %>',
			label: '<%= LanguageUtil.get(pageContext, "Category") %>',
			show:['elementSelect','required','labelRow','categoryRow','displayType','hintText','userSearchable','radioText','categories'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.category")) %>',
			displayOrder: 15
		},{
			id: '<%= LegacyFieldTypes.CHECKBOX.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Checkbox") %>',
			label: '<%= LanguageUtil.get(pageContext, "Checkbox") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioText'],
			dataType:'<%= DataTypes.TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.checkbox")) %>',
			displayOrder: 4
		},{
			id: '<%= LegacyFieldTypes.CONSTANT.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Constant-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Constant-Field") %>',
			show:['elementSelect','labelRow','valueRow','textAreaValues','displayType','hintText'],
			dataType:'<%= DataTypes.LONG_TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.constant")) %>',
			displayOrder: 21
		},{
			id: '<%= LegacyFieldTypes.CUSTOM_FIELD.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Custom-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Custom-Field") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','validationRow','defaultText','userSearchable','indexed','listed','unique','hintText'],
			dataType:'<%= DataTypes.LONG_TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.custom_field")) %>',
			displayOrder: 19
		},{
			id: '<%= LegacyFieldTypes.DATE.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Date") %>',
			label: '<%= LanguageUtil.get(pageContext, "Date") %>',
			show:['elementSelect','required','labelRow','hintText','defaultText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'<%= DataTypes.DATE %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.date")) %>',
			displayOrder: 12
		},{
			id: '<%= LegacyFieldTypes.DATE_TIME.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Date-and-Time") %>',
			label: '<%= LanguageUtil.get(pageContext, "Date-and-Time") %>',
			show:['elementSelect','required','labelRow','hintText','defaultText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'<%= DataTypes.DATE %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.date_time")) %>',
			displayOrder: 13
		},{
			id: '<%= LegacyFieldTypes.FILE.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "File") %>',
			label: '<%= LanguageUtil.get(pageContext, "File") %>',
			show:['elementSelect','required','labelRow','displayType','dataTypeRow','radioText','hintText'],
			dataType:'<%= DataTypes.TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.file")) %>',
			displayOrder: 11,
			groupLast: true
		},{

			id: '<%= LegacyFieldTypes.HIDDEN.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Hidden-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Hidden-Field") %>',
			show:['elementSelect','labelRow','valueRow','textAreaValues'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.hidden")) %>',
			displayOrder: 20
		},{
			id: '<%= LegacyFieldTypes.IMAGE.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Image") %>',
			label: '<%= LanguageUtil.get(pageContext, "Image") %>',
			show:['elementSelect','required','labelRow','displayType','hintText','dataTypeRow','radioText'],
			dataType:'<%= DataTypes.TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.image")) %>',
			displayOrder: 10
		},{
			id: '<%= LegacyFieldTypes.LINE_DIVIDER.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Line-Divider") %>',
			label: '<%= LanguageUtil.get(pageContext, "Line-Divider") %>',
			show:['elementSelect','displayType','labelRow'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.line_divider")) %>',
			displayOrder: 24

		},{
			id: '<%= LegacyFieldTypes.MULTI_SELECT.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Multi-Select") %>',
			label: '<%= LanguageUtil.get(pageContext, "Multi-Select") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','dataTypeRow','unique','radioBlockText'],
			dataType:'<%= DataTypes.LONG_TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.multi_select")) %>',
			displayOrder: 7
		},{
			id: '<%= LegacyFieldTypes.PERMISSIONS_TAB.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Permissions-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Permissions-Field") %>',
			show:['elementSelect','displayType','labelRow'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.permissions_tab")) %>',
			displayOrder: 22,
			groupLast: true
		},{
			id: '<%= LegacyFieldTypes.RADIO.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Radio") %>',
			label: '<%= LanguageUtil.get(pageContext, "Radio") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','radioText','radioBool','radioDecimal','radioNumber'],
			dataType:'<%= DataTypes.TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.radio")) %>',
			displayOrder: 5
		},{
			id: '<%= LegacyFieldTypes.RELATIONSHIPS_TAB.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Relationships-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Relationships-Field") %>',
			show:['elementSelect','displayType','labelRow'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.relationships_tab")) %>',
			displayOrder: 17
		},{
			id: '<%= LegacyFieldTypes.SELECT.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Select") %>',
			label: '<%= LanguageUtil.get(pageContext, "Select") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','unique','radioText','radioBool','radioDecimal','radioNumber'],
			dataType:'<%= DataTypes.TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.select")) %>',
			displayOrder: 6
		},{
			id: '<%= LegacyFieldTypes.HOST_OR_FOLDER.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Host-Folder") %>',
			label: '<%= LanguageUtil.get(pageContext, "Host-Folder") %>',
			show:['elementSelect','required','labelRow','displayType','hintText','userSearchable'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.host or folder")) %>',
			displayOrder: 18,
			groupLast: true
		},{
			id: '<%= LegacyFieldTypes.TAB_DIVIDER.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Tab-Divider") %>',
			label: '<%= LanguageUtil.get(pageContext, "Tab-Divider") %>',
			show:['elementSelect','displayType','labelRow','radioSystemField'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.tab_divider")) %>',
			displayOrder: 23
		},{
			id: '<%= LegacyFieldTypes.TAG.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Tag") %>',
			label: '<%= LanguageUtil.get(pageContext, "Tag") %>',
			show:['elementSelect','required','labelRow','displayType','defaultText','hintText','userSearchable','dataTypeRow','radioBlockText'],
			dataType:'<%= DataTypes.SYSTEM %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.tag")) %>',
			displayOrder: 16
		},{
			id: '<%= LegacyFieldTypes.TEXT.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Text") %>',
			label: '<%= LanguageUtil.get(pageContext, "Text") %>',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','unique','radioText','radioDecimal','radioNumber'],
			dataType:'<%= DataTypes.TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.text")) %>',
			displayOrder: 1
		},{
			id: '<%= LegacyFieldTypes.TEXT_AREA.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Textarea") %>',
			label: '<%= LanguageUtil.get(pageContext, "Textarea") %>',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioBlockText'],
			dataType:'<%= DataTypes.LONG_TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.textarea")) %>',
			displayOrder: 2
		},{
			id: '<%= LegacyFieldTypes.TIME.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Time") %>',
			label: '<%= LanguageUtil.get(pageContext, "Time") %>',
			show:['elementSelect','required','labelRow','hintText','defaultText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'<%= DataTypes.DATE %>date',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.time")) %>',
			displayOrder: 14,
			groupLast: true
		},{
			id: '<%= LegacyFieldTypes.WYSIWYG.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "WYSIWYG") %>',
			label: '<%= LanguageUtil.get(pageContext, "WYSIWYG") %>',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioBlockText'],
			dataType:'<%= DataTypes.LONG_TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.wysiwyg")) %>',
			displayOrder: 3
		},{
			id: '<%= LegacyFieldTypes.KEY_VALUE.legacyValue() %>',
			displayName: '<%= LanguageUtil.get(pageContext, "Key-Value") %>',
			label: '<%= LanguageUtil.get(pageContext, "Key-Value") %>',
			show:['elementSelect','required','labelRow','displayType','hintText','userSearchable'],
			dataType:'<%= DataTypes.LONG_TEXT %>',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.keyvalue")) %>',
			displayOrder: 8,
			groupLast: true
		}
		]
	};