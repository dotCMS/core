<script>
											<%StringBuffer buff = new StringBuffer();
											  // http://jira.dotmarketing.net/browse/DOTCMS-6148
											  buff.append("{identifier:'id',imageurl:'imageurl',label:'label',items:[");

											  String imageURL="/html/images/languages/all.gif";
											  String style="background-image:url(URLHERE);width:16px;height:11px;display:inline-block;vertical-align:middle;margin:3px 5px 3px 2px;";
											  buff.append("{id:'0',value:'',lang:'All',imageurl:'"+imageURL+"',label:'<span style=\""+style.replaceAll("URLHERE",imageURL)+"\"></span>All'}");
											  for (Language lang : languages) {
												  imageURL="/html/images/languages/" + lang.getLanguageCode()  + "_" + lang.getCountryCode() +".gif";
												  final String display=lang.getLanguage() + " (" + lang.getCountryCode().trim() + ")";
												  buff.append(",{id:'"+lang.getId()+"',");
												  buff.append("value:'"+lang.getId()+"',");
												  buff.append("imageurl:'"+imageURL+"',");
												  buff.append("lang:'"+display+"',");
												  buff.append("label:'<span style=\""+style.replaceAll("URLHERE",imageURL)+"\"></span>"+display+"'}");
											  }
											  buff.append("]}");%>

											function updateSelectBoxImage(myselect) {
												var imagestyle = "url('" + myselect.item.imageurl + "')";
												var selField = dojo.query('#combo_zone2 div.dijitInputField')[0];
												dojo.style(selField, "backgroundImage", imagestyle);
												dojo.style(selField, "backgroundRepeat", "no-repeat");
												dojo.style(selField, "padding", "0px 0px 0px 25px");
												dojo.style(selField, "backgroundColor", "transparent");
												dojo.style(selField, "backgroundPosition", "3px 6px");
											}

												var storeData=<%=buff.toString()%>;
												var langStore = new dojo.data.ItemFileReadStore({data: storeData});
												var myselect = new dijit.form.FilteringSelect({
														 id: "language_id",
														 name: "language_id",
														 value: '',
														 required: true,
														 store: langStore,
														 searchAttr: "lang",
														 labelAttr: "label",
														 labelType: "html",
														 onChange: function() {
															 var el=dijit.byId('language_id');
															 updateSelectBoxImage(el);
															 doSearch();
														 },
														 labelFunc: function(item, store) { return store.getValue(item, "label"); }
													},
													dojo.byId("language_id"));
													
													myselect.setValue('<%=languageId%>');

										</script>