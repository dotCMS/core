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
    const headerError = 'Error on upload plugin';
	// Refresh the list After upload a plugin
	const dotAssetDropZone = document.querySelector('dot-asset-drop-zone');
	dotAssetDropZone.addEventListener('uploadComplete', function(){ getBundlesData() });

	const uploadPlugin = ({files, onSuccess, updateProgress, onError}) => {
        // Check if we get an array of files otherwise create array.
        const data = Array.isArray(files) ? files : [files];
        
        // Create Form Data
        const formData = new FormData();
        data.forEach((file) => formData.append('file', file));
        formData.append('json', '{}');
        
        console.log(formData);
        console.log(files, data);
        
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
            if (request.status === 200) {
                onSuccess();
                return JSON.parse(request.response);
            } else {
                throw request;
            }
        })
        .catch((request) => {
            const response = JSON.parse(request.response);
            const errors = response.errors;
            onError(headerError, errors[0].message);
            throw errors;
        });
    }

    dotAssetDropZone.customUploadFiles = uploadPlugin;
</script>