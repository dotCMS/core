<script>
	<%StringBuffer buff = new StringBuffer();
	  buff.append("{identifier:'id',label:'label',items:[");

	  String style="width:16px;height:11px;display:inline-block;vertical-align:middle;margin:3px 5px 3px 2px;";
	  buff.append("{id:'0',value:'',lang:'"+ LanguageUtil.get(pageContext, "All") +"',label:'" + LanguageUtil.get(pageContext, "All") + "'}");
	  for (Language lang : languages) {
		  final String display=lang.getLanguage() + (UtilMethods.isSet(lang.getCountryCode()) ? " (" + lang.getCountryCode().trim() + ")" : "");
		  buff.append(",{id:'"+lang.getId()+"',");
		  buff.append("value:'"+lang.getId()+"',");
		  buff.append("lang:'"+display+"',");
		  buff.append("label:'"+display+"'}");
	  }
	  buff.append("]}");%>

	function updateSelectBoxImage(myselect) {
		var selField = dojo.query('#combo_zone2 div.dijitInputField')[0];
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