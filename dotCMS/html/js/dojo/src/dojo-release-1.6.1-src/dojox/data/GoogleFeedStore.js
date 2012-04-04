define("dojox/data/GoogleFeedStore", ["dojo", "dojox", "dojox/data/GoogleSearchStore"], function(dojo, dojox) {

dojo.experimental("dojox.data.GoogleFeedStore");

dojo.declare("dojox.data.GoogleFeedStore", dojox.data.GoogleSearchStore,{
	// summary:
	//	A data store for retrieving RSS and Atom feeds from Google. The
	//  feeds can come from any source, which is specified in the "url"
	//  parameter of the query passed to the "fetch" function.
	//	The following attributes are supported on each item:
	//		<ul>
	//			<li>title - The feed entry title.</li>
	//			<li>link - The URL for the HTML version of the feed entry.</li>
	//			<li>content - The full content of the blog post, in HTML format</li>
	//			<li>summary - A snippet of information about the feed entry, in plain text</li>
	//			<li>published - The string date on which the entry was published.
	//				You can parse the date with new Date(store.getValue(item, "published")</li>
	//			<li>categories - An array of string tags for the entry</li>
	//		</ul>
	//	The query accepts one parameter: url - The URL of the feed to retrieve
	_type: "",
	_googleUrl: "http://ajax.googleapis.com/ajax/services/feed/load",
	_attributes: ["title", "link", "author", "published",
			"content", "summary", "categories"],
	_queryAttrs: {
		"url":"q"
	},
	
	getFeedValue: function(attribute, defaultValue){
		// summary:
		//		Non-API method for retrieving values regarding the Atom feed,
		//		rather than the Atom entries.
		var values = this.getFeedValues(attribute, defaultValue);
		if(dojo.isArray(values)){
			return values[0];
		}
		return values;
	},

	getFeedValues: function(attribute, defaultValue){
		// summary:
		//		Non-API method for retrieving values regarding the Atom feed,
		//		rather than the Atom entries.
		if(!this._feedMetaData){
			return defaultValue;
		}
		return this._feedMetaData[attribute] || defaultValue;
	},

	_processItem: function(item, request) {
		this.inherited(arguments);
		item["summary"] = item["contentSnippet"];
		item["published"] = item["publishedDate"];
	},

	_getItems: function(data){
		if(data['feed']){
			this._feedMetaData = {
				title: data.feed.title,
				desc: data.feed.description,
				url: data.feed.link,
				author: data.feed.author
			};
			return data.feed.entries;
		}
		return null;
	},

	_createContent: function(query, callback, request){
		var cb = this.inherited(arguments);
		cb.num = (request.count || 10) + (request.start || 0);
		return cb;
	}
});

return dojox.data.GoogleFeedStore;
});
