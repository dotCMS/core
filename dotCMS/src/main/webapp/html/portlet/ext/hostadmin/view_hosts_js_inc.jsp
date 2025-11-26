<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="java.net.URLDecoder"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="java.util.List"%>

<%

	Structure hostStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
	Language lang = APILocator.getLanguageAPI().getDefaultLanguage();

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/hostadmin/view_hosts"});
	String referer = URLDecoder.decode(PortletURLUtil.getRenderURL(request,WindowState.MAXIMIZED.toString(),params));

	params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/hostadmin/view_hosts"});
	params.put("cmd", new String[] { "copyHost" });
	params.put("copy_from_host_id", new String[] { "{copyFromHostId}" });
	params.put("copy_all", new String[] { "{copyAll}" });
	params.put("copy_templates_containers", new String[] { "{copyTemplatesAndContainers}" });
	params.put("copy_content_on_pages", new String[] { "{copyContentOnPages}" });
	params.put("copy_folders", new String[] { "{copyFolders}" });
	params.put("copy_content_on_host", new String[] { "{copyContentOnHost}" });
	params.put("copy_links", new String[] { "{copyLinks}" });
	params.put("copy_host_variables", new String[] { "{copyHostVariables}" });
	params.put("copy_tag_storage", new String[] { "{copyTagStorage}" });

	String copyHostReferer = URLDecoder.decode(PortletURLUtil.getRenderURL(request,WindowState.MAXIMIZED.toString(),params));

%>

