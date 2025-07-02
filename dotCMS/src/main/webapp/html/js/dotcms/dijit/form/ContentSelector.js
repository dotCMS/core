/**
 *
 * This is a dijit widget that present a dialog to select a content of the specified structure type,
 * it renders a content search dialog that lets the user search and select a content.
 *
 * To include the dijit widget into your page
 *
 * JS Side
 *
 * <script type="text/javascript">
 * 	dojo.require("dotcms.dijit.form.ContentSelector");
 *
 * ...
 *
 * </script>
 *
 * HTML side
 *
 * <div id="myDijitId" jsId="myJSVariable" structureInode="structureInode" languageId="languageId"
 * 	style="cssStyleOptions" title="My title" onContentSelected="your JS function" dojoType="dotcms.dijit.form.ContentSelector"></div>
 *
 * How to show the dialog
 *
 * <script type="text/javascript">
 * 	myJSVariable.show();
 * </script>
 *
 *
 *
 * Properties
 *
 * structureInode: required - this is used to search content of particular structure type.
 *
 * languageId: non-required - this is the com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE used to search for contents.
 *
 * id: non-required - this is the id of the widget if not specified then it will be auto generated.
 *
 * jsId: non-required - if specified then dijit will be registered in the JS global environment with this name.
 *
 * title: non-required - Title of the dialog.
 *
 * variantName: non-required - this is used to search content of particular variant.
 * 
 * onFileSelected: non-required - JS script or JS function callback to be executed when the user selects a content from the results,
 *
 * the content object is passed to the function.
 *
 */

dojo.provide('dotcms.dijit.form.ContentSelector');

dojo.require('dijit._Widget');
dojo.require('dijit._Templated');
dojo.require('dijit.Dialog');
dojo.require('dijit.form.Button');
dojo.require('dijit.layout.BorderContainer');

var isNg = new URLSearchParams(document.location.search).get('ng');
// This variable maps to the values declared in Field.FieldType.HOST_OR_FOLDER
const HOST_OR_FOLDER = "host or folder"

