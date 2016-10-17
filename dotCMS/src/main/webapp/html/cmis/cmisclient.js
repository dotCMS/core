/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 	The CMIS javascript client gives access to a CMIS document management system
 *	from client-side java script code.	   
 *	 
 */

CMISClient = function (url) {
	this.CMIS_SERVICE_URL=url;
	this.info = this.getRepositoryInfo();
	this.connected = this.info?true:false;
}

CMISClient.NAME_OF_THIS_FILE = "cmisclient.js";

/** trim helper */
CMISClient.trim = function(s) {
	return s.replace(/^\s*/, "").replace(/\s*$/, "");
}
	

/** implements transformation from an xml document to a reasonable js object 
essentially poor mans jquery
*/
CMISClient.flatten = function(elem, obj) {
	if (!obj) obj=new Object();
	var i=0;
	while (i<elem.childNodes.length) {
		
		/* iterate through all the child nodes of the atom structure */
		
		var child=elem.childNodes[i];
		
		var value="";
		
		switch (child.nodeType) {

			/* found an element */
			case Node.ELEMENT_NODE:
				if (child.attributes.length==0 && child.childNodes.length==1 && child.childNodes[0].nodeType==Node.TEXT_NODE && child.childNodes[0].nodeValue) {
					/* fold simple cdata into a property */
					obj[child.nodeName]=this.trim(child.childNodes[0].nodeValue);
				} else {
					if (!obj[child.nodeName]) {

						/* proper substructure without same name sibling */
						obj[child.nodeName]=new Object();
						this.flatten(child, obj[child.nodeName]);
					} else {
						/* ugly same name sibling handling needs fixing */
						var j=1;
						while (obj[child.nodeName+"_"+j]) {
							j++;
						}
						obj[child.nodeName+"_"+j]=new Object();
						CMISClient.flatten(child, obj[child.nodeName+"_"+j]);
					}
				}
				break;


			case Node.TEXT_NODE:
				/* cdata in unexpected place */
				var val=CMISClient.trim(child.nodeValue);
				if (val) {
					obj["text"]=CMISClient.trim(child.nodeValue);
				}

				break;

		}
		
		
		
		i++;
	}
	var i=0;
	if (elem.attributes) {
		while (i<elem.attributes.length) {

			/* place attribute values with their names */
			var child=elem.attributes[i];
			obj[child.nodeName]=child.nodeValue;

			i++;
		}
	}
	return (obj);
}

/** Processes an Atom entry into a usable js object */
CMISClient.processEntry = function(node) {
	var entry=new Object();
	for (var a in node) {
		var elem=node[a];
		if (a.indexOf("link")==0) {
			entry[elem.rel]=elem.href;
		} else if (a=="cmis:object") {
			var props=elem["cmis:properties"];
			for (var b in props) {
				var prop=props[b];
				entry[prop["cmis:name"]]=prop["cmis:value"];
			}
		} else {
			entry[a]=elem;
		}
	}
	
	entry.author=node.author.name;
	entry.content=node.content;
	entry.id=node.id;
	return (entry);
}

/** Gets a folder from via atom url */
CMISClient.prototype.getFolder = function(url) {
	
	if (url=="/" || !url) {
		url=this.info.collections["root"];
	}
	var htcon=this.httpGet(url);
	this.lastHttpStatus = htcon.status;
	if (htcon.status != 200) {
		return null;
	}
	
	var doc=htcon.responseXML;
	var flatres=CMISClient.flatten(doc);

	var feed=flatres.feed;
	
	var res=new Object();
	res.author=feed.author.name;
	res.id=feed.id;
	res.title=feed.title;
	res.updated=feed.updated;
	
	res.links=new Object();
	res.entries=new Object();

	var linkcount=0;
	var entrycount=0;
	
	for (var a in feed) {
		var node=feed[a];
		if (a.indexOf("entry")==0) {
			res.entries[entrycount++]=CMISClient.processEntry(node);
		}
		if (a.indexOf("link")==0) {
			
		}

	}

	return(res);
}

/** This method reads the repository Info */
CMISClient.prototype.getRepositoryInfo = function() {
	var htcon=this.httpGet(this.CMIS_SERVICE_URL);
	this.lastHttpStatus = htcon.status;
	
	/* could not connect */
	if (htcon.status != 200) { 
		return null;
		}
		
	var doc=htcon.responseXML;
	var flatres=CMISClient.flatten(doc);
	var res=new Object();
	
	var repoinfo=flatres.service.workspace["cmisra:repositoryInfo"];
	
	res.repositoryId = repoinfo["cmis:repositoryId"];
	res.repositoryName = repoinfo["cmis:repositoryName"];
	res.repositoryRelationship = repoinfo["cmis:repositoryRelationship"];
	res.repositoryDescription = repoinfo["cmis:repositoryDescription"];
	res.vendorName = repoinfo["cmis:vendorName"];
	res.productName = repoinfo["cmis:productName"];
	res.productVersion = repoinfo["cmis:productVersion"];
	res.rootFolderId= repoinfo["cmis:rootFolderId"];
	
	var caps=repoinfo["cmis:capabilities"];
	res.capabilities = new Object();
	res.capabilities.multifiling = caps["cmis:capabilityMultifiling"];
	res.capabilities.unfiling = caps["cmis:capabilityUnfiling"];
	
	res.cmisVersionsSupported = repoinfo["cmis:cmisVersionsSupported"];
	
	res.collections = new Object();
	
	for (var a in flatres.service.workspace) {
		if (a.indexOf("collection")==0) {
			var collection=flatres.service.workspace[a];
			res.collections[collection["cmisra:collectionType"]]=collection.href;
		}
	}
	return (res);
}
	
/**
 *	Get an XMLHttpRequest in a portable way
 *
 */
CMISClient.prototype.getXHR = function () {
	var xhr=null;
	
	if(!xhr) {
		try {
			// built-in (firefox, recent Opera versions, etc)
			xhr=new XMLHttpRequest();
		} catch (e) {
			// ignore
		}
	}
	
	if(!xhr) {
		try {
			// IE, newer versions
			xhr=new ActiveXObject("Msxml2.XMLHTTP");
		} catch (e) {
			// ignore
		}
	}
	
	if(!xhr) {
		try {
			// IE, older versions
			xhr=new ActiveXObject("Microsoft.XMLHTTP");
		} catch (e) {
			// ignore
		}
	}
	
	if(!xhr) {
		alert("Unable to access XMLHttpRequest object, cmis client will not work!");
	}
	
	return xhr;
}

/**
 * HTTP GET XHR Helper
 * @param {String} url The URL
 * @return the XHR object, use .responseText for the data
 * @type String
 */
CMISClient.prototype.httpGet = function(url) {
    var httpcon = this.getXHR();
    if (httpcon) {
		httpcon.open('GET', url, false);
		httpcon.send(null);
		return httpcon;
    } else {
		return null;
    }
}
	
/**
 * Produces a "sort-of-json" string representation of a object
 * for debugging purposes only
 * @param {Object} obj The object
 * @param {int} level The indentation level
 * @return The result
 * @type String
 */
CMISClient.dumpObj = function(obj, level) {
	var res="";
	for (var a in obj) {
		if (typeof(obj[a])!="object") {
			res+=a+":"+obj[a]+"  ";
		} else {
			res+=a+": { ";
			res+=this.dumpObj(obj[a])+"} ";
		}
	}
	return (res);
}
