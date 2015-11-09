<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>

<div id="rules-engine-container" class="portlet-wrapper" style="height:800px">
  <iframe id="rulesIframe" name="rulesIframe" width="100%" height="100%" frameborder="0" style="width:100%;height:100%"></iframe>
</div>

<script>
  var localeParam = '';
  //Trying to get ISO Code from session and format to standard.
  var langIsoCode = '<%= request.getSession().getAttribute(Globals.LOCALE_KEY) %>';
  langIsoCode = langIsoCode.replace("_", "-");

  if(langIsoCode){
    localeParam = "?locale=" + langIsoCode;
  }

  //Add param to the rules engine iframe.
  document.getElementById("rulesIframe").src = "/html/js/_rulesengine" + localeParam;
</script>