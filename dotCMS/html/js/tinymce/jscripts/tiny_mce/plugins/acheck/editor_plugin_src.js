/**
 * $Id: editor_plugin_src.js 201 2008-04-17 15:56:56Z cindy $
 *
 * @author Cindy Li
 * @copyright Copyright © 2008, ATutor, All rights reserved.
 */

(function() {
	// Load plugin specific language pack
	tinymce.PluginManager.requireLangPack('acheck');

	tinymce.create('tinymce.plugins.AcheckPlugin', {
		/**
		 * Initializes the plugin, this will be executed after the plugin has been created.
		 * This call is done before the editor instance has finished it's initialization so use the onInit event
		 * of the editor instance to intercept that event.
		 *
		 * @param {tinymce.Editor} ed Editor instance that the plugin is initialized in.
		 * @param {string} url Absolute URL to where the plugin is located.
		 */
		init : function(ed, url) {
			// Register the command so that it can be invoked by using tinyMCE.activeEditor.execCommand('mceACheck');
			ed.addCommand('mceACheck', function() {

					var theCode = '<html><body onLoad="document.accessform.submit();"> \n';
					theCode += '<h1>Submitting Code for Accessibility Checking.....</h1>\n';
					theCode += '<form action="http://achecker.ca/checker/index.php" name="accessform" method="post"> \n';
					theCode += '<input type="hidden" name="gid[]" value="8" /> \n';
					theCode += '<textarea name="validate_content">' + tinyMCE.activeEditor.getContent({format : 'raw'}) + '</textarea>\n';
					theCode += '<input type="submit" /></form> \n';  
					theCode += '</body></html> \n';
					accessWin = window.open('', 'accessWin',  '');
					accessWin.document.writeln(theCode);
					accessWin.document.close();
			});

			// Register ACheck button
			ed.addButton('acheck', {
				title : 'acheck.desc',
				cmd : 'mceACheck',
				image : url + '/img/acheck.gif'
			});

			// Add a node change handler, selects the button in the UI when a image is selected
			ed.onNodeChange.add(function(ed, cm, n) {
				cm.setActive('acheck', n.nodeName == 'acheck');
			});
		},

		/**
		 * Creates control instances based in the incomming name. This method is normally not
		 * needed since the addButton method of the tinymce.Editor class is a more easy way of adding buttons
		 * but you sometimes need to create more complex controls like listboxes, split buttons etc then this
		 * method can be used to create those.
		 *
		 * @param {String} n Name of the control to create.
		 * @param {tinymce.ControlManager} cm Control manager to use inorder to create new control.
		 * @return {tinymce.ui.Control} New control instance or null if no control was created.
		 */
		createControl : function(n, cm) {
			return null;
		},

		/**
		 * Returns information about the plugin as a name/value array.
		 * The current keys are longname, author, authorurl, infourl and version.
		 *
		 * @return {Object} Name/value array containing information about the plugin.
		 */
		getInfo : function() {
			return {
				longname : 'ACheck Plugin',
				author   : 'ATutor',
				authorurl : 'http://www.atutor.ca',
				infourl : 'http://www.atutor.ca',
				version : "1.0"
			};
		}
	});

	// Register plugin
	tinymce.PluginManager.add('acheck', tinymce.plugins.AcheckPlugin);
})();
