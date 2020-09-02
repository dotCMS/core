dojo.provide("dotcms.dijit.RemotePublisherDialog");

dojo.require("dijit._Widget");
dojo.require("dijit.Dialog");
dojo.require("dijit.form.Button");

dojo.declare("dotcms.dijit.RemotePublisherDialog", null, {

    myId: "remotePublisherDia",
    title: "",
    admin: "",
    dateFilter: false,
    removeOnly: false,
    container: null,
    cats: false,
    restricted:false,
    workflow:{
        inode:null,
        actionId:null,
        publishDate:null,
        expireDate:null,
        structInode:null
    },

    show: function (isBulk) {
        var self = this;
        //Required clean up as these modals has duplicated widgets and collide without a clean up

        const url = this._buildUrl();

        var workflowPPDialog = dijit.byId("contentletWfDialog");
        if (workflowPPDialog) {
            workflowPPDialog.destroyRecursive();
        }

        var dia = dijit.byId(this.myId);
        if (dia) {
            dia.destroyRecursive();
        }
        dia = new dijit.Dialog({
            id: this.myId,
            title: this.title,
            href: url
        });

        //Verify if we need to display the date filtering box
        var dateFilter = this.dateFilter;
        var cats = this.cats;
        var restricted = this.restricted;
        var removeOnly = this.removeOnly;

        var container = this.container;

        var connection = dojo.connect(dia, "onLoad", function () {

            dojo.disconnect(connection);
            var hasCondition = (dojo.byId("hasCondition") ? dojo.byId("hasCondition").value : "");
            if (hasCondition === 'true') {
                var actionId = self.workflow.actionId;
                container.evaluateCondition(actionId, this.title);
                self.hide();
                return;
            }

            var filterDiv = dojo.byId("filterTimeDiv");

            if (filterDiv) {
                if (dateFilter) {
                    filterDiv.style.display = "";
                } else {
                    filterDiv.style.display = "none";
                }
            }

            if (cats || restricted) {
                dijit.byId("iwtExpire").set("disabled", true);
                dijit.byId("iwtPublishExpire").set("disabled", true);
            }
            // For archived Content, allow users to "Push" and "Push Remove" ONLY
            if (removeOnly) {
                dijit.byId("iwtPublish").set("checked", true);
                dijit.byId("iwtPublishExpire").set("disabled", true);
                dijit.byId("iwtExpire").set("disabled", false);
            }

            dojo.connect(dijit.byId("iwtExpire"), "onChange", function () {
                container.togglePublishExpireDivs();
            });

            dojo.connect(dijit.byId("iwtPublish"), "onChange", function () {
                container.togglePublishExpireDivs();
            });

            dojo.connect(dijit.byId("iwtPublishExpire"), "onChange", function () {
                container.togglePublishExpireDivs();
            });

            dojo.connect(dijit.byId("environmentSelect"), "onChange", function () {
                container.addSelectedToWhereToSend();
            });

            dojo.connect(dijit.byId("remotePublishSaveButton"), "onClick", function () {
                container.remotePublish();
            });

            dojo.connect(dijit.byId("remotePublishCancelButton"), "onClick", function () {
                var lastSelectedEnvironments = JSON.parse(sessionStorage.getItem("lastSelectedEnvironments"));
                lastSelectedEnvironments = container.inialStateEnvs;
                sessionStorage.setItem("lastSelectedEnvironments", JSON.stringify(lastSelectedEnvironments));
                container.inialStateEnvs = [];
                self.hide();
            });

            var environmentSelect = dijit.byId("environmentSelect");
            if (environmentSelect) {
                environmentSelect.set('store', container.environmentStore);
                environmentSelect.searchAttr = 'name';
                environmentSelect.displayedValue = '0';
                environmentSelect.startup();
            }

            var assignSelect = dijit.byId("taskAssignmentAux");
            if (assignSelect) {
                assignSelect.set('store', container.roleReadStore);
                assignSelect.startup();
            }

        });

        dojo.connect(dia, "onDownloadEnd", function () {
            var lastSelectedEnvironments = JSON.parse(sessionStorage.getItem("lastSelectedEnvironments"));
            if (lastSelectedEnvironments) {
                for (var count = 0; count < lastSelectedEnvironments.length; count++) {
                    container.addToWhereToSend(lastSelectedEnvironments[count].id, lastSelectedEnvironments[count].name);
                    container.inialStateEnvs[count] = {
                        name: lastSelectedEnvironments[count].name,
                        id: lastSelectedEnvironments[count].id
                    };
                }
                container.refreshWhereToSend();
            }
        });

        dojo.connect(dia, "onClose", function () {
            container.clear();
        });
        dojo.connect(dia, "onHide", function () {
            container.clear();
        });

        if (this._hasWorkflow()) {
            this._getWorkFLow(this.workflow.actionId).then((action) => {
                if ( action.assignable || action.commentable || isBulk){
                    this._dispatchAngularWorkflowEvent(action);
                    dia.set(url);
                    dia.show();
                } else {
                    this._dispatchAngularDialogEvent();
                }
            });
        } else {
            this._dispatchAngularDialogEvent();
        }
    },

    hide: function () {

        var dia = dijit.byId(this.myId);
        if (dia) {
            dia.hide();
            dia.destroyRecursive();
        }
    },

    _buildUrl: function () {

        const baseUrl = "/html/portlet/ext/remotepublish/remote_publish_dialog.jsp";
        var workflow = this.workflow;
        if(workflow) {

            var urlParams = '';

            if (workflow.actionId) {
                urlParams = '?actionId=' + workflow.actionId;
            }

            if (workflow.inode) {
                if (urlParams) {
                    urlParams = urlParams + '&inode=' + workflow.inode;
                } else {
                    urlParams = urlParams + '?inode=' + workflow.inode;
                }
            }

            if (workflow.publishDate) {
                if (urlParams) {
                    urlParams = urlParams + '&publishDate='
                        + workflow.publishDate;
                } else {
                    urlParams = urlParams + '?publishDate='
                        + workflow.publishDate;
                }
            }

            if (workflow.expireDate) {
                if (urlParams) {
                    urlParams = urlParams + '&expireDate='
                        + workflow.expireDate;
                } else {
                    urlParams = urlParams + '?expireDate='
                        + workflow.expireDate;
                }
            }

            if (workflow.structInode) {
                if (urlParams) {
                    urlParams = urlParams + '&structInode='
                        + workflow.structInode;
                } else {
                    urlParams = urlParams + '?structInode='
                        + workflow.structInode;
                }
            }
        }
        return baseUrl + (urlParams !== undefined ? urlParams : '');
    },

    _hasWorkflow: function () {
        return this.workflow.inode  || this.workflow.actionId || this.workflow.structInode
    },

    _getWorkFLow: function (action) {
        return fetch(`/api/v1/workflow/actions/${action}`)
            .then(response => response.json())
            .then(data => data.entity)
            .catch(() => []);
    },

    _dispatchAngularDialogEvent: function () {
        const eventData = {
            assetIdentifier: this.container.assetIdentifier || this.workflow.inode,
            dateFilter: this.dateFilter,
            isBundle: this.container.isBundle,
            removeOnly: this.removeOnly,
            restricted: this.restricted,
            cats: this.cats,
            title: this.title
        };

        if (this.workflow && this.workflow.actionId) {
            this.container.evaluateCondition(this.workflow.actionId, this.title, eventData);
        } else {
            const customEvent = document.createEvent("CustomEvent");
            customEvent.initCustomEvent("ng-event", false, false,  {
                name: "push-publish",
                data: eventData
            });
            document.dispatchEvent(customEvent);
        }
    },

    _dispatchAngularWorkflowEvent: function (workflow) {
        debugger;
        const data = {
            workflow: workflow,
            callback: 'angularWorkflowEventCallback',
            inode: this.workflow.inode,
        };
        if (typeof getSelectedInodes === "function") {
            data['selectedInodes'] = getSelectedInodes();
        }

        const customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "workflow-wizard",
            data: data
        });
        document.dispatchEvent(customEvent);
    }


});
