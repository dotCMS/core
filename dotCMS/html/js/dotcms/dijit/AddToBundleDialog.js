dojo.provide("dotcms.dijit.AddToBundleDialog");

dojo.require("dijit._Widget");
dojo.require("dijit.Dialog");
dojo.require("dijit.form.Button");

dojo.declare("dotcms.dijit.AddToBundleDialog", null, {

    myId: "addToBundleDia",
    title: "",
    admin: "",

    show: function () {

        var dia = dijit.byId(this.myId);
        if (dia) {
            dia.destroyRecursive();
        }
        dia = new dijit.Dialog({
            id: this.myId,
            title: this.title,
            href: "/html/portlet/ext/remotepublish/add_to_bundle_dialog.jsp"
        });

        //Verify if we need to display the date filtering box
        var dateFilter = this.dateFilter;
        var connection = dojo.connect(dia, "onLoad", function () {
        	dojo.disconnect(connection);

        	var filterDiv = dojo.byId("filterTimeDiv_atb");
        	if (dateFilter) {
        		filterDiv.style.display = "";
        	} else {
        		filterDiv.style.display = "none";
        	}
        });

        dojo.connect(dia, "onDownloadEnd", function () {
            if (window.lastSelectedBundle && window.lastSelectedBundle.id) {
                dojo.byId("bundleName").value = window.lastSelectedBundle.name;
                dijit.byId('bundleSelect').set('value', window.lastSelectedBundle.name);
            }
        });

        dia.set("href", "/html/portlet/ext/remotepublish/add_to_bundle_dialog.jsp");
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