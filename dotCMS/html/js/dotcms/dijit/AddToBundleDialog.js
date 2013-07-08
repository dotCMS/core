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