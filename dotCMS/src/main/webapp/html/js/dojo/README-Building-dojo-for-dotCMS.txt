Building dojo for the dotCMS

To get the dojo source we keep a copy in https://svn.dotcms.org/thirdparty/dojo.  Any changes to themes will be lost if this
repository is not updated with the latest themes before building. Check out and verify.

1) 	make sure you have dojo's source code tree (it will have a ./util folder in it).  Place the dojo
	source dir into ./dotCMS/html/js/dojo/src/, e.g. ./dotCMS/html/js/dojo/src/dojo-release-1.4.1-src

2) 	have java and ant on your path

3) 	have a dotcms.profile.js file set up as your build profile script.  To see a basic build profile,
	look at the bottom of this file.

4) 	cd into $DOJO-SRC/util/buildscripts

5) 	run the command

		./build.sh profile=dotcms action=release optimize=shrinkSafe releaseDir=../../../../release/

	This will build everything you need into $DOTCMS_ROOT/dotCMS/html/js/dojo/

	If successful, you should see the files
		$DOJO-SRC/release/dojo/dojo/dot-dojo.js
		$DOJO-SRC/release/dojo/dojo/dot-dojo.js.uncompressed.js

	These are your custom builds - the uncompressed one has not been minified for easier debugging.

	You use them by calling dojo.js first, then the custom build .js, like this:

		<script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js"></script>
		<script type="text/javascript" src="<%=dojoPath%>/dojo/dot-dojo.js"></script>

	The dojoPath is set in dotmarketing-config.properties.
	Both dojo.js and dot-dojo.js need to be in the same folder, unless you want to spend the time to
	configure dojo to look for them in different places.

	Enjoy!


Additional Notes:

===========================================
===========================================
dojo 1.8 has an issue visible on the filtering select and combo boxes (https://github.com/dotCMS/core/issues/6431) where you can NOT
expand and element at the end (actually if the element to select is outside the initial visible area and you need to scroll to see it) of the tree
component causing the tree to jump to the beginning of the component avoiding the child to expand.
This happen when the we set a max-height (required as our trees can grow a lot) for the tree component, in that case the focus events are effected
and when trying to expand the child dojo will focus on the parent container avoiding the user to expand the child.

fix:
    dot-dojo.js:
        Original:
            _onContainerFocus:function(evt){
                if(evt.target!==this.domNode||this.focusedChild){
                    return;
                }
                this.focus();
            }
        Change for:
            _onContainerFocus:function(evt){
                if((evt.explicitOriginalTarget && evt.explicitOriginalTarget.tagName == "IMG" && evt.explicitOriginalTarget.className.indexOf("TreeExpando") > -1)
                 ||  evt.target!==this.domNode||this.focusedChild){
                    return;
                }
                this.focus();
            }

We added a fix for the specific tree issue in order to avoid to brake old working functionality.
NOTE: In case of an upgrade test this issue before to apply this fix, a fix could be already added on new dojo releases.

===========================================
===========================================
The file /dotCMS/html/js/dojo/release/dojo/dojox/form/uploader/plugins/Flash.js	needs to be modified to be able to fix https://github.com/dotCMS/dotCMS/issues/524.
The modification is in lines ~(275,276).

replace: var args = {
			expressInstall:true,
			path: (this.swfPath.uri || this.swfPath) + "?cb_" + (new Date().getTime()),
			width: w,
			height: h,

with: var args = {
			expressInstall:true,
			path: (this.swfPath.uri || this.swfPath) + "?cb_" + (new Date().getTime()),
//			width: w,
//			height: h,


---------------------------- START example dotcms.profile.js ------------------------------


dependencies = {

	stripConsole: "normal",

	layers: [
		{
			name: "../dojo/dot-dojo.js",
			dependencies: [
				"dojo.dojo",
				"dijit.Dialog",
				"dijit.form.Button",
				"dijit.form.CheckBox",
				"dijit.form.DateTextBox",
				"dijit.form.FilteringSelect",
				"dijit.form.TextBox",
				"dijit.form.ValidationTextBox",
				"dijit.form.Textarea",
				"dijit.Menu",
				"dijit.MenuItem",
				"dijit.MenuSeparator",
				"dijit.ProgressBar",
				"dijit.PopupMenuItem",
				"dijit.layout.TabContainer",
				"dijit.layout.ContentPane",
				"dijit.layout.BorderContainer",
				"dijit.TitlePane",
				"dijit.Tooltip",
				"dojo.parser",
				"dojo.fx",
				"dojox.form.DropDownSelect",
				"dojox.json.query",
				"dojo.data.api.Read",
				"dojo.data.api.Request",
				"dojo.data.api.Identity",
				"dijit.Tree",
				"dojo.DeferredList",
				"dijit.tree.TreeStoreModel",
				"dijit.tree.ForestStoreModel",
				"dojo.data.ItemFileReadStore",
				"dijit.form.MultiSelect",
				"dijit.layout.AccordionContainer",
				"dijit.layout.AccordionPane",
				"dijit.form.Form",
				"dojox.grid.DataGrid",
				"dijit.dijit",
				"dojox.html.metrics",
				"dojox.grid.util",
				"dojo.dnd.Source",
				"dojo.dnd.Selector",
				"dojo.dnd.Container",
				"dojo.dnd.Manager",
				"dojo.dnd.Avatar",
				"dojox.grid.Selection",
				"dijit.nls.loading",
				"dojox.grid.DataSelection",
				"dojo.data.ItemFileWriteStore",
				"dijit.form.NumberTextBox",
				"dijit.form.Slider",
				"dijit.form.HorizontalSlider",
				"dijit.form.VerticalSlider",
				"dijit.form.HorizontalRule",
				"dijit.form.VerticalRule",
				"dijit.form.HorizontalRuleLabels",
				"dijit.form.VerticalRuleLabels",
                "dijit.form.ComboBox",
	            "dijit.TooltipDialog",
     	     	"dojox.grid.EnhancedGrid",
     		    "dojox.grid.enhanced.plugins.Menu"
            ]
        }
    ],
    
    prefixes: [
               [ "dijit", "../dijit" ],
               [ "dojox", "../dojox" ]
           ]

}