<script type="text/javascript">

    dojo.require('dojo.data.ItemFileReadStore');
	dojo.require('dijit.form.FilteringSelect');
    dojo.require('dijit.form.Form');
    dojo.require('dijit.ProgressBar');
    dojo.require("dotcms.dojo.push.PushHandler");

    var isCMSAdmin = <%= isCMSAdmin %>;

	//I18n messages
	var deleteConfirmMessage = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-host-confirm")) %>';
	var unarchiveConfirmMessage = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unarchive-host-confirm")) %>';
	var archiveConfirmMessage = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "archive-host-confirm")) %>';
	var unpublishConfirmMessage = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unpublish-host-confirm")) %>';
	var publishConfirmMessage = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publish-host-confirm")) %>';
	var makeDefaultConfirmMessage = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "make-default-host-confirm")) %>';

	var enterprise = <%=LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level%>;

	<%PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
		List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();%>
	var sendingEndpoints = <%=UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty()%>;

	var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');


    function editContentletEvent(contInode) {
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "edit-host",
            data: {
                inode: contInode
            }
        });
        document.dispatchEvent(customEvent);
    }

    function createContentlet(url) {
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "create-host",
            data: {
                url
            }
        });
        document.dispatchEvent(customEvent);

        var dialog = dijit.byId("addHostDialog");

        if (dialog) {
            dialog.hide();
        }
    }

    function refreshHostTable() {
        hostAdmin.refreshHostTable();
    }


    //Object responsible of the interactions with the listing page -->
    dojo.declare("HostAdmin", null, {

		viewHostsReferer: '<%= referer %>',

		copyHostOptions: 'copy_from_host_id:{copyFromHostId};copy_all:{copyAll};copy_templates_containers:{copyTemplatesAndContainers};copy_content_on_pages:{copyContentOnPages};copy_folders:{copyFolders};copy_content_on_host:{copyContentOnHost};copy_content_types:{copyContentTypes};copy_links:{copyLinks};copy_host_variables:{copyHostVariables};copy_tag_storage:{copyTagStorage}',

		newHostURL: '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">\
							<portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" />\
							<portlet:param name="cmd" value="new" />\
							<portlet:param name="selectedStructure" value="<%= hostStructure.getInode() %>" />\
							<portlet:param name="lang" value="<%= Long.toString(lang.getId()) %>" />\
					  </portlet:actionURL>',

		editHostURL: '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">\
							<portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" />\
							<portlet:param name="cmd" value="edit" />\
							<portlet:param name="selectedStructure" value="<%= hostStructure.getInode() %>" />\
							<portlet:param name="referer" value="<%=referer%>" />\
					  </portlet:actionURL>&inode={hostInode}',

        tableHeaderRowTemplate: '<tr>\
    				<th>{hostNameHeader}</th>\
    				{headerColumns}\
    			 </tr>',

        tableHeaderColumnTemplate: '<th>{fieldName}</th>',

        tableRowTemplate: '<tr id="row{identifier}">\
            		<td width="25%">\
            			<div class="host-list__item" id="hostName{identifier}">\
            				<span class="{imgSrc}"></span>\
            				<a href="javascript: editContentletEvent(\'{workingInode}\')">{hostName}</a>\
                            <div class="host-list__item-loader">\
                                <div dojoType="dijit.ProgressBar" style="width:100px; display:none;" jsId="{identifier}SetupProgress" id="{identifier}SetupProgress">\
                                </div>\
                            </div>\
                        </div>\
            		</td>\
    				{rowColumns}\
    		    </tr>',

        tableRowColumnTemplate: '	<td>{value}</td>',

        tableRowMenuTemplate: '<div dojoType="dijit.Menu" class="dotContextMenu" contextMenuForWindow="false" style="display: none;" targetNodeIds="row{identifier}">\
                    {menuesHTML}\
                </div>',

		tableEditRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="editContentletEvent(\'{workingInode}\')">\
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>\
                     </div>\
                     <div dojoType="dijit.MenuItem" iconClass="editScriptIcon" onClick="hostAdmin.editHostVariables(\'{identifier}\')">\
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Host-Variables")) %>\
                  	 </div>',

		tablePublishRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="hostIcon" onClick="hostAdmin.publishHost(\'{identifier}\')">\
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Start-Host")) %>\
                    </div>',

		tableUnpublishRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="hostAdmin.unpublishHost(\'{identifier}\')">\
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Stop-Host")) %>\
                    </div>',

		tableMakeDefaultRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="hostDefaultIcon" onClick="hostAdmin.makeDefault(\'{identifier}\')">\
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Make-Default")) %>\
                    </div>',

		tableArchiveRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="archiveIcon" onClick="hostAdmin.archiveHost(\'{identifier}\')">\
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive-Host")) %>\
                    </div>',

		tableUnarchiveRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="unarchiveIcon" onClick="hostAdmin.unarchiveHost(\'{identifier}\')">\
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unarchive-Host")) %>\
                    </div>',

		tableDeleteRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="hostAdmin.deleteHost(\'{identifier}\')">\
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Host")) %>\
                    </div>',

        tableResultSummaryTemplate: '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Viewing-Results")) %> {startRecord} <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "to")) %> \
            {endRecord} <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "of")) %> {total}',


        tableLoadingTemplate:
                 '<div id="loadingContentListing" name="loadingContentListing" align="center" style=""> <br /><br /><font class="bg" size="2"> <b>\
                 <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Loading")) %>\
                 </b> <br /><img src="/html/images/icons/processing.gif" /></font> <br /> <br />\
	              </div>',

         tablePushPublishRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="sServerIcon" onClick="hostAdmin.remotePublish(\'{identifier}\')">\
	                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>\
	               </div>',

         tableAddToBundleRowMenuTemplate:
					'<div dojoType="dijit.MenuItem" iconClass="bundleIcon" onClick="hostAdmin.addToBundle(\'{identifier}\')">\
	                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>\
	               </div>',

        constructor: function(){
            this.currentPage = 1;
            this.RESULTS_PER_PAGE = 20;
        },
        openAddHostDialog: function(){
        	dojo.style('addHostDialog', {width: '450px'});
            dojo.style('addHostStep1', {
                display: ''
            });
            dojo.style('addHostStep2', {
                display: 'none'
            });
            dijit.byId('addHostDialog').show();
        },
        cancelCreateHost: function(){
            dijit.byId('addHostDialogForm').reset();
            dijit.byId('addHostDialog').hide();
        },
        goToStep2: function(){

            if (dijit.byId('startBlankHostRadio').attr('value')) {
                this.gotoCreateHost();
            }
            else {
				HostAjax.findAllHostThumbnails(dojo.hitch(this, this.goToStep2Callback));
            }

        },
		goToStep2Callback: function (thumbnails) {

			this.hostThumbnails = thumbnails;

			var hostsList = [];
			for(var i = 0; i < thumbnails.length; i++) {
				var host = thumbnails[i];
				hostsList.push({ id: host.hostId, name: host.hostName, tagStorage: host.tagStorage });
			}

	        var hostsStore = new dojo.data.ItemFileReadStore({
            	data: {
					identifier: 'id',
					label: 'name',
					items: hostsList
				}
        	});

			dijit.registry.remove('hostToCopy');
			dijit.registry.remove('id');
			dijit.registry.remove('id_popup');
			dojo.byId('hostToCopyWrapper').innerHTML = '<div id="hostToCopy"></div>';

			this.hostsSelect = new dijit.form.FilteringSelect({
				 id: "id",
				 name: "name",
				 value: thumbnails[0].hostId,
				 onChange: dojo.hitch(this, this.hostChanged),
				 store: hostsStore,
				 searchAttr: "name",
				 labelAttr: "name"
			},
			dojo.byId("hostToCopy"));

			dojo.byId('websitePreviewLink').href = "/html/portlet/ext/common/page_preview_popup.jsp?hostname=" + thumbnails[0].hostName + "&host_id=" + thumbnails[0].hostId;
            dojo.byId('websitePreviewLink').innerText = thumbnails[0].hostName;

            if(thumbnails[0].hasThumbnail) {
				dojo.style('hostThumbnailWrapper', {display: ''});
				dojo.byId('hostThumbnail').src ='/contentAsset/image-thumbnail/' + thumbnails[0].hostInode + '/hostThumbnail?byInode=true&w=200&h=200';
            } else
            	dojo.style('hostThumbnailWrapper', {display: 'none'});

            dojo.style('addHostStep1', {
                display: 'none'
            });
            dojo.style('addHostStep2', {
                display: ''
            });
            dojo.style('addHostDialog', {width: '800px'});
            document.getElementById('copyTagStorage').value=thumbnails[0].tagStorage;
		},
        goToStep1: function(){
            dojo.style('addHostStep1', {
                display: ''
            });
            dojo.style('addHostStep2', {
                display: 'none'
            });
            dojo.style('addHostDialog', {width: '450px'});
        },
        copyAllChanged: function(){
            if (dijit.byId('copyAll').attr('value')) {
                dojo.query('#otherCheckboxesWrapper input[type$="checkbox"]').forEach(function(checkbox){
                    dijit.byId(checkbox.id).attr('value', 'on');
                    dijit.byId(checkbox.id).attr('disabled', true);
                });
            }
            else {
                dojo.query('#otherCheckboxesWrapper input[type$="checkbox"]').forEach(function(checkbox){
                    dijit.byId(checkbox.id).attr('value', 'on');
                    dijit.byId(checkbox.id).attr('disabled', false);
                });
            }
        },
        gotoCreateHost: function(){
			var url = this.newHostURL;

			if(dijit.byId('startBlankHostRadio').attr('value')) {
				url += "&referer=" + escape(this.viewHostsReferer);
				createContentlet(url);
			} else {

				var copyHostOptions = escape(dojo.replace(this.copyHostOptions,
					{
						copyFromHostId: this.hostsSelect.attr('value'),
						copyAll: dijit.byId('copyAll').attr('value'),
						copyTemplatesAndContainers: dijit.byId('copyTemplatesAndContainers').attr('value'),
						copyContentOnPages: dijit.byId('copyContentOnPages').attr('value'),
						copyFolders: dijit.byId('copyFolders').attr('value'),
						copyContentOnHost: dijit.byId('copyContentOnHost').attr('value'),
						copyContentTypes: dijit.byId('copyContentTypes').attr('value'),
						copyLinks: dijit.byId('copyLinks').attr('value'),
						copyHostVariables: dijit.byId('copyHostVariables').attr('value'),
						copyTagStorage: document.getElementById('copyTagStorage').value
					}));
				url += "&_copyOptions=" + copyHostOptions + "&referer=" + escape(this.viewHostsReferer);
				createContentlet(url);
			}
        },
        hostChanged: function(){

			var hostId = this.hostsSelect.attr('value');
			for(var i = 0; i < this.hostThumbnails.length; i++) {
				if(this.hostThumbnails[i].hostId == hostId) {

					dojo.byId('websitePreviewLink').href = "/html/portlet/ext/common/page_preview_popup.jsp?hostname=" + this.hostThumbnails[i].hostName + "&host_id=" + hostId;
		            dojo.byId('websitePreviewLink').innerText = this.hostThumbnails[i].hostName;

		            if(this.hostThumbnails[i].hasThumbnail) {
						dojo.style('hostThumbnailWrapper', {display: ''});
						dojo.byId('hostThumbnail').src ='/contentAsset/image-thumbnail/' + this.hostThumbnails[i].hostInode + '/hostThumbnail?byInode=true&w=200&h=200';
		            } else
		            	dojo.style('hostThumbnailWrapper', {display: 'none'});

		            document.getElementById('copyTagStorage').value = this.hostThumbnails[i].tagStorage;
				}
			}


        },
        refreshHostTable: function(){
            var filter = dijit.byId('filter').attr('value');
            var showDeleted = dijit.byId('showDeleted').attr('checked');
            var offset = (this.currentPage - 1) * this.RESULTS_PER_PAGE;
            var count = this.RESULTS_PER_PAGE;
            HostAjax.findHostsPaginated(filter, showDeleted, offset, count, dojo.hitch(this, this.refreshHostTableCallback));
        },
        refreshHostTableCallback: function(data){
            //Clearing up the table
            DWRUtil.removeAllRows(dojo.byId('hostsTableHeader'));
            DWRUtil.removeAllRows(dojo.byId('hostsTableBody'));
            dojo.byId('hostContextMenues').innerHTML = '';

            //Data variables
            var total = data.total;
            var list = data.list;
            var structure = data.strucuture;
            var fields = data.fields;

            //Adding the headers to the table
            var hostNameField;
            var columnsHTML = '';
            var listedFields = [];

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.fieldVelocityVarName == 'hostName') {
                    hostNameField = field;
                    continue;
                }

                if (field.fieldListed) {
                    columnsHTML += dojo.replace(this.tableHeaderColumnTemplate, field);
                    listedFields.push(field);
                }
            }
            var headersHTML = dojo.replace(this.tableHeaderRowTemplate, {
                hostNameHeader: hostNameField.fieldName,
                headerColumns: columnsHTML
            });
            dojo.place(headersHTML, 'hostsTableHeader', 'last');

            //Adding the hosts to the table
            for (var i = 0; i < list.length; i++) {

                var content = list[i];
                var rowColumnsHTML = '';

                for (var j = 0; j < listedFields.length; j++) {
                    var field = listedFields[j];
                    var value = content[field.fieldVelocityVarName] == null?'':content[field.fieldVelocityVarName];
                    if(field.fieldVelocityVarName == 'aliases')
                        value = value.replace(/\n/g, '<br/>');
                    rowColumnsHTML += dojo.replace(this.tableRowColumnTemplate, {
                        value: value == null?"":value
                    });
                }

                if(content.isDefault && content.live)
                    content.imgSrc = 'hostDefaultIcon';
                else if (content.live)
                    content.imgSrc = 'hostIcon';
                else if(content.archived)
					content.imgSrc = 'hostArchivedIcon';
                else
                    content.imgSrc = 'hostStoppedIcon';
                if(content.isDefault)
                	content.hostName = '<b>' + content[hostNameField.fieldVelocityVarName] + ' (<%= LanguageUtil.get(pageContext, "Default") %>)</b>';
                else
                	content.hostName = content[hostNameField.fieldVelocityVarName];
                content.rowColumns = rowColumnsHTML;

                var rowHTML = dojo.replace(this.tableRowTemplate, content);
                dojo.place(rowHTML, 'hostsTableBody', 'last');

                //Checking if host is in setup process
                dijit.registry.remove(content.identifier + "SetupProgress");
                dojo.parser.parse("row" + content.identifier);
                if(content.hostInSetup) {
                	dojo.attr(content.identifier + "SetupProgress",'style','display:block;width:100px;');
                    setTimeout(dojo.hitch(this, this.checkSetupProgress, content.identifier), 1000);
                }

                //Checking the status and permission to stop, start, edit and delete a host
				var menuesHTML = '';
                if (dojo.indexOf(content.userPermissions, 2) > 0  && content.archived == false) {
                    menuesHTML += dojo.replace(this.tableEditRowMenuTemplate, content);
                }

                if (content.live == false && content.archived == false && dojo.indexOf(content.userPermissions, 4) > 0) {
                    menuesHTML += dojo.replace(this.tablePublishRowMenuTemplate, content);
                }

                if (content.live == true && content.archived == false && dojo.indexOf(content.userPermissions, 4) > 0) {
                    menuesHTML += dojo.replace(this.tableUnpublishRowMenuTemplate, content);
                }

                if (isCMSAdmin && !content.isDefault && content.live == false && content.archived == false && dojo.indexOf(content.userPermissions, 2) > 0) {
                    menuesHTML += dojo.replace(this.tableArchiveRowMenuTemplate, content);
                }

                if (!content.isDefault && content.live == false && content.archived == true && dojo.indexOf(content.userPermissions, 2) > 0) {
                    menuesHTML += dojo.replace(this.tableUnarchiveRowMenuTemplate, content);
                }

                if (!content.isDefault && content.live == false && content.archived == true && dojo.indexOf(content.userPermissions, 2) > 0) {
                    menuesHTML += dojo.replace(this.tableDeleteRowMenuTemplate, content);
                }

                if (!content.isDefault && content.archived == false) {
                    menuesHTML += dojo.replace(this.tableMakeDefaultRowMenuTemplate, content);
                }

                if (dojo.indexOf(content.userPermissions, 4) > 0 && enterprise && sendingEndpoints) {
                    menuesHTML += dojo.replace(this.tablePushPublishRowMenuTemplate, content);
                }

                if (dojo.indexOf(content.userPermissions, 4) > 0 && enterprise ) {
                    menuesHTML += dojo.replace(this.tableAddToBundleRowMenuTemplate, content);
                }


                var rowMenuHTML = dojo.replace(this.tableRowMenuTemplate, { menuesHTML: menuesHTML, identifier: content.identifier });
                dojo.place(rowMenuHTML, 'hostContextMenues', 'last');

            };
            dojo.parser.parse('hostContextMenues');

            //rendering the no results message
            if (total == 0)
                dojo.style('noResultsSection', {
                    display: ''
                });
            else
                dojo.style('noResultsSection', {
                    display: 'none'
                });

            //Rendering the results summary bottom section of the table
            var startRecord = (this.currentPage - 1) * this.RESULTS_PER_PAGE + 1;
            if (startRecord > total)
                startRecord = total;
            var endRecord = startRecord + this.RESULTS_PER_PAGE - 1;
            if (endRecord > total)
                endRecord = total;

            var summaryHTML = dojo.replace(this.tableResultSummaryTemplate, {
                startRecord: startRecord,
                endRecord: endRecord,
                total: total
            });
            dojo.byId('resultsSummary').innerHTML = summaryHTML;

            //Rendering the next and previous buttons
            dojo.style('buttonNextResultsWrapper', {
                visibility: 'hidden'
            });
            dojo.style('buttonPreviousResultsWrapper', {
                visibility: 'hidden'
            });
            if (endRecord < total)
                dojo.style('buttonNextResultsWrapper', {
                    visibility: 'visible'
                });
            if (startRecord > 1)
                dojo.style('buttonPreviousResultsWrapper', {
                    visibility: 'visible'
                });

            if (total == 0)
                dojo.style('resultsSummary', {
                    visibility: 'hidden'
                })
            else
                dojo.style('resultsSummary', {
                    visibility: 'visible'
                })

        },
        checkSetupProgress: function(hostIdentifier){
			HostAjax.getHostSetupProgress(hostIdentifier, dojo.hitch(this, this.checkSetupProgressCallback, hostIdentifier));
        },
        checkSetupProgressCallback: function(hostIdentifier, progress){
			if(progress < 0 || progress >= 100)
				dojo.attr(hostIdentifier + "SetupProgress",'style','display:none');
			else {
				dijit.byId(hostIdentifier + "SetupProgress").update({ progress: progress });
                setTimeout(dojo.hitch(this, this.checkSetupProgress, hostIdentifier), 2500);
				dojo.fadeOut({
					node: dojo.byId('hostName' + hostIdentifier),
					duration: 1000,
					onEnd: function () {
						dojo.fadeIn({ node: dojo.byId('hostName' + hostIdentifier), duration: 1000 }).play();
					}
				}).play();
			}
        },
        filterHosts: function(){
        	DWRUtil.removeAllRows(dojo.byId('hostsTableHeader'));
		    DWRUtil.removeAllRows(dojo.byId('hostsTableBody'));
		    dojo.byId('resultsSummary').innerHTML = this.tableLoadingTemplate;
            clearTimeout(this.filterHandler);
            this.filterHandler = setTimeout(dojo.hitch(this, this.refreshHostTable), 700);
        },
        clearFilter: function(){
        	DWRUtil.removeAllRows(dojo.byId('hostsTableHeader'));
		    DWRUtil.removeAllRows(dojo.byId('hostsTableBody'));
		    dojo.byId('resultsSummary').innerHTML = this.tableLoadingTemplate;
        	dijit.byId('showDeleted').attr('checked', false);
            dijit.byId('filter').attr('value', '');
            this.refreshHostTable();
        },
        gotoNextPage: function(){
            this.currentPage++;
            this.refreshHostTable();
        },
        gotoPreviousPage: function(){
            this.currentPage--;
            this.refreshHostTable();
        },
		editHost: function (id, inode) {
			var href = dojo.replace(this.editHostURL, { hostInode: inode});
			window.location=href;
		},
		editHostVariables: function (id) {
			hostVariablesAdmin.showHostVariables(id);
		},
		publishHost: function (id) {
			if(confirm(publishConfirmMessage)) {
				HostAjax.publishHost(id, dojo.hitch(this, this.refreshHostTable));
			}
		},
		remotePublish: function (id) {
			pushHandler.showDialog(id);
		},
		addToBundle: function (id) {
			pushHandler.showAddToBundleDialog(id, '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');
		},
		unpublishHost: function (id) {
			if(confirm(unpublishConfirmMessage)) {
				HostAjax.unpublishHost(id, dojo.hitch(this, this.refreshHostTable));
			}
		},
		archiveHost: function (id) {
			if(confirm(archiveConfirmMessage)) {
				HostAjax.archiveHost(id, dojo.hitch(this, this.refreshHostTable));
			}
		},
		unarchiveHost: function (id) {
			if(confirm(unarchiveConfirmMessage)) {
				HostAjax.unarchiveHost(id, dojo.hitch(this, this.refreshHostTable));
			}
		},
		deleteHost: function (id) {
			if(confirm(deleteConfirmMessage)) {
				DWRUtil.removeAllRows(dojo.byId('hostsTableHeader'));
			    DWRUtil.removeAllRows(dojo.byId('hostsTableBody'));
			    dojo.byId('resultsSummary').innerHTML = this.tableLoadingTemplate;
			    setTimeout(dojo.hitch(this, this.refreshAfterDelete, id), 1000);
				HostAjax.deleteHost(id, dojo.hitch(this, this.refreshHostTable));
			}
		},
        refreshAfterDelete: function(identifier){
            var filter = dijit.byId('filter').attr('value');
            var showDeleted = dijit.byId('showDeleted').attr('checked');
            var offset = (this.currentPage - 1) * this.RESULTS_PER_PAGE;
            var count = this.RESULTS_PER_PAGE;
            var callMetaData = {
            		  callback:dojo.hitch(this, this.refreshAfterDeleteCallback),
            		  arg: identifier
            		};
            HostAjax.findHostsPaginated(filter, showDeleted, offset, count, callMetaData);
        },
        refreshAfterDeleteCallback: function(data, identifier){
            var list = data.list;
			var isDeletionDone = true;
            for (var i = 0; i < list.length; i++) {
                if(list[i]["identifier"] == identifier)
                	isDeletionDone = false;
            }
            if(isDeletionDone){
            	setTimeout(dojo.hitch(this, this.refreshHostTable), 200);
            }else{
            	dojo.attr(identifier + "SetupProgress",'innerHTML','<%= LanguageUtil.get(pageContext, "Processing") %> . . . .');
            	dojo.attr(identifier + "SetupProgress",'style','display:block;width:100px;');
				dojo.fadeOut({
					node: dojo.byId('hostName' + identifier),
					duration: 1000,
					onEnd: function () {
						dojo.fadeIn({ node: dojo.byId('hostName' + identifier), duration: 1000 }).play();
					}
				}).play();
            	setTimeout(dojo.hitch(this, this.refreshAfterDelete, identifier), 2000);
            }
        },
		makeDefault: function (id) {
			if(confirm(makeDefaultConfirmMessage)) {
				DWRUtil.removeAllRows(dojo.byId('hostsTableHeader'));
			    DWRUtil.removeAllRows(dojo.byId('hostsTableBody'));
			    dojo.byId('resultsSummary').innerHTML = this.tableLoadingTemplate;
				HostAjax.makeDefault(id, dojo.hitch(this, this.refreshHostTable));
			}
		}

    });

    dojo.addOnLoad(dojo.hitch(this, function(){
        hostAdmin = new HostAdmin();
        hostAdmin.refreshHostTable();
    }))

</script>
