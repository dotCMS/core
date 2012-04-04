<%@page import="com.dotmarketing.util.Config"%>
<%String dojoPath = Config.getStringProperty("path.to.dojo");%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"    
"http://www.w3.org/TR/html4/strict.dtd">  
<html>  
<head> 
<meta name="verify-v1" content="Luvx8voB5MjHhuEXDSRYodL2Dpzl506jetnN+a3kPw4=" >
<link rel="shortcut icon" href="//www.dotcms.org/global/favicon.ico" type="image/x-icon">
<link rel="alternate" type="application/rss+xml" title="dotcms.org RSS" href="http://www.dotcms.org/company/press_release_rss.dot" />

	<title>dotCMS Code Style Guide</title>
	<style media="all" type="text/css">
		@import url(/html/css/reset-min.css);
		@import url(/html/css/grids-min.css);
		@import url(/html/css/base.css);
		@import url(/html/css/dot_admin.css);
        @import "<%=dojoPath%>/dijit/themes/dmundra/dmundra.css";
	</style>
	
	<style>
		#doc,#doc1,#doc2,#doc3,#doc4{background-color:#fff;padding:10px;border:1px solid #d1d1d1;}
		#hd{margin-top:5px;}
		#bd{margin-bottom:20px;-moz-box-shadow:0px 0px 0px #fff;-webkit-box-shadow:0px 0px 0px #fff;border:0px;padding:0;}
		body{font:13px/1.22 Verdana, Geneva,  Arial, sans-serif;*font-size:small;*font:x-small;color:#555;line-height:138.5%;background-color:#f3f3f3;}/* 13pt */
		h1{font-size:174%;font-family:"Baskerville Old Face", "Times New Roman";margin:10px 0 10px 10px;font-weight:normal;}/* 22pt */
		h2{font-size:108%;color:#990000;padding:10px 15px 0 0;font-weight:normal;}/* 18pt */
		p,fieldset,table{font-size:85%;margin:0 0 .2em 0;}
		a{color: #2C548D;}
		a:hover{}
		hr {border:0; color: #9E9E9E;background-color: #9E9E9E;height: 1px;width: 100%;text-align: left;}
		pre {width: 100%;overflow: auto;font-size: 12px;padding: 0;margin: 0;background: #f0f0f0;border: 1px solid #ccc;line-height: 20px;overflow-Y: hidden;}
		pre code {margin: 0 0 0 20px;padding: 0 0 16px 0;display: block;white-space:pre;}
		ol li { list-style: upper-roman; padding-bottom: 15px; }
		.wrong { color: red; }
		.good { color: green; }
	</style>
				
	<SCRIPT TYPE="text/javascript" SRC="<%=dojoPath%>/dojo/dojo.js" djConfig="parseOnLoad: true"></SCRIPT>

	<script type="text/javascript" src="<%=dojoPath%>/dojo/dot-dojo.js"></script>
	<script type="text/javascript">
		dojo.require("dijit.layout.TabContainer");
		dojo.require("dijit.layout.ContentPane");
	</script>
	
</head>

<body class="dmundra">

<div id="doc3">

<div id="hd">
	<img src="http://www.dotcms.org/global/images/template/logo2.gif" />
	<div class="yui-g">
		<div class="yui-u first"><h1>dotCMS Coding Style Guide</h1></div>
		<div class="yui-u" style="text-align:right;padding: 10px 10px 0 0;">
			<div style="font-size:85%;">
			</div>
		</div>
	</div>
</div>
	
<div id="bd">

<!-- START TABS -->
<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">


	<!-- START HTML Code Tab -->
	<div id="HTMLCodeTab" dojoType="dijit.layout.ContentPane" title="HTML Code">
		<br>
		<ol>
			<li>
				Keep the HTML as clean as possible base your html (static and dynamic generated) on the  
				<a href="/html/style_guide/">html style guide</a>
			</li>
			<li>
				Do not add unnecesary attributes to html tags. For example: do not add a name attribute to an input tag that you are not going to submit, if you need to reference
			   	that input object you will be using only the id not the name attribute.
				<br><span class="wrong">WRONG</span>
				<br>
				<pre><code>
&lt;input id="myDinamicManipulatedInput" <span class="wrong">name="myUnnecesaryName"</span>/>
				</code></pre>
				<br><span class="good">GOOD</span>
				<br>
				<pre><code>
&lt;input id="myDinamicManipulatedInput"/>
				</code></pre>
			</li>
			<li>
	 			Avoid inline styles, instead assign styles via css classes.
				<br><span class="wrong">WRONG</span>
				<br>
				<pre><code>
&lt;div <span class="wrong">style="width:300px"</span>>...&lt;/div>
				</code></pre>
				<br><span class="good">GOOD</span>
				<br>
				<pre><code>
&lt;div <span class="good">class="myReusableCSSClass"</span>>...&lt;/div>
				</code></pre>
			</li>
			<li>
				Avoid HTML deprecated styling attributes and elements, use for those css classes instead,
				<br><span class="wrong">WRONG</span>
				<pre><code>
&lt;img <span class="wrong">width="200" height="200"</span> ... />
				</code></pre>
				<pre><code>
&lt;<span class="wrong">font</span> ...>
				</code></pre>
				<br><span class="good">GOOD</span>
				<pre><code>
&lt;img <span class="good">class="myReusableCSSClass"</span> ... />
				</code></pre>
			</li>
			<li>
				Avoid making up css classes instead always check css classes already created in the html style guide or on the 
				main css dotCMS style sheet.				
			</li>
			<li>
				Use comment markers to indicate when an html code section starts and ends. E.G.
				<pre><code>
&lt;!-- START My Special Tab -->
&lt;div id="MySpecialTab" dojoType="dijit.layout.ContentPane" title="My Special Tab"> ... &lt;/div>
&lt;!-- END My Special Tab -->					
				</code></pre>
				
			</li>
			<li>
				Proper Code indentation is important, I repeat proper indentation is IMPORTANT
			</li>
		</ol>
	
	</div>
	<!-- END HTML tab -->
	
	<!-- START JSP code tab -->
	<div id="JSPCodeTab" dojoType="dijit.layout.ContentPane" title="JSP Code">
		<br>
		<ol>
			<li>
				Don't mix multiline scriptlet code within the page body, instead initialize your variables at the top of the jsp page as possible. 
			</li>
			<li>
				Unless it is at the top of the page do not use multiline scriptlet code. Instead open and close the scriptlet at the same line.
				<pre><code>
<span class="wrong">&lt;% 
	line 1 of code
	line 2 of code
%&gt;</span> 
				</code></pre>
				<br><span class="good">GOOD</span>
				<pre><code>
&lt;% line 1 of code %&gt;
&lt;% line 2 of code %&gt;
				</code></pre>				
				 
			</li>
			<li>
				All page import should be at the top of the page. 
			</li>
		</ol>
	</div>
	<!-- END JSP code tab -->
	
	<!-- START Javascript/AJAX Tab -->
	<div id="JSTab" dojoType="dijit.layout.ContentPane" title="Javascript/AJAX">
		<br>
		<ol>
			<li>
			 	Open your script tags including the type attribute, this is the HTML correct way.<br/>
			 	<pre><code>
&lt;script type=&quot;text/javascript&quot;&gt;
	...
&lt;/script&gt;
				</code></pre>
			</li>
			<li>
				Separate functionality on separate jsp file, E.G. if your page have multiple tabs separate every tab funcionality per jsp.
			</li>			
			<li>
				Correctly camel case variable names, functions and methods. Camel case object names with the first letter capitalized as well like in Java.<br/>
				<span class="wrong">WRONG</span><br/>
					<pre><code>
dojo.declare('<span class="wrong">nottheRightName</span>', null, { 
	<span class="wrong">wrongvariablename</span>: 'bad',
	<span class="wrong">iAmnotawellnamedFunction</span>: function () { 
		var <span class="wrong">wronglocalFunctionName</span>;
	},
});
					</code></pre>
				<span class="good">GOOD</span><br/>
					<pre><code>
dojo.declare('<span class="good">IAmAWellNamedClass</span>', null, {
	<span class="good">iAmAWellNamedClassAttribute</span> = 'bad';
	<span class="good">iAmAWellNamedClassMethod</span>: function () { },
});	
					</code></pre>
			</li>		
			<li>
				Sandbox js functions within JS classes using dojo.declare.
			
				<br>	<span class="wrong">WRONG</span>
				<pre><code>
function <span class="wrong">myBadGlobalFunctionThatCanConflictWithOthers</span> () {
	...
}
				</code></pre>
				<br>	<span class="good">GOOD</span>
				<pre><code>
dojo.declare('MyBeautyfulEncapsulationObject', null, {
	<span class="good">iAmNotGoingToConflictWithOtherFunction</span>: function (...) {
		//Safe code place
		...
	}
});
				</code></pre>
			</li>
			<li>
				User the &quot;var&quot; keyword when declaring local function variables, if you do not you are making it a dangerous global variable.
				<br>	<span class="wrong">WRONG</span>
				<pre><code>
dojo.declare('MyClass', null, { 
	myFunction: function () { 
		<span class="wrong">wrongGlobalVariable</span> = 'bad';
	},
});
				</code></pre>
				<br>	<span class="good">GOOD</span>
				<pre><code>
dojo.declare('MyClass', null, { 
	localClassAttribute: 0;
	myFunction: function () { 
		<span class="good">var safeLocalVariable</span> = 'bad';
		<span class="good">this.localClassAttribute</span>++; //Correct refence to local object variable
	},
});
				</code></pre>
			</li>
			<li>
				Don't use more than dojo and prototype functions, no other framework allowed.
			</li>
			<li>
				If you need new dijits check for dijits developed under /html/js/dotcms/dijit
			</li>
			<li>
				If you need to modify a Dojo dijit, you need to develop your own dijit and place it under /html/js/dotcms/dijit, always assume your dijit will
				be reused somewhere else so code it generic and make it take constructor parameters so it can be generically configured.
			</li>
			<li>
				If you are going to develop an Ajax listing do it all in Ajax, meaning do not mix loading the initial table within JSP Code and then manipulate 
				it using Javascript.
			</li>
			<li>
				Avoid declaring global variables, instead make them local attributes of your object.
			</li> 
			<li>
				Use template variables for dinamic generated html, never
			</li> 
			<li>
				Proper Code indentation is important, I repeat proper indentation is IMPORTANT
			</li>	
			<li>
				If there is already an API for the object you need to manipulate use it instead of using the old/deprecated factories. If the API is missng
				the method you need then add it after discussing with a lead developer of the core team about the needed method.
			</li>		
		</ol>
	
	</div>
	<!-- END Javascript/AJAX Tab -->
	
	
	
	<!-- START Java Tab -->
	<div id="Java" dojoType="dijit.layout.ContentPane" title="Java">
		<br>
		<ol>
			<li>
				First and most important premise, old code should be fixed and make it follow this list of guidelines while you work on it. It is unacceptable
				to fix old code without make it also follow the coding guidelines.
			</li>
			<li>
				Import within Eclipse the following Code Style configuration <a href="eclipseCodeStyle.xml">here</a><br/>
				To import go to Eclipse->Preferences->Java->Code Style->Formatter
			</li>
			<li>
				Any modification to a public api interface must be discussed, no public api method should be changed or new methods added without consulting.
			</li>
			<li>
				DO NOT use deprecated methods, yellow code warnings are bad. When modifying old code with yellow warnings fix it as you work on it. Only 
				@SuppressWarnings("unchecked") is allowed when casting old non generic collections. DO NOT supress deprecation warnings.
			</li>
			<li>
				Proper Code indentation is important, I repeat proper indentation is IMPORTANT
				<br>	<span class="wrong">WRONG</span>
				<br>
				<pre><code>
public class MyClass {
<span class="wrong">public void iAmNotCorrectlyIndentedMethod () { ... }</span>
	public String myOtherMethod () { 
		if(...) {
		<span class="wrong">String iAmNotCorrectlyIndented;</span>
			...
		}
 	}
	...
}
				</code></pre>
				<br>	<span class="good">GOOD</span>
				<pre><code>
public class MyClass {
	public void myFirstMethod () { ... }
	public String myOtherMethod () { 
		if(...) {
			String localVariable;
		}
 	}
	...
}
				</code></pre>	
			</li>		
			<li>
				On apis and factories declare dependencies on other apis and factories at the top and initialize them in the constructor, 
				so if we decide later to inject them instead of search them manually through the APILocator we can easily do.

				<br><span class="wrong">WRONG</span>
				<pre><code>
				
public class CopyHostAssetsJob extends DotStatefulJob {

     ...
     public void myInternalMethod () {
          ...
          HostAPI hostAPI = APILocator.getHostAPI(); //WRONG locally defined dependency
          UserAPI userAPI = APILocator.getUserAPI();
          ...
          
          
     }
     ...
}
				</code></pre>
				
				<br><span class="good">GOOD</span>
				<pre><code>
public class CopyHostAssetsJob extends DotStatefulJob {
     private HostAPI hostAPI;
     private UserAPI userAPI;

     public CopyHostAssetsJob () {
          hostAPI = APILocator.getHostAPI();
          userAPI = APILocator.getUserAPI();
     }
     ...
}
				</code></pre>
				
			</li>	
			<li>
				All source folders must compile before commit, and that includes the test folder. 
				So make sure you have the test folder in your Eclipse build path.
			</li>
			<li>
				Exceptions like DotHibernateException are low level exceptions that APIs are not meant to throw instead they should throw its parent higher
				level ones like DotDataException. API methods should only throw DotDataException and DotSecurityException.
			</li>
			<li>
				If there is already an API for the object you need to manipulate then use it instead of using the old/deprecated factories. If the API is missng
				the method you need then discuss with a lead developer of the core team about the needed method and add it after approval.
			</li>	
			<li>
				Never throw or catch a generic Exception class, it is very bad practice to do a catch all exception kind of handlers.
			</li>	
		</ol>
	</div>
	<!-- END Java Tab -->

	
	
	<!-- START Java Tab -->
	<div id="Compiling" dojoType="dijit.layout.ContentPane" title="Compiling and Commiting">
		<br>
		<ol>
			<li>
				Make sure JSP compiles, use ant deploy, before committing
			</li>
			<li>
				All source folders must compile before commit, and that includes the test folder. 
				So make sure you have the test folder in your Eclipse build path.
			</li>
			<li>
				Include FULL path to the Jira task in the commit coment. E.G. http://jira.dotmarketing.net/browse/DOTCMS-XXX
			</li>
		</ol>
	</div>
	<!-- END Java Tab -->

</div>
<!-- END TABS -->




<div id="ft">
	<div style="float:right;margin-top:10px;">
		<script language="JavaScript" type="text/javaScript">document.write((new Date()).getFullYear());</script> &copy; dotCMS Inc. All rights reserved.
	</div>
</div>



</body>
</html>
