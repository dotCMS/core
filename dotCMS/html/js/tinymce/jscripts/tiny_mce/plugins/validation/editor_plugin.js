(function() {
        tinymce.PluginManager.requireLangPack('validation');

        tinymce.create('tinymce.plugins.ValidationPlugin', {
		 
                /**
                 * Initializes the plugin, this will be executed after the plugin has been created.
                 * This call is done before the editor instance has finished it's initialization so use the onInit event
                 * of the editor instance to intercept that event.
                 *
                 * @param {tinymce.Editor} ed Editor instance that the plugin is initialized in.
                 * @param {string} url Absolute URL to where the plugin is located.
                 */
                init : function(ed, url) {
                        // Register the command so that it can be invoked by using tinyMCE.activeEditor.execCommand('mceExample');
			var t = this;			
			t.url = url;
			t.editor = ed;
			
                        // Add a node change handler, selects the button in the UI when a image is selected
                        ed.onNodeChange.add(function(ed, cm, n) {
				var t = this;			
				t.editor = ed;
				cm.setActive('mceValidation',  t.active );
				 
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
			var t = this   ;
			var c;
			var ed = t.editor; 
			var guideLineSel = "";  
			var s = ed.settings ;
			
			switch (n) {
			    case 'validation':
				 if (navigator.appName == 'Netscape')
				    var language = navigator.language;
				else
				    var language = navigator.browserLanguage;
				var code = language.substring(0,2);
				// code = 'eng';
			    c = cm.createSplitButton( n , {
			      title : 'validation.desc',
				  image : t.url + '/img/check.png',
				  scope : t,	
				  onclick: function () {
				                var request = {	'lang': code, 
				                	'content': tinyMCE.activeEditor.getContent({format : 'text'}), 
							        'guidelines': guideLineSel ,
							        'fragment'  : true
							};
 				var idName ='acheck'+tinyMCE.activeEditor.id;
				
				
				function createValidationResult( response ){
					
					var div = document.createElement("div"); 
					div.id = idName; 
					div.setAttribute("align", "center");
				
					var text = "" ;
					var errors = false;
					var msgText = ed.getLang('validation.validation_complete', 0);
					
					if( response.errors.length > 0 ){
						errors = true;
						msgText += response.errors.length + ed.getLang('validation.errors_found', 0);
					}
					else {
					  msgText += ed.getLang('validation.no_errors_found', 0);
					}

					if( response.errors.length > 0 ){
						text  += "<p style='color:red;'><strong>"+ed.getLang('validation.table_summary', 0)+"" + guideLineSel +" </strong> </p>";
						var testo = ed.getLang('validation.table_description', 0);
						text += "<table border='1' class='listingTable'>";
						text += "<tr> <th>Check ID </th>  "+ 
							"<th>"+testo+"</th>"+
							"<th>"+ed.getLang('validation.table_rowcols', 0)+"</th>"+
							"<th>"+ed.getLang('validation.table_errortype', 0)+"</th> </tr>";
						
						for ( var i = 0; i < response.errors.length; i++ ) {
							var error = response.errors[i];
							var modulo = (i+1)   % 2;
							var classValue ="1";
							if( modulo == 0 ){
								classValue = "2";
							}
							var classType = "alternate_"+classValue;							 
 							var idRow = "elem_"+i;								
							text += "<tr id='"+idRow+"' class='"+classType + "' >";	
							text += "<td class='titleCellDiv'>" +error.check.check_id + "</td>"; 
							text += "<td> " + error.check.err +"( " + error.check.description +" ) </td>";
							  						 
							text += "<td>" + error.line_number + "/" + error.col_number + "</td>";
							text += "<td>" + error.check.confidenceEnum +"</td>";
							text += "</tr> ";
						}
						text += "</table>";
						div.innerHTML = text;
						var maxTextAreaWidth=dojo.query("textarea").map(function(x) {
							return dojo.style(x,'width');
					    }).max();
						var dialog = dijit.Dialog({
							title: msgText,
							content : div,
							style: "width: "+ maxTextAreaWidth +"px"
						});
						dialog.show();
					}
					else {
						showDotCMSSystemMessage( msgText );
					}
				} 
				try{
				    ACheckerDWR.validate( request ,createValidationResult );
				} catch(e){
				    alert(e);
				}
			 }
			 });

				var menuArray = new Object();
				var validationStatus = new Object();
				var guideLineSize = 0;
				var menu ;
				function toggleValidationItem(i) {
					for (var key = 0; key < guideLineSize; key++) {
						if (validationStatus[key]) {
							validationStatus[key] = false;
							menuArray[key].setSelected(false);
 						}
					}
					validationStatus[i] = true
					menuArray[i].setSelected(true);
				}
				
				// Funzione che crea i menu con le linee guida supportate
				function createGuideline( response  ){
					 guideLineSize = response.length;
					
					 for (var i = 0; i < response.length; i++) {
						validationStatus[i] = false;
					 	var field = response[i].abbr;						 		
					  	var o = {icon: 1, myIndex: i}, mi  ;						
						o.title = field;
						o.onclick = function(title) {
								guideLineSel = this.title;
								toggleValidationItem(this.myIndex);
							}; 
						mi = menu.add(o);								 
						menuArray[i] = mi;
						if( guideLineSel == '' && response[i].defaultGuideLine ) 							
						{
							toggleValidationItem(i);
							t.selectedItem = mi;	
						}
					    }
				}
 				if( c!= null ){		
				      c.onRenderMenu.add(function (c, m) {
						menu = m; 	
 						m.add({ title: 'validation.guidelines', 'class': 'mceMenuItemTitle' })  ;
						//var replaynew  = createGuideline(response);
 						ACheckerDWR.getSupportedGudelines(  createGuideline    ); 
						   
						});
					}/*  */

				        // Return the new splitbutton instance
				        return c;
				}

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
                                longname : 'Validation plugin',
                                author : 'Eng',
                                authorurl : 'http://www.eng.it',
                                infourl : 'http://www.eng.it',
                                version : "1.0"
                        };
                }
        });

        // Register plugin
        tinymce.PluginManager.add('validation', tinymce.plugins.ValidationPlugin);
})();