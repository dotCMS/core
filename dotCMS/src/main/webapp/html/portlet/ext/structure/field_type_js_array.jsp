<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>

	var myData = {
		identifier: 'id',
		label: 'label',
		displayName: 'displayName',
		items: [
		{
			id: 'binary',
			displayName: '<%= LanguageUtil.get(pageContext, "Binary") %>',
			label: '<%= LanguageUtil.get(pageContext, "Binary") %>',
			show:['elementSelect','required','labelRow','hintText','dataTypeRow','radioBinary'],
			dataType:'binary',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.binary")) %>'
		},{
			id: 'category',
			displayName: '<%= LanguageUtil.get(pageContext, "Category") %>',
			label: '<%= LanguageUtil.get(pageContext, "Category") %>',
			show:['elementSelect','required','labelRow','categoryRow','displayType','hintText','userSearchable','radioText','categories'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.category")) %>'
		},{
			id: 'checkbox',
			displayName: '<%= LanguageUtil.get(pageContext, "Checkbox") %>',
			label: '<%= LanguageUtil.get(pageContext, "Checkbox") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioText'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.checkbox")) %>'
		},{
			id: 'constant',
			displayName: '<%= LanguageUtil.get(pageContext, "Constant-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Constant-Field") %>',
			show:['elementSelect','labelRow','valueRow','textAreaValues','displayType','hintText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.constant")) %>'
		},{
			id: 'custom_field',
			displayName: '<%= LanguageUtil.get(pageContext, "Custom-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Custom-Field") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','validationRow','defaultText','userSearchable','indexed','listed','unique','hintText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.custom_field")) %>'
		},{
			id: 'date',
			displayName: '<%= LanguageUtil.get(pageContext, "Date") %>',
			label: '<%= LanguageUtil.get(pageContext, "Date") %>',
			show:['elementSelect','required','labelRow','hintText','defaultText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'date',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.date")) %>'
		},{
			id: 'date_time',
			displayName: '<%= LanguageUtil.get(pageContext, "Date-and-Time") %>',
			label: '<%= LanguageUtil.get(pageContext, "Date-and-Time") %>',
			show:['elementSelect','required','labelRow','hintText','defaultText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'date',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.date_time")) %>'
		},{
			id: 'file',
			displayName: '<%= LanguageUtil.get(pageContext, "File") %>',
			label: '<%= LanguageUtil.get(pageContext, "File") %>',
			show:['elementSelect','required','labelRow','displayType','dataTypeRow','radioText','hintText'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.file")) %>'
		},{

			id: 'hidden',
			displayName: '<%= LanguageUtil.get(pageContext, "Hidden-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Hidden-Field") %>',
			show:['elementSelect','labelRow','valueRow','textAreaValues'],
			dataType:'constant',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.hidden")) %>'
		},{
			id: 'image',
			displayName: '<%= LanguageUtil.get(pageContext, "Image") %>',
			label: '<%= LanguageUtil.get(pageContext, "Image") %>',
			show:['elementSelect','required','labelRow','displayType','hintText','dataTypeRow','radioText'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.image")) %>'
		},{
			id: 'line_divider',
			displayName: '<%= LanguageUtil.get(pageContext, "Line-Divider") %>',
			label: '<%= LanguageUtil.get(pageContext, "Line-Divider") %>',
			show:['elementSelect','displayType','labelRow'],
			dataType:'section_divider',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.line_divider")) %>'

		},{
			id: 'multi_select',
			displayName: '<%= LanguageUtil.get(pageContext, "Multi-Select") %>',
			label: '<%= LanguageUtil.get(pageContext, "Multi-Select") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','dataTypeRow','unique','radioBlockText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.multi_select")) %>'
		},{
			id: 'permissions_tab',
			displayName: '<%= LanguageUtil.get(pageContext, "Permissions-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Permissions-Field") %>',
			show:['elementSelect','displayType','labelRow'],
			dataType:'system_field',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.permissions_tab")) %>'
		},{
			id: 'radio',
			displayName: '<%= LanguageUtil.get(pageContext, "Radio") %>',
			label: '<%= LanguageUtil.get(pageContext, "Radio") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','radioText','radioBool','radioDecimal','radioNumber'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.radio")) %>'
		},{
			id: 'relationships_tab',
			displayName: '<%= LanguageUtil.get(pageContext, "Relationships-Field") %>',
			label: '<%= LanguageUtil.get(pageContext, "Relationships-Field") %>',
			show:['elementSelect','displayType','labelRow'],
			dataType:'system_field',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.relationships_tab")) %>'
		},{
			id: 'select',
			displayName: '<%= LanguageUtil.get(pageContext, "Select") %>',
			label: '<%= LanguageUtil.get(pageContext, "Select") %>',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','unique','radioText','radioBool','radioDecimal','radioNumber'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.select")) %>'
		},{
			id: 'host or folder',
			displayName: '<%= LanguageUtil.get(pageContext, "Host-Folder") %>',
			label: '<%= LanguageUtil.get(pageContext, "Host-Folder") %>',
			show:['elementSelect','required','labelRow','displayType','hintText','userSearchable'],
			dataType:'system_field',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.host or folder")) %>'
		},{
			id: 'tab_divider',
			displayName: '<%= LanguageUtil.get(pageContext, "Tab-Divider") %>',
			label: '<%= LanguageUtil.get(pageContext, "Tab-Divider") %>',
			show:['elementSelect','displayType','labelRow','radioSystemField'],
			dataType:'section_divider',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.tab_divider")) %>'
		},{
			id: 'tag',
			displayName: '<%= LanguageUtil.get(pageContext, "Tag") %>',
			label: '<%= LanguageUtil.get(pageContext, "Tag") %>',
			show:['elementSelect','required','labelRow','displayType','defaultText','hintText','userSearchable','dataTypeRow','radioBlockText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.tag")) %>'
		},{
			id: 'text',
			displayName: '<%= LanguageUtil.get(pageContext, "Text") %>',
			label: '<%= LanguageUtil.get(pageContext, "Text") %>',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','unique','radioText','radioDecimal','radioNumber'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.text")) %>'
		},{
			id: 'textarea',
			displayName: '<%= LanguageUtil.get(pageContext, "Textarea") %>',
			label: '<%= LanguageUtil.get(pageContext, "Textarea") %>',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioBlockText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.textarea")) %>'
		},{
			id: 'time',
			displayName: '<%= LanguageUtil.get(pageContext, "Time") %>',
			label: '<%= LanguageUtil.get(pageContext, "Time") %>',
			show:['elementSelect','required','labelRow','hintText','defaultText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'date',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.time")) %>'
		},{
			id: 'wysiwyg',
			displayName: '<%= LanguageUtil.get(pageContext, "WYSIWYG") %>',
			label: '<%= LanguageUtil.get(pageContext, "WYSIWYG") %>',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioBlockText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.wysiwyg")) %>'
		},{
			id: 'key_value',
			displayName: '<%= LanguageUtil.get(pageContext, "Key-Value") %>',
			label: '<%= LanguageUtil.get(pageContext, "Key-Value") %>',
			show:['elementSelect','required','labelRow','displayType','hintText','userSearchable'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.keyvalue")) %>'
		}
		]
	};