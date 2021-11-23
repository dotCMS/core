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
</dot-asset-drop-zone">

<script type="application/javascript">
    const headerError = '<%=LanguageUtil.get(pageContext, "OSGI-Header-Error")%>';
    const dotAssetDropZone = document.querySelector('dot-asset-drop-zone');

	const uploadPlugin = ({files, onSuccess, updateProgress, onError}) => {
        // Check if we get an array of files otherwise create array.
        const data = Array.isArray(files) ? files : [files];

        // Create Form Data
        const formData = new FormData();
        data.forEach((file) => formData.append('file', file));
        formData.append('json', '{}');
        
        return new Promise((res, rej) => {
            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/osgi');
            xhr.onload = () => res(xhr);
            xhr.onerror = rej;

            // Get Upload Process
            if (xhr.upload && updateProgress) {
                xhr.upload.onprogress = (e) => {
                    const percentComplete = (e.loaded / e.total) * 100;
                    updateProgress(percentComplete);
                };
            }

            xhr.send(formData);

        }).then(async (request) => {
            if (request.status !== 200) {
                throw request;
            }
            onSuccess();
            dijit.byId('savingOSGIDialog').show();
            return JSON.parse(request.response);
        })
        .catch((request) => {
            const response = typeof (request.response) === 'string' ? JSON.parse(request.response) : request.response;
            const errorMesage = response.errors[0].message;
            onError(headerError, errorMesage);
            throw response;
        });
    }

    // Custom Props
    dotAssetDropZone.customUploadFiles = uploadPlugin;
    dotAssetDropZone.acceptTypes = ['.jar'];
    dotAssetDropZone.typesErrorLabel = '<%=LanguageUtil.get(pageContext, "OSGI-Invalid-Extension-File")%>';
</script>