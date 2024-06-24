	<%@page import="com.liferay.portal.language.LanguageUtil"%>

<script type="text/javascript" src="/html/portlet/ext/osgi/js.jsp" ></script>
<dot-asset-drop-zone dojoAttachPoint="dropzone">
<div class="portlet-wrapper">
 
	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">
			<li>
				<a href="javascript:bundles.show()"><%=LanguageUtil.get(pageContext, "OSGI")%></a>
			</li>
			<li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "OSGI-MANAGER")%></span></li>
		</ul>
		<div class="clear"></div>
	</div>
 
	<div id="osgiMain"></div>
</div>
</dot-asset-drop-zone>

<script type="application/javascript">
    const headerError = '<%=LanguageUtil.get(pageContext, "OSGI-Header-Error")%>';
    const dotAssetDropZone = document.querySelector('dot-asset-drop-zone');

    const uploadPlugin = ({ files, onSuccess, updateProgress, onError }) => {
        return bundles.handleUpload({
            files,
            onSuccess,
            updateProgress,
            onError
        });
    };

    // Custom Props
    dotAssetDropZone.customUploadFiles = uploadPlugin;
    dotAssetDropZone.acceptTypes = ['.jar'];
    dotAssetDropZone.typesErrorLabel = '<%=LanguageUtil.get(pageContext, "OSGI-Invalid-Extension-File")%>';
</script>
