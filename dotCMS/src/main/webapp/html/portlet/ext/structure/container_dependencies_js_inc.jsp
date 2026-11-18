<%--
  Content type delete check shared by view_structure.jsp and edit_structure.jsp. Included inside
  their <script> block; the including page defines processDelete() and the dependenciesDialog.
--%>
    /**
     * Loads the containers that use a content type from the containers REST endpoint, which only
     * returns the containers the current user can read, archived ones included.
     */
    function loadContainersUsingType(typeId) {
        return fetch('/api/v1/containers?content_type=' + encodeURIComponent(typeId) + '&archive=true&per_page=100')
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Containers request failed with status ' + response.status);
                }
                return response.json();
            });
    }

    /**
     * Deletes the content type when no container uses it; otherwise lists the containers in the
     * dependencies dialog. Values are written as text. A response without the container list or its
     * total is treated as a failure, so the type is not deleted.
     */
    function handleDepResponse(data) {
        if (!data || !Array.isArray(data.entity) || !data.pagination
                || !Number.isInteger(data.pagination.totalEntries) || data.pagination.totalEntries < 0) {
            throw new Error('Unexpected containers response');
        }
        var containers = data.entity;
        var total = data.pagination.totalEntries;

        if (total !== 0) {
            var table = document.createElement('table');
            table.className = 'listingTable';
            var headerRow = table.createTHead().insertRow();
            [
                '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "TITLE")) %>',
                '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "IDENTIFIER")) %>',
                '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "INODE")) %>'
            ].forEach(function (label) {
                var th = document.createElement('th');
                th.textContent = label;
                headerRow.appendChild(th);
            });

            var body = table.createTBody();
            containers.forEach(function (container) {
                var row = body.insertRow();
                var title = container.hostName ? container.hostName + ' : ' + container.title : container.title;
                [title, container.identifier, container.inode].forEach(function (value) {
                    row.insertCell().textContent = value;
                });
            });

            dojo.byId("depDiv").replaceChildren(document.createElement('br'), table);

            dijit.byId("dependenciesDialog").show();
        } else {
            processDelete();
        }
    }

    /**
     * Loads the containers that use a content type and either deletes the type or shows them. A
     * failure is logged to the console and reported to the user; the type is not deleted.
     */
    function checkContainersThenDelete(typeId) {
        loadContainersUsingType(typeId)
            .then(handleDepResponse)
            .catch(function (error) {
                console.error(error);
                alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "an-unexpected-error-occurred")) %>');
            });
    }
