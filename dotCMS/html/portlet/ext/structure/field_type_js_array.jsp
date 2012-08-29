<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>

	var myData = {
		identifier: 'id',
		label: 'label',
		imageurl: 'imageurl',
		displayName: 'displayName',
		items: [
		{
			id: 'binary',
			displayName: '<%= LanguageUtil.get(pageContext, "Binary") %>',
			label: '<span class="docNumIcon"></span> <%= LanguageUtil.get(pageContext, "Binary") %>',
			imageurl: '/html/images/icons/document-number.png',
			show:['elementSelect','required','labelRow','hintText','dataTypeRow','radioBinary'],
			dataType:'binary',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.binary")) %>'
		},{
			id: 'category',
			displayName: '<%= LanguageUtil.get(pageContext, "Category") %>',
			label: '<span class="nodeAllIcon"></span> <%= LanguageUtil.get(pageContext, "Category") %>',
			imageurl: '/html/images/icons/node-select-all.png',
			show:['elementSelect','required','labelRow','categoryRow','displayType','hintText','userSearchable','radioText','categories'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.category")) %>'
		},{
			id: 'checkbox',
			displayName: '<%= LanguageUtil.get(pageContext, "Checkbox") %>',
			label: '<span class="checkBoxIcon"></span> <%= LanguageUtil.get(pageContext, "Checkbox") %>',
			imageurl: '/html/images/icons/ui-check-box.png',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioText'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.checkbox")) %>'
		},{
			id: 'constant',
			displayName: '<%= LanguageUtil.get(pageContext, "Constant-Field") %>',
			label: '<span class="textFieldIcon"></span> <%= LanguageUtil.get(pageContext, "Constant-Field") %>',
			imageurl: '/html/images/icons/ui-text-field.png',
			show:['elementSelect','labelRow','valueRow','textAreaValues','displayType','hintText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.constant")) %>'
		},{
			id: 'custom_field',
			displayName: '<%= LanguageUtil.get(pageContext, "Custom-Field") %>',
			label: '<span class="propertyIcon"></span> <%= LanguageUtil.get(pageContext, "Custom-Field") %>',
			imageurl: '/html/images/icons/property.png',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','validationRow','defaultText','userSearchable','indexed','listed','unique','hintText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.custom_field")) %>'
		},{
			id: 'date',
			displayName: '<%= LanguageUtil.get(pageContext, "Date") %>',
			label: '<span class="calDayIcon"></span> <%= LanguageUtil.get(pageContext, "Date") %>',
			imageurl: '/html/images/icons/calendar-day.png',
			show:['elementSelect','required','labelRow','hintText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'date',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.date")) %>'
		},{
			id: 'date_time',
			displayName: '<%= LanguageUtil.get(pageContext, "Date-and-Time") %>',
			label: '<span class="calClockIcon"></span> <%= LanguageUtil.get(pageContext, "Date-and-Time") %>',
			imageurl: '/html/images/icons/calendar-clock.png',
			show:['elementSelect','required','labelRow','hintText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'date',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.date_time")) %>'
		},{
			id: 'file',
			displayName: '<%= LanguageUtil.get(pageContext, "File") %>',
			label: '<span class="docTextIcon"></span> <%= LanguageUtil.get(pageContext, "File") %>',
			imageurl: '/html/images/icons/document-text.png',
			show:['elementSelect','required','labelRow','displayType','dataTypeRow','radioText','hintText'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.file")) %>'
		},{

			id: 'hidden',
			displayName: '<%= LanguageUtil.get(pageContext, "Hidden-Field") %>',
			label: '<span class="tabIcon"></span> <%= LanguageUtil.get(pageContext, "Hidden-Field") %>',
			imageurl: '/html/images/icons/ui-tab.png',
			show:['elementSelect','labelRow','valueRow','textAreaValues'],
			dataType:'constant',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.hidden")) %>'
		},{

			id: 'host or folder',
			displayName: '<%= LanguageUtil.get(pageContext, "Host-Folder") %>',
			label: '<span class="folderGlobeIcon"></span> <%= LanguageUtil.get(pageContext, "Host-Folder") %>',
			imageurl: '/html/images/icons/folder-open-globe.png',
			show:['elementSelect','required','labelRow','displayType','hintText','userSearchable'],
			dataType:'system_field',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.host or folder")) %>'
		},{
			id: 'image',
			displayName: '<%= LanguageUtil.get(pageContext, "Image") %>',
			label: '<span class="imageIcon"></span> <%= LanguageUtil.get(pageContext, "Image") %>',
			imageurl: '/html/images/icons/image.png',
			show:['elementSelect','required','labelRow','displayType','hintText','dataTypeRow','radioText'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.image")) %>'
		},{
			id: 'line_divider',
			displayName: '<%= LanguageUtil.get(pageContext, "Line-Divider") %>',
			label: '<span class="splitterIcon"></span> <%= LanguageUtil.get(pageContext, "Line-Divider") %>',
			imageurl: '/html/images/icons/ui-splitter-horizontal.png',
			show:['elementSelect','displayType','labelRow'],
			dataType:'section_divider',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.line_divider")) %>'

		},{
			id: 'multi_select',
			displayName: '<%= LanguageUtil.get(pageContext, "Multi-Select") %>',
			label: '<span class="multiSelectIcon"></span> <%= LanguageUtil.get(pageContext, "Multi-Select") %>',
			imageurl: '/html/images/icons/ui-list-box.png',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','validationRow','defaultText','hintText','userSearchable','indexed','dataTypeRow','unique','radioBlockText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.multi_select")) %>'
		},{
			id: 'permissions_tab',
			displayName: '<%= LanguageUtil.get(pageContext, "Permissions-Field") %>',
			label: '<span class="tabIcon"></span> <%= LanguageUtil.get(pageContext, "Permissions-Field") %>',
			imageurl: '/html/images/icons/ui-tab.png',
			show:['elementSelect','displayType','labelRow'],
			dataType:'system_field',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.permissions_tab")) %>'
		},{
			id: 'radio',
			displayName: '<%= LanguageUtil.get(pageContext, "Radio") %>',
			label: '<span class="radioIcon"></span> <%= LanguageUtil.get(pageContext, "Radio") %>',
			imageurl: '/html/images/icons/ui-radio-button.png',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','radioText','radioBool','radioDecimal','radioNumber'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.radio")) %>'
		},{
			id: 'relationships_tab',
			displayName: '<%= LanguageUtil.get(pageContext, "Relationships-Field") %>',
			label: '<span class="tabIcon"></span> <%= LanguageUtil.get(pageContext, "Relationships-Field") %>',
			imageurl: '/html/images/icons/ui-tab.png',
			show:['elementSelect','displayType','labelRow'],
			dataType:'system_field',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.relationships_tab")) %>'
		},{
			id: 'select',
			displayName: '<%= LanguageUtil.get(pageContext, "Select") %>',
			label: '<span class="selectIcon"></span> <%= LanguageUtil.get(pageContext, "Select") %>',
			imageurl: '/html/images/icons/ui-combo-box.png',
			show:['elementSelect','required','labelRow','valueRow','textAreaValues','displayType','validationRow','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','unique','radioText','radioBool','radioDecimal','radioNumber'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.select")) %>'
		},{
			id: 'tab_divider',
			displayName: '<%= LanguageUtil.get(pageContext, "Tab-Divider") %>',
			label: '<span class="tabIcon"></span> <%= LanguageUtil.get(pageContext, "Tab-Divider") %>',
			imageurl: '/html/images/icons/ui-tab.png',
			show:['elementSelect','displayType','labelRow','radioSystemField'],
			dataType:'section_divider',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.tab_divider")) %>'
		},{
			id: 'tag',
			displayName: '<%= LanguageUtil.get(pageContext, "Tag") %>',
			label: '<span class="tagIcon"></span> <%= LanguageUtil.get(pageContext, "Tag") %>',
			imageurl: '/html/images/icons/tag.png',
			show:['elementSelect','required','labelRow','displayType','defaultText','hintText','userSearchable','dataTypeRow','radioBlockText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.tag")) %>'
		},{
			id: 'text',
			displayName: '<%= LanguageUtil.get(pageContext, "Text") %>',
			label: '<span class="textFieldIcon"></span> <%= LanguageUtil.get(pageContext, "Text") %>',
			imageurl: '/html/images/icons/ui-text-field.png',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','listed','dataTypeRow','unique','radioText','radioDecimal','radioNumber'],
			dataType:'text',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.text")) %>'
		},{
			id: 'textarea',
			displayName: '<%= LanguageUtil.get(pageContext, "Textarea") %>',
			label: '<span class="scrollPaneIcon"></span> <%= LanguageUtil.get(pageContext, "Textarea") %>',
			imageurl: '/html/images/icons/ui-scroll-pane.png',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioBlockText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.textarea")) %>'
		},{
			id: 'time',
			displayName: '<%= LanguageUtil.get(pageContext, "Time") %>',
			label: '<span class="clockIcon"></span> <%= LanguageUtil.get(pageContext, "Time") %>',
			imageurl: '/html/images/icons/clock.png',
			show:['elementSelect','required','labelRow','hintText','userSearchable','indexed','listed','dataTypeRow','radioDate','hintText'],
			dataType:'date',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.time")) %>'
		},{
			id: 'wysiwyg',
			displayName: '<%= LanguageUtil.get(pageContext, "WYSIWYG") %>',
			label: '<span class="wysiwygIcon"></span> <%= LanguageUtil.get(pageContext, "WYSIWYG") %>',
			imageurl: '/html/images/icons/ui-scroll-pane-blog.png',
			show:['elementSelect','required','labelRow','displayType','validationRow','defaultText','hintText','userSearchable','indexed','dataTypeRow','radioBlockText'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.wysiwyg")) %>'
		},{
			id: 'key_value',
			displayName: '<%= LanguageUtil.get(pageContext, "Key-Value") %>',
			label: '<span class="keyvalueIcon"></span> <%= LanguageUtil.get(pageContext, "Key-Value") %>',
			imageurl: '/html/images/icons/application-detail.png',
			show:['elementSelect','required','labelRow','displayType','hintText','userSearchable'],
			dataType:'text_area',
			helpText:'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "field.type.help.keyvalue")) %>'
		}



		]
	};