dojo.declare(
    'dotcms.dijit.form.ContentSelector',
    [dijit._Widget, dijit._Templated],
    {
        templatePath: dojo.moduleUrl(
            'dotcms',
            isNg
                ? 'dijit/form/ContentSelectorNoDialog.jsp'
                : 'dijit/form/ContentSelector.jsp'
        ),
        selectButtonTemplate:
            '<button id="{buttonInode}" dojoType="dijit.form.Button">{selectButtonLabel}</button>',
        checkBoxTemplate:
            '<input value="{buttonInode}" class="contentCheckbox" onClick="event.stopPropagation(); contentSelector.setSelectedInode(this)" dojoType="dijit.form.CheckBox"></input>',
        widgetsInTemplate: true,
        title: '',
        structureInode: '',
        structureVelVar: '',
        setDotFieldTypeStr: '',
        currentSortBy: 'score,modDate desc',
        DOT_FIELD_TYPE: 'dotFieldType',
        hasHostFolderField: false,
        counter_radio: 0,
        counter_checkbox: 0,
        languageId: '',
        currentPage: 1,
        searchCounter: 1,
        dialogCounter: 1,
        headers: new Array(),
        categories: new Array(),
        checkboxesIds: {},
        radiobuttonsIds: {},
        resultsButtonIds: new Array(),
        currentStructureFields: new Array(),
        tagTextValue: '',
        suggestedTagsTextValue: '',
        noResultsTextValue: '',
        matchResultsTextValue: '',
        contentletLanguageId: '',
        relationJsName: '',
        multiple: 'false',
        containerStructures: new Array(),
        availableLanguages: new Array(),
        numberOfResults: 20,
        selectButtonLabel: 'Select',
        useRelateContentOnSelect: false,
        selectedInodesSet: new Set(),
        variantName: 'DEFAULT',

        setSelectedInode: function (selectBtn) {
            if (selectBtn.checked) {
                this.selectedInodesSet.add(selectBtn.value);
            } else {
                this.selectedInodesSet.delete(selectBtn.value);
            }
        },

        postCreate: function () {
            this.containerStructures = this._decodeQuoteChars(
                this.containerStructures
            );
            var structuresParam = this.containerStructures.toString();
            this.containerStructures = structuresParam.length
                ? JSON.parse(structuresParam)
                : [];

            if (this.containerStructures.length > 0) {
                this._fillStructures();
            } else {
                this._structureChanged();
            }
            LanguageAjax.getLanguagesWithAllOption(
                dojo.hitch(this, this._fillLanguages)
            );

            if (this.title != '') !isNg && this.dialog.set('title', this.title);
            this.tagTextValue = this.tagText.value;
            this.suggestedTagsTextValue = this.suggestedTagsText.value;
            this.noResultsTextValue = this.noResultsText.value;
            this.matchResultsTextValue = this.matchResultsText.value;
            !isNg && this.dialog.hide();
            dojo.parser.parse(this.search_fields_table);
        },

        displayStructureFields: function (structureInode) {
            this.structureInode = structureInode;
            StructureAjax.getStructureDetails(
                this.structureInode,
                dojo.hitch(this, this._structureDetailsCallback)
            );
        },

        show: function (noclear) {
            if (!noclear) {
                this._clearSearch();
            }

            !isNg && this.dialog.show();
        },

        hide: function () {
            this._clearSearch();
            !isNg && this.dialog.hide();
        },

        _decodeQuoteChars: function (structures) {
            return structures.map((chunk) =>
                chunk.replace(/%27/g, "'").replace(/%22/g, '&quot;')
            );
        },

        _structureDetailsCallback: function (structure) {
            if (structure.inode === 'catchall') {
                this.search_general.style.display = 'block';
            } else {
                this.generalSearch.set('value', '');
                this.search_general.style.display = 'none';
            }

            !isNg &&
                this.dialog.set(
                    'title',
                    structure['name'] ? structure['name'] : ''
                );
            this.structureVelVar = structure['velocityVarName']
                ? structure['velocityVarName']
                : '';
            this._structureChanged();

            if (isNg) {
                var self = this;
                setTimeout(function () {
                    self._doSearchPage1();
                }, 100);
            }
        },

        _structureChanged: function () {
            this.currentSortBy = 'score,modDate desc';
            this.setDotFieldTypeStr = '';
            LanguageAjax.getLanguagesWithAllOption(
                dojo.hitch(this, this._fillLanguages)
            );
            StructureAjax.getSearchableStructureFields(
                this.structureInode,
                dojo.hitch(this, this._fillFields)
            );
            StructureAjax.getStructureCategories(
                this.structureInode,
                dojo.hitch(this, this._fillCategories)
            );
            this._hideMatchingResults();
            this.nextDiv.style.display = 'none';
            this.previousDiv.style.display = 'none';
            if (this.langDropdown) this.langDropdown.style.display = '';
            //this.counter_radio = 0;
            this.counter_checkbox = 0;
        },

        _fillStructures: function () {
            this.content_type_select.innerHTML = '';
            var htmlstr = "<dl class='vertical'>";
            htmlstr += '<dt><label><b>Content Type:</b></label></dt>';
            htmlstr += '<dd>';
            if (this.containerStructures.length > 1) {
                dojo.require('dijit.form.FilteringSelect');
                htmlstr +=
                    "<select dojoType='dijit.form.FilteringSelect' onChange='displayStructure(this.value)' id='structuresSelect+" +
                    this.dialogCounter +
                    "' required='true' name='structuresSelect+" +
                    this.dialogCounter +
                    "'>";

                for (var i = 0; i < this.containerStructures.length; i++) {
                    htmlstr +=
                        "<option value='" +
                        this.containerStructures[i].inode +
                        "'";
                    if (
                        (this.structureInode == '' && i == 0) ||
                        this.containerStructures[i].inode == this.structureInode
                    ) {
                        htmlstr += " selected='selected'";
                    }
                    htmlstr +=
                        '>' + this.containerStructures[i].name + '</option>';
                }

                htmlstr += '</select>';
            } else {
                htmlstr += this.containerStructures[0].name;
            }

            this.displayStructureFields(this.containerStructures[0].inode);
            htmlstr += '</dd>';
            htmlstr += '</dl>';
            dojo.place(htmlstr, this.content_type_select);
            dojo.parser.parse(this.content_type_select);
        },

        _fillLanguages: function (data) {
            if (dijit.byId('langcombo+' + this.dialogCounter)) {
                dijit.byId('langcombo+' + this.dialogCounter).destroy();
            }
            let selectedLang = null;
            let options = ''
            this.availableLanguages = data;

            for (var i = 0; i < data.length; i++) {
                options += "<option  value='" + data[i].id + "'";
                if (this.contentletLanguageId == data[i].id || this.languageId == data[i].id ) {
                    options += " selected='true' ";
                    selectedLang = data[i];
                }
                options +=
                    '>' + this._getCountryLabel(data[i]) + '</option>';
            }


            this.search_languages_table.innerHTML = '';
            var htmlstr = "<dl class='vertical'>";
            htmlstr +=
                "<dt><label for='langcombo+" +
                this.dialogCounter +
                "'>" +
                selectedLang ? selectedLang.title : data[0].title +
                '</label></dt>';
            htmlstr += '<dd>';
            dojo.require('dijit.form.FilteringSelect');
            htmlstr +=
                "<select dojoType='dijit.form.FilteringSelect' dojoAttachPoint='langDropdown' id='langcombo+" +
                this.dialogCounter +
                "' required='false' name='langcombo+" +
                this.dialogCounter +
                "'>";

            htmlstr += options;
            htmlstr += '</select>';
            htmlstr += '</dd>';
            htmlstr += '</dl>';
            dojo.place(htmlstr, this.search_languages_table);
            dojo.parser.parse(this.search_languages_table);

            let obj = dijit.byId('langcombo+' + this.dialogCounter);

            // Set the displayed value
            if (selectedLang) {
                obj.set('displayedValue', this._getCountryLabel(selectedLang));
            }
        },

        _getSiteFolderFieldDefaultHTML: function (){
            const defaultSiteFolderField = {
                "fieldContentlet": "system_field",
                "fieldFieldType": HOST_OR_FOLDER,
                "fieldName": "Site or Folder",
                "fieldValues": "",
                "fieldVelocityVarName": "siteOrFolder",
            }

            var htmlstr = "<dl class='vertical'>";
            htmlstr += '<dt><label>' + this._fieldName(defaultSiteFolderField) + '</label></dt>';
            htmlstr += '<dd>' + this._renderSearchField(defaultSiteFolderField) + '</dd>';
            htmlstr += '</dl>';

            return htmlstr;
        },

        _fillFields: function (data) {
            this.currentStructureFields = data;
            this.search_fields_table.innerHTML = '';
            this.site_folder_field_pop.innerHTML = '';
            var htmlstr = "<dl class='vertical'>";
            for (var i = 0; i < data.length; i++) {
                var type = data[i]['fieldFieldType'];
                if (type == 'category' || type == 'hidden' || type == HOST_OR_FOLDER) {
                    continue;
                }
                htmlstr +=
                    '<dt><label>' + this._fieldName(data[i]) + '</label></dt>';
                htmlstr += '<dd>' + this._renderSearchField(data[i]) + '</dd>';
            }
            htmlstr += '</dl>';

            const siteFolderFieldHtml = this._getSiteFolderFieldDefaultHTML();
            dojo.place(siteFolderFieldHtml, this.site_folder_field_pop);
            dojo.place(htmlstr, this.search_fields_table);

            dojo.parser.parse(this.search_fields_table);
            dojo.parser.parse(this.site_folder_field_pop);
            eval(this.setDotFieldTypeStr);
        },

        _fieldName: function (field) {
            var type = field['fieldFieldType'];
            if (type == 'category') {
                return '';
            } else {
                return field['fieldName'] + ':'; //DOTCMS -4381
            }
        },

        // DOTCMS-3896
        _renderSearchField: function (field) {

            var fieldVelocityVarName = field['fieldVelocityVarName'];
            var fieldContentlet = field['fieldContentlet'];
            var value = '';

            var type = field['fieldFieldType'];
            if (type == 'checkbox') {
                //checkboxes fields
                var option = field['fieldValues'].split('\r\n');

                var result = '';

                for (var i = 0; i < option.length; i++) {
                    var actual_option = option[i].split('|');
                    if (
                        actual_option.length > 1 &&
                        actual_option[1] != '' &&
                        actual_option[1].length > 0
                    ) {
                        var checkId =
                            this.structureVelVar +
                            '.' +
                            fieldVelocityVarName +
                            'Field-D' +
                            this.dialogCounter +
                            '-O' +
                            i;
                        result =
                            result +
                            '<input type="checkbox" dojoType="dijit.form.CheckBox" value="' +
                            actual_option[1] +
                            '" ' +
                            'id="' +
                            checkId +
                            '" ' +
                            'name="' +
                            this.structureVelVar +
                            '.' +
                            fieldVelocityVarName +
                            '"> ' +
                            actual_option[0] +
                            '<br>\n';
                        if (!this.checkboxesIds[this.dialogCounter])
                            this.checkboxesIds[this.dialogCounter] =
                                new Array();
                        this.checkboxesIds[this.dialogCounter][
                            this.checkboxesIds[this.dialogCounter].length
                        ] = checkId;
                        this.setDotFieldTypeStr =
                            this.setDotFieldTypeStr +
                            'dojo.attr(' +
                            "'" +
                            checkId +
                            "'" +
                            ",'" +
                            this.DOT_FIELD_TYPE +
                            "'" +
                            ",'" +
                            type +
                            "');";
                    }
                }
                return result;
            } else if (type == 'radio') {
                //radio buttons fields
                var option = field['fieldValues'].split('\r\n');
                var result = '';

                for (var i = 0; i < option.length; i++) {
                    var actual_option = option[i].split('|');
                    if (
                        actual_option.length > 1 &&
                        actual_option[1] != '' &&
                        actual_option[1].length > 0
                    ) {
                        var radioId =
                            this.structureVelVar +
                            '.' +
                            fieldVelocityVarName +
                            'Field-D' +
                            this.dialogCounter +
                            '-R' +
                            this.counter_radio;
                        dijit.registry.remove(radioId);
                        result =
                            result +
                            '<input type="radio" dojoType="dijit.form.RadioButton" value="' +
                            actual_option[1] +
                            '" id="' +
                            radioId +
                            '" name="' +
                            this.structureVelVar +
                            '.' +
                            fieldVelocityVarName +
                            '"> ' +
                            actual_option[0] +
                            '<br>\n';
                        if (!this.radiobuttonsIds[this.dialogCounter])
                            this.radiobuttonsIds[this.dialogCounter] =
                                new Array();
                        this.radiobuttonsIds[this.dialogCounter][
                            this.radiobuttonsIds[this.dialogCounter].length
                        ] = radioId;

                        this.setDotFieldTypeStr =
                            this.setDotFieldTypeStr +
                            'dojo.attr(' +
                            "'" +
                            radioId +
                            "'" +
                            ",'" +
                            this.DOT_FIELD_TYPE +
                            "'" +
                            ",'" +
                            type +
                            "');";

                        this.counter_radio++;
                    }
                }
                return result;
            } else if (type == 'select' || type == 'multi_select') {
                var fieldId =
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    'Field' +
                    this.dialogCounter;
                dijit.registry.remove(fieldId);
                var option = field['fieldValues'].split('\r\n');
                var result = '';
                if (type == 'multi_select')
                    result =
                        result +
                        '<select  dojoType=\'dijit.form.MultiSelect\'  multiple="multiple" size="4" id="' +
                        fieldId +
                        '" name="' +
                        this.structureVelVar +
                        '.' +
                        fieldVelocityVarName +
                        '">\n';
                else
                    result =
                        result +
                        "<select  dojoType='dijit.form.FilteringSelect' id=\"" +
                        fieldId +
                        '" name="' +
                        this.structureVelVar +
                        '.' +
                        fieldVelocityVarName +
                        '">\n<option value="">None</option>';

                for (var i = 0; i < option.length; i++) {
                    var actual_option = option[i].split('|');
                    if (
                        actual_option.length > 1 &&
                        actual_option[1] != '' &&
                        actual_option[1].length > 0
                    ) {
                        auxValue = actual_option[1];
                        if (fieldContentlet.indexOf('bool') != -1) {
                            if (
                                actual_option[1] == 'true' ||
                                actual_option[1] == 't' ||
                                actual_option[1] == '1'
                            ) {
                                auxValue = 't';
                            } else if (
                                actual_option[1] == 'false' ||
                                actual_option[1] == 'f' ||
                                actual_option[1] == '0'
                            ) {
                                auxValue = 'f';
                            }
                        }
                        result =
                            result +
                            '<option value="' +
                            auxValue +
                            '" >' +
                            actual_option[0] +
                            '</option>\n';
                    }
                }

                this.setDotFieldTypeStr =
                    this.setDotFieldTypeStr +
                    'dojo.attr(' +
                    "'" +
                    fieldId +
                    "'" +
                    ",'" +
                    this.DOT_FIELD_TYPE +
                    "'" +
                    ",'" +
                    type +
                    "');";

                result = result + '</select>\n';
                return result;
            } else if (type == 'tag') {
                var result = '<table border="0">';
                result = result + "<tr><td style='padding:0px;'>";
                result =
                    result +
                    '<textarea id="' +
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    'Field' +
                    this.dialogCounter +
                    '"' +
                    ' name="' +
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    '"' +
                    ' cols="20" rows="2" onkeyup="suggestTagsForSearch(this,\'' +
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    'suggestedTagsDiv' +
                    this.dialogCounter +
                    '\');" ' +
                    ' style="border-color: #7F9DB9; border-style: solid; border-width: 1px; ' +
                    ' height: 50px; width: 100%;" ' +
                    ' ></textarea><br/><span style="font-size:11px; color:#999;"> ' +
                    this.tagTextValue +
                    ' </span> ' +
                    ' </td></tr>';
                result =
                    result + '<tr><td valign="top" style=\'padding:0px;\'>';
                result =
                    result +
                    '<div id="' +
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    'suggestedTagsDiv' +
                    this.dialogCounter +
                    '" ' +
                    ' style="height: 50px; font-size:10px;font-color:gray; width: 146px; border:1px solid #ccc;overflow: auto;" ' +
                    '></div><span style="font-size:11px; color:#999;"> ' +
                    this.suggestedTagsTextValue +
                    '</span><br></td></tr></table>';

                this.setDotFieldTypeStr =
                    this.setDotFieldTypeStr +
                    'dojo.attr(' +
                    "'" +
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    'Field' +
                    this.dialogCounter +
                    "'" +
                    ",'" +
                    this.DOT_FIELD_TYPE +
                    "'" +
                    ",'" +
                    type +
                    "');";

                return result;
            } //http://jira.dotmarketing.net/browse/DOTCMS-3232
            else if (type == 'host or folder') {
                // Below code is used to fix the "widget already registered error".
                if (
                    dojo.byId(
                        'FolderHostSelector' +
                            this.dialogCounter +
                            '-hostFoldersTreeWrapper'
                    )
                ) {
                    dojo.byId(
                        'FolderHostSelector' +
                            this.dialogCounter +
                            '-hostFoldersTreeWrapper'
                    ).remove();
                }

                if (dijit.byId('FolderHostSelector' + this.dialogCounter)) {
                    dijit
                        .byId('FolderHostSelector' + this.dialogCounter)
                        .destroy();
                }
                if (
                    dijit.byId(
                        'FolderHostSelector' + this.dialogCounter + '-tree'
                    )
                ) {
                    dijit
                        .byId(
                            'FolderHostSelector' + this.dialogCounter + '-tree'
                        )
                        .destroy();
                }

                dojo.require('dotcms.dijit.form.HostFolderFilteringSelect');

                var hostId = '';
                var fieldValue = hostId;

                var result =
                    '<div id="FolderHostSelector' +
                    this.dialogCounter +
                    '" style=\'width270px\' dojoType="dotcms.dijit.form.HostFolderFilteringSelect" includeAll="true" ' +
                    ' hostId="' +
                    hostId +
                    '" value = "' +
                    fieldValue +
                    '"' +
                    '></div>';

                this.hasHostFolderField = true;

                return result;
            } else if (type == 'category' || type == 'hidden') {
                return '';
            } else if (type.indexOf('date') > -1) {
                var fieldId =
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    'Field' +
                    this.dialogCounter;
                dijit.registry.remove(fieldId);
                if (dijit.byId(fieldId)) {
                    dijit.byId(fieldId).destroy();
                }
                dojo.require('dijit.form.DateTextBox');

                this.setDotFieldTypeStr =
                    this.setDotFieldTypeStr +
                    'dojo.attr(' +
                    "'" +
                    fieldId +
                    "'" +
                    ",'" +
                    this.DOT_FIELD_TYPE +
                    "'" +
                    ",'" +
                    type +
                    "');";

                return (
                    '<input type="text" dojoType="dijit.form.DateTextBox" constraints={datePattern:\'MM/dd/yyyy\'} validate=\'return false;\' invalidMessage=""  id="' +
                    fieldId +
                    '" name="' +
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    '" value="' +
                    value +
                    '">'
                );
            } else {
                var fieldId =
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    'Field' +
                    this.dialogCounter;
                dijit.registry.remove(fieldId);
                if (dijit.byId(fieldId)) {
                    dijit.byId(fieldId).destroy();
                }

                this.setDotFieldTypeStr =
                    this.setDotFieldTypeStr +
                    'dojo.attr(' +
                    "'" +
                    fieldId +
                    "'" +
                    ",'" +
                    this.DOT_FIELD_TYPE +
                    "'" +
                    ",'" +
                    type +
                    "');";

                return (
                    '<input type="text" dojoType="dijit.form.TextBox"  id="' +
                    fieldId +
                    '" name="' +
                    this.structureVelVar +
                    '.' +
                    fieldVelocityVarName +
                    '" value="' +
                    value +
                    '">'
                );
            }
        },

        _fillCategories: function (data) {
            this.categories = data;
            var searchCategoryList = this.search_categories_list;
            searchCategoryList.innerHTML = '';
            var form = this.search_form;
            form.categories = null;
            if (form.categories != null) {
                var tempChildNodesLength = form.categories.childNodes.length;
                for (var i = 0; i < tempChildNodesLength; i++) {
                    form.categories.removeChild(form.categories.childNodes[0]);
                }
            }
            dojo.require('dijit.form.MultiSelect');
            if (data != null) {
                categories = data;
                for (i = 0; i < categories.length; i++) {
                    dojo.create(
                        'dt',
                        {
                            innerHTML:
                                '<label>' +
                                categories[i]['categoryName'] +
                                ':</label>',
                        },
                        searchCategoryList
                    );
                    var selectId =
                        categories[i]['categoryName'].replace(
                            /[^A-Za-z0-9_]/g,
                            ''
                        ) +
                        'Select' +
                        this.dialogCounter;
                    dijit.registry.remove(selectId);
                    if (dijit.byId(selectId)) {
                        dijit.byId(selectId).destroy();
                    }
                    var selectObj =
                        "<select dojoType='dijit.form.MultiSelect' class='width-equals-200' multiple='true' name=\"categories\" id=\"" +
                        selectId +
                        '"></select>';

                    dojo.create(
                        'dd',
                        { innerHTML: selectObj },
                        searchCategoryList
                    );
                }
            }

            var fillCategoryOptions = function (selectId, data) {
                var select = document.getElementById(selectId);
                if (select != null) {
                    for (var i = 0; i < data.length; i++) {
                        var option = new Option();
                        option.text = data[i]['categoryName'];
                        option.value = data[i]['inode'];

                        option.style.marginLeft =
                            data[i]['categoryLevel'] * 10 + 'px';

                        select.options[i] = option;
                    }
                }
            };

            var fillCategorySelect = function (selectId, data) {
                fillCategoryOptions(selectId, data);
                var selectObj = document.getElementById(selectId);
                if (data.length > 1) {
                    var len = data.length;
                    if (len > 9) len = 9;
                    selectObj.size = len;
                }
            };

            for (var i = 0; i < categories.length; i++) {
                var cat = categories[i];
                var selectId =
                    cat['categoryName'].replace(/[^A-Za-z0-9_]/g, '') +
                    'Select' +
                    this.dialogCounter;
                var mycallbackfnc = function (data) {
                    fillCategorySelect(selectId, data);
                };

                CategoryAjax.getSubCategories(cat['inode'], '', {
                    callback: mycallbackfnc,
                    async: false,
                });
            }
        },

        _hideMatchingResults: function () {
            this.matchingResultsDiv.style.visibility = 'hidden';
        },

        _doRelateContent: function (content) {
            var inodes = new Array();

            if (!content.inode) {
                Array.from(this.selectedInodesSet).forEach(function (
                    inode,
                    index
                ) {
                    inodes[index] = inode;
                });
            } else {
                inodes[0] = content.inode;
            }

            // If we add a new relation,
            // We have to wait until that relation is loaded to save the content.
            if(
                typeof relationsLoadedMap !== 'undefined' &&
                relationsLoadedMap[this.relationJsName] &&
                inodes?.length > 0
            ) {
                relationsLoadedMap[this.relationJsName] = false;
            }

            setTimeout(
                "ContentletAjax.getContentletsData ('" +
                    inodes +
                    "', " +
                    this.relationJsName +
                    '_addRelationshipCallback)',
                50
            );

            this._clearSearch();
            !isNg && this.dialog.hide();
        },

        _doSearchPage1: function () {
            this._doSearch(1, null);
        },

        _doSearch: function (page, sortBy) {
            if (sortBy && sortBy[0] === '.') {
                sortBy = sortBy.slice(1);
            }

            var fieldsValues = new Array();

            fieldsValues[fieldsValues.length] = 'languageId';

            //		if(this.languageId == '')
            //		fieldsValues[fieldsValues.length] = this.htmlPageLanguage.value;
            //		else
            //		fieldsValues[fieldsValues.length] = this.languageId;
            let obj = dijit.byId('langcombo+' + this.dialogCounter);
            if (obj && obj.get('displayedValue') != '') {
                fieldsValues[fieldsValues.length] = obj.get('value');
            } else {
                fieldsValues[fieldsValues.length] = '';
            }
            var allField = this.generalSearch.value;

            if (allField != undefined && allField.length > 0) {
                fieldsValues[fieldsValues.length] = 'catchall';
                fieldsValues[fieldsValues.length] = allField + '*';
            }

            for (var h = 0; h < this.currentStructureFields.length; h++) {
                var field = this.currentStructureFields[h];
                var fieldId =
                    this.structureVelVar +
                    '.' +
                    field['fieldVelocityVarName'] +
                    'Field';
                var formField = document.getElementById(fieldId);
                if (formField == null) {
                    fieldId = fieldId + this.dialogCounter;
                    formField = document.getElementById(fieldId);
                }
                var fieldValue = '';

                if (formField != null) {
                    if (field['fieldFieldType'] == 'select') {
                        var tempDijitObj = dijit.byId(formField.id);
                        fieldsValues[fieldsValues.length] =
                            this.structureVelVar +
                            '.' +
                            field['fieldVelocityVarName'];
                        fieldsValues[fieldsValues.length] =
                            tempDijitObj.get('value');
                    } else if (
                        formField.type == 'select-one' ||
                        formField.type == 'select-multiple'
                    ) {
                        var values = '';
                        for (var i = 0; i < formField.options.length; i++) {
                            if (formField.options[i].selected) {
                                fieldsValues[fieldsValues.length] =
                                    this.structureVelVar +
                                    '.' +
                                    field['fieldVelocityVarName'];
                                fieldsValues[fieldsValues.length] =
                                    formField.options[i].value;
                            }
                        }
                    } else {
                        fieldsValues[fieldsValues.length] =
                            this.structureVelVar +
                            '.' +
                            field['fieldVelocityVarName'];
                        fieldsValues[fieldsValues.length] = formField.value;
                    }
                }
            }

            if (this.hasHostFolderField) {
                var fieldId = 'FolderHostSelector' + this.dialogCounter;
                if (!isInodeSet(dijit.byId(fieldId).attr('value'))) {
                    this.hostField.value = '';
                    this.folderField.value = '';
                } else {
                    var data = dijit.byId(fieldId).attr('selectedItem');
                    if (data['type'] == 'host') {
                        this.hostField.value = dijit
                            .byId(fieldId)
                            .attr('value');
                        this.folderField.value = '';
                    } else if (data['type'] == 'folder') {
                        this.hostField.value = '';
                        this.folderField.value = dijit
                            .byId(fieldId)
                            .attr('value');
                    }
                }

                var hostValue = this.hostField.value;
                var folderValue = this.folderField.value;
                if (isInodeSet(hostValue)) {
                    fieldsValues[fieldsValues.length] = 'conHost';
                    fieldsValues[fieldsValues.length] = hostValue;
                }
                if (isInodeSet(folderValue)) {
                    fieldsValues[fieldsValues.length] = 'conFolder';
                    fieldsValues[fieldsValues.length] = folderValue;
                }
            } else {
                fieldsValues[fieldsValues.length] = 'conHost';
                fieldsValues[fieldsValues.length] = 'current';
            }

            if (this.radiobuttonsIds[this.dialogCounter]) {
                for (
                    var i = 0;
                    i < this.radiobuttonsIds[this.dialogCounter].length;
                    i++
                ) {
                    var formField = document.getElementById(
                        this.radiobuttonsIds[this.dialogCounter][i]
                    );
                    if (formField != null && formField.type == 'radio') {
                        var values = '';
                        if (formField.checked) {
                            values = formField.value;
                            fieldsValues[fieldsValues.length] = formField.name;
                            fieldsValues[fieldsValues.length] = values;
                        }
                    }
                }
            }

            if (this.checkboxesIds[this.dialogCounter]) {
                for (
                    var i = 0;
                    i < this.checkboxesIds[this.dialogCounter].length;
                    i++
                ) {
                    var formField = document.getElementById(
                        this.checkboxesIds[this.dialogCounter][i]
                    );
                    if (formField != null && formField.type == 'checkbox') {
                        var values = '';
                        if (formField.checked) {
                            values = formField.value;
                            name = formField.name.substring(
                                0,
                                formField.name.length
                            );
                            fieldsValues[fieldsValues.length] = name;
                            fieldsValues[fieldsValues.length] = values;
                        }
                    }
                }
            }

            var categoriesValues = new Array();
            var form = this.search_form;
            var categories = document.getElementsByName('categories');

            if (categories != null) {
                if (categories.options != null) {
                    var opts = categories.options;
                    for (var j = 0; j < opts.length; j++) {
                        var option = opts[j];
                        if (option.selected) {
                            categoriesValues[categoriesValues.length] =
                                option.value;
                        }
                    }
                } else {
                    for (var i = 0; i < categories.length; i++) {
                        var catSelect = categories[i];
                        var opts = catSelect.options;
                        for (var j = 0; j < opts.length; j++) {
                            var option = opts[j];
                            if (option.selected) {
                                categoriesValues[categoriesValues.length] =
                                    option.value;
                            }
                        }
                    }
                }
            }

            if (page == null) this.currentPage = 1;
            else this.currentPage = page;

            if (sortBy != null) {
                if (sortBy == this.currentSortBy) sortBy = sortBy + ' desc';
                this.currentSortBy = sortBy;
            }

            var searchFor = this.structureInode;
            if (
                this.structureInode === 'catchall' &&
                this.containerStructures.length > 0
            ) {
                searchFor = this.containerStructures
                    .map(function (contentType) {
                        return contentType.inode;
                    })
                    .filter(function (inode) {
                        return inode !== 'catchall';
                    });
            }

            ContentletAjax.searchContentlets(
                searchFor,
                fieldsValues,
                categoriesValues,
                false,
                false,
                false,
                false,
                this.currentPage,
                this.numberOfResults,
                this.currentSortBy,
                null,
                null,
                this.variantName,
                dojo.hitch(this, this._fillResults)
            );

            this.searchCounter++; // this is used to eliminate the widget already registered exception upon repeated searchs.
        },

        _fillResults: function (data) {
            var counters = data[0];
            var hasNext = counters['hasNext'];
            var hasPrevious = counters['hasPrevious'];
            var total = counters['total'];

            this.headers = data[1];

            for (var i = 3; i < data.length; i++) {
                data[i - 3] = data[i];
            }

            data.length = data.length - 3;

            dwr.util.removeAllRows(this.results_table);

            if (hasNext) {
                this.nextDiv.style.display = '';
            } else {
                this.nextDiv.style.display = 'none';
            }

            if (hasPrevious) {
                this.previousDiv.style.display = '';
            } else {
                this.previousDiv.style.display = 'none';
            }

            var funcs = new Array();
            if (data.length <= 0) {
                funcs[0] = this._noResults();
                dwr.util.addRows(this.results_table, [this.headers], funcs, {
                    escapeHtml: false,
                });
                this.nextDiv.style.display = 'none';
                this.previousDiv.style.display = 'none';
                this._showMatchingResults(0);
                return;
            }

            this._fillResultsTable(this.headers, data);
            this._showMatchingResults(total);
        },

        _fillResultsTable: function (headers, data) {
            if (this.multiple == 'true') {
                this.relateDiv.style.display = '';
            }

            var table = this.results_table;

            //Filling Headers
            var row = table.insertRow(table.rows.length);

            // Add checkbox/select column header FIRST
            var cell = row.insertCell(row.cells.length);
            cell.setAttribute('class', 'beta');
            cell.setAttribute('className', 'beta');
            if (this.multiple == 'true') {
                cell.innerHTML = '<b>Select</b>';
            } else {
                cell.innerHTML = '<b></b>'; // Empty for single select
            }

            var cell = row.insertCell(row.cells.length);
            cell.setAttribute('class', 'beta');
            cell.setAttribute('className', 'beta');
            cell.setAttribute('style', 'text-align: center;');
            cell.innerHTML = '<td><b>Type</b></td>';

            for (var i = 0; i < headers.length; i++) {
                var header = headers[i];
                var cell = row.insertCell(row.cells.length);
                cell.innerHTML = this._getHeader(header);
                cell.setAttribute('class', 'beta');
                cell.setAttribute('className', 'beta');
            }

            var cell = row.insertCell(row.cells.length);
            cell.setAttribute('class', 'beta');
            cell.setAttribute('className', 'beta');
            cell.setAttribute('style', 'min-width:120px;');
            cell.innerHTML =
                '<b>' + this.availableLanguages[0]['title'] + '</b>';

            var style = document.createElement('style');
            style.type = 'text/css';
            style.innerHTML = `
             .selectMeRowInIframe { cursor: pointer; } 
             .selectMeRowInIframe:hover{background:#DFE8F6;}
             .listingTitleImg {
               border: 1px solid #ddd; 
               border-radius: 4px;  
               padding: 3px; 
               width: 64px; 
               cursor: pointer;
               max-height:100px
               overflow:hide;
             }
             `;
            document.getElementsByTagName('head')[0].appendChild(style);

            //Filling data
            for (var i = 0; i < data.length; i++) {
                var cellData = data[i];
                var row = table.insertRow(table.rows.length);
                row.id = 'rowId' + i;
                row.className = 'selectMeRowInIframe';
                if (this.multiple == 'true') {
                    row.setAttribute(
                        'onClick',
                        'javascript: contentSelector.toggleCheckbox(this)'
                    );
                }

                // Select button functionality
                var selected = function (scope, content) {
                    if (scope.useRelateContentOnSelect) {
                        scope._doRelateContent(content);
                    } else {
                        scope._onContentSelected(content);
                    }
                };
                if (this.multiple == 'false') {
                    var asset = cellData;
                    var selectRow = dojo.byId('rowId' + i);
                    if (selectRow.onclick == undefined) {
                        selectRow.onclick = dojo.hitch(
                            this,
                            selected,
                            this,
                            asset
                        );
                    }
                }

                // Add checkbox/select button cell FIRST
                var cell = row.insertCell(row.cells.length);
                cell.setAttribute('id', i);
                if (this.multiple == 'true') {
                    cell.innerHTML = this._checkButton(cellData);
                } else {
                    cell.innerHTML = this._selectButton(cellData);
                }

                var cell = row.insertCell(row.cells.length);
                var iconName = this._getIconName(cellData['__type__']);
                var hasTitleImage = cellData.hasTitleImage === 'true';

                cell.innerHTML = hasTitleImage
                    ? '<img class="listingTitleImg" onError="contentSelector._replaceWithIcon(this.parentElement, \'' +
                      iconName +
                      '\')" src="/dA/' +
                      cellData.inode +
                      '/titleImage/500w/50q" alt="' +
                      cellData['__title__'].replace(/[^A-Za-z0-9_]/g, ' ') +
                      '" >'
                    : '<span class="' +
                      iconName +
                      '" style="font-size:24px;width:auto;"></span>';

                cell.setAttribute('style', 'text-align: center;');

                for (var j = 0; j < this.headers.length; j++) {
                    var header = this.headers[j];
                    var cell = row.insertCell(row.cells.length);

                    if (header.fieldVelocityVarName === '__title__') {
                        cell.style.width = '50%';
                    }

                    var value = cellData[header['fieldVelocityVarName']];
                    if (value != null) cell.innerHTML = value;
                }

                for (var l = 0; l < this.availableLanguages.length; l++) {
                    if (
                        this.availableLanguages[l]['id'] ==
                        cellData['languageId']
                    ) {
                        var cell = row.insertCell(row.cells.length);
                        var flagUrl = this.availableLanguages[l]['countryCode'] ? this.availableLanguages[l]['languageCode'] +
                        '_' + this.availableLanguages[l]['countryCode'] : this.availableLanguages[l]['languageCode'];
                        var langStr =
                            '<img src="/html/images/languages/' +
                            flagUrl +
                            '.gif" width="16px" height="11px" />&nbsp;';
                            
                        cell.innerHTML =
                            langStr +
                            this.availableLanguages[l]['language'] +
                            '&nbsp;(' +
                            this.availableLanguages[l]['countryCode'] +
                            ')';
                    }
                }
            }

            //dojo.parser.parse("results_table_popup_menus");
            dojo.parser.parse(this.results_table);

            if (this.multiple == 'false') {
                data.map(asset => {
                    var selectButton = dojo.byId(
                        this.searchCounter + asset.inode
                    );
                    if (selectButton.onclick == undefined) {
                        selectButton.onclick = dojo.hitch(this, function (event) {
                            event.stopPropagation();
                            selected(this, asset);
                        });
                    }
                })
            }

            // Header based sorting functionality
            var headerClicked = function (scope, header) {
                const fieldStructureVarName =
                    header?.fieldStructureVarName || this.structureVelVar;
                if ('__title__' === header['fieldVelocityVarName']) {
                    scope._doSearch(1, fieldStructureVarName + '.title');
                } else if ('__type__' === header['fieldVelocityVarName']) {
                    scope._doSearch(
                        1,
                        fieldStructureVarName + '.structurename'
                    );
                } else {
                    scope._doSearch(
                        1,
                        fieldStructureVarName +
                            '.' +
                            header['fieldVelocityVarName']
                    );
                }
            };
            for (var i = 0; i < headers.length; i++) {
                var header = headers[i];
                var anchor = dojo.byId(
                    this.structureVelVar +
                        '.' +
                        header['fieldVelocityVarName'] +
                        'header'
                );
                if (anchor.onclick == undefined) {
                    anchor.onclick = dojo.hitch(
                        this,
                        headerClicked,
                        this,
                        header
                    );
                }
            }
        },

        toggleCheckbox: function (row) {
            row.querySelector('input[type="checkbox"]').click();
        },

        _noResults: function (data) {
            return (
                "<div class='noResultsMessage'>" +
                this.noResultsTextValue +
                '</div>'
            );
        },

        /**
         * Stub method that you can use dojo.connect to catch every time a user selects a content
         * @param {Object} content
         */
        onContentSelected: function (content) {},

        _onContentSelected: function (content) {
            this.onContentSelected(content);
            this._clearSearch();
            !isNg && this.dialog.hide();
        },

        _getHeader: function (field) {
            var fieldName = field['fieldName'];
            var fieldContentlet =
                this.structureVelVar + '.' + field['fieldVelocityVarName'];

            return (
                '<a class="beta" id="' +
                fieldContentlet +
                'header' +
                '"' +
                ' href="#"><b>' +
                fieldName +
                '</b></a>'
            );
        },

        _selectButton: function (data) {
            var inode = data['inode'];
            var buttonInode = this.searchCounter + inode;
            var button = dojo.replace(this.selectButtonTemplate, {
                buttonInode: buttonInode,
                selectButtonLabel: this.selectButtonLabel,
            });
            return button;
        },

        _checkButton: function (data) {
            var inode = data['inode'];
            var buttonInode = inode;
            var button = dojo.replace(this.checkBoxTemplate, {
                buttonInode: buttonInode,
            });
            button = this.selectedInodesSet.has(inode)
                ? button.replace('></input>', ' checked="true"></input>')
                : button;

            return button;
        },

        _showMatchingResults: function (num) {
            var div = this.matchingResultsDiv;
            div.style.visibility = 'visible';
            div.innerHTML =
                '<b> ' + this.matchResultsTextValue + ' (' + num + ')</b>';
        },

        _clearSearch: function () {
            dojo.empty(this.results_table);
            this.selectedInodesSet.clear();

            if (dijit.byId('langcombo+' + this.dialogCounter))
                dijit
                    .byId('langcombo+' + this.dialogCounter)
                    .set('displayedValue', '');

            for (var i = 0; i < this.categories.length; i++) {
                var mainCat = this.categories[i];
                var selectId =
                    mainCat['categoryName'].replace(/[^A-Za-z0-9_]/g, '') +
                    'Select' +
                    this.dialogCounter;
                var selectObj = document.getElementById(selectId);
                var options = selectObj.options;
                for (var j = 0; j < options.length; j++) {
                    var opt = options[j];
                    opt.selected = false;
                }
            }

            for (var h = 0; h < this.currentStructureFields.length; h++) {
                var field = this.currentStructureFields[h];
                var fieldId =
                    this.structureVelVar +
                    '.' +
                    field['fieldVelocityVarName'] +
                    'Field';
                var formField = document.getElementById(fieldId);
                if (formField == null) {
                    fieldId = fieldId + this.dialogCounter;
                    formField = document.getElementById(fieldId);
                }

                if (formField != null) {
                    if (
                        formField.type == 'select-one' ||
                        formField.type == 'select-multiple'
                    ) {
                        var options = formField.options;
                        for (var j = 0; j < options.length; j++) {
                            var opt = options[j];
                            opt.selected = false;
                        }
                    } else if (field['fieldFieldType'] == 'select') {
                        var obj = dijit.byId(fieldId);
                        obj.attr('displayedValue', 'None');
                    } else {
                        formField.value = '';
                        var temp = dijit.byId(formField.id);
                        if (temp == undefined) dojo.byId(formField.id);
                        if (temp != undefined) temp.reset();
                    }
                }
                if (field.fieldFieldType == 'host or folder') {
                    if (
                        dijit.byId('FolderHostSelector' + this.dialogCounter) !=
                        null
                    ) {
                        dijit
                            .byId('FolderHostSelector' + this.dialogCounter)
                            ._setValueAttr('');
                    }
                }
            }

            if (this.radiobuttonsIds[this.dialogCounter]) {
                for (
                    var i = 0;
                    i < this.radiobuttonsIds[this.dialogCounter].length;
                    i++
                ) {
                    var formField = document.getElementById(
                        this.radiobuttonsIds[this.dialogCounter][i]
                    );
                    if (formField != null && formField.type == 'radio') {
                        var values = '';
                        if (formField.checked) {
                            var temp = dijit.byId(formField.id);
                            temp.reset();
                        }
                    }
                }
            }

            if (this.checkboxesIds[this.dialogCounter]) {
                for (
                    var i = 0;
                    i < this.checkboxesIds[this.dialogCounter].length;
                    i++
                ) {
                    var formField = document.getElementById(
                        this.checkboxesIds[this.dialogCounter][i]
                    );
                    if (formField != null && formField.type == 'checkbox') {
                        if (formField.checked) {
                            var temp = dijit.byId(formField.id);
                            temp.reset();
                        }
                    }
                }
            }

            dwr.util.removeAllRows(this.results_table);
            this.nextDiv.style.display = 'none';
            this.previousDiv.style.display = 'none';
            this.relateDiv.style.display = 'none';
            this.generalSearch.set('value', '');

            this._hideMatchingResults();
        },

        _previousPage: function () {
            this._doSearch(this.currentPage - 1);
        },

        _nextPage: function () {
            this._doSearch(this.currentPage + 1);
        },

        _getIconName: function (iconCode) {
            var startIndex = iconCode.indexOf('<span class') + 13;
            var endIndex = iconCode.indexOf('</span>') - 2;
            return iconCode.substring(startIndex, endIndex);
        },

        _getCountryLabel: function (data) {
            return  data.language + (data.country === '' ? '' : ' - ' + data.country)
        },

        _replaceWithIcon: function (parentElement, iconName) {
            parentElement.innerHTML =
                '<span class="' +
                iconName +
                '" style="font-size:24px;width:auto;"></span>';
        },
    }
);
