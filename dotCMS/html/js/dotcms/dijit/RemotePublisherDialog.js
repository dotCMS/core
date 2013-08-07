dojo.provide("dotcms.dijit.RemotePublisherDialog");

dojo.require("dijit._Widget");
dojo.require("dijit.Dialog");
dojo.require("dijit.form.Button");

dojo.declare("dotcms.dijit.RemotePublisherDialog", null, {

    myId: "remotePublisherDia",
    title: "",
    admin: "",
    dateFilter: false,
    container: null,
    cats: false,

    show: function () {

        var dia = dijit.byId(this.myId);
        if (dia) {
            dia.destroyRecursive();
        }
        dia = new dijit.Dialog({
            id: this.myId,
            title: this.title,
            href: "/html/portlet/ext/remotepublish/remote_publish_dialog.jsp"
        });

        //Verify if we need to display the date filtering box
        var dateFilter = this.dateFilter;
        var cats = this.cats;
        var connection = dojo.connect(dia, "onLoad", function () {
            dojo.disconnect(connection);

            var filterDiv = dojo.byId("filterTimeDiv");
            if (dateFilter) {
                filterDiv.style.display = "";
            } else {
                filterDiv.style.display = "none";
            }

            if(cats) {
            	dijit.byId("iwtExpire").set("disabled", true) ;
            	dijit.byId("iwtPublishExpire").set("disabled", true) ;
            }

        });

        var container = this.container;
        dojo.connect(dia, "onDownloadEnd", function () {

            if (window.lastSelectedEnvironments) {
                for (var count = 0; count < window.lastSelectedEnvironments.length; count++) {
                	container.addToWhereToSend(window.lastSelectedEnvironments[count].id, window.lastSelectedEnvironments[count].name);
                	container.inialStateEnvs[count] = {name: window.lastSelectedEnvironments[count].name, id: window.lastSelectedEnvironments[count].id};
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

        dia.set("href", "/html/portlet/ext/remotepublish/remote_publish_dialog.jsp");

        dia.show();
    },

    hide: function () {

        var dia = dijit.byId(this.myId);
        if (dia) {
            dia.hide();
            dia.destroyRecursive();
        }
    }

});