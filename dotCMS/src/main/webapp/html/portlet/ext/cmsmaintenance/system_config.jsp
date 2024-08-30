<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<style>
    #systemInfoDiv{
       margin: 20px 20px;
    }

    #anchorTOCDiv {
        max-width:600px;
        margin: 20px;
        font-size: 2em;
        display:grid;
        text-align: center;
        grid-gap: 10px;

        grid-template-columns: auto auto auto auto auto auto auto auto auto auto auto auto;
    }

    #anchorTOCDiv div{
        font-size: .7em;
        text-transform: capitalize;
    }

    .propTh{
        width:40%;
        text-align: right;
        vertical-align: top!important;
        overflow-wrap: break-word;
    }
    .propTd{
        font-family: monospace;
        max-width: 500px;
        vertical-align: top!important;
        word-wrap: break-word;

    }
    .propLabel{
        font-weight: normal;
        font-size:24px;
        text-transform: capitalize;
        padding:10px;

        border:0px solid black;

    }


</style>


<script>
    const envLoadedKeys = []

    const showConfirmDialog = (key) => {
        const deleteDialog = document.createElement("dialog");
        deleteDialog.id = "deleteKeyDialog" + key;
        var div = document.createElement("div");
        div.style.padding = "20px";
        div.style.textAlign = "center";
        div.innerHTML = "<%= LanguageUtil.get(pageContext,"This key exists, overwrite") %>? <br> <b>" + key + "</b>";
        deleteDialog.appendChild(div);
        div = document.createElement("div");
        div.style.display = "grid";
        div.style.gridTemplateColumns = "auto auto";
        div.style.gridGap = "50px";
        div.style.margin = "20 auto";
        var cancelButton = document.createElement("button");
        cancelButton.className = "dijit dijitReset dijitInline dijitButton dijitButtonFlat";
        cancelButton.style.cursor = "pointer";
        cancelButton.innerHTML = "Cancel";
        cancelButton.onclick = () => {
            document.getElementById("deleteKeyDialog" + key).close();
            document.getElementById("deleteKeyDialog" + key).remove();
        }
        div.appendChild(cancelButton);
        var deleteButton = document.createElement("button");
        deleteButton.className = "dijit dijitReset dijitInline dijitButton dijitButtonDanger";
        deleteButton.style.cursor = "pointer";
        deleteButton.innerHTML = "Delete";
        deleteButton.onclick = () => deleteConfigKey(key);
        div.appendChild(deleteButton);
        deleteDialog.appendChild(div);
        document.body.appendChild(deleteDialog);
        deleteDialog.showModal();

    }

    const findOrCreateRowWithKey = (key) => {
        const table = document.getElementById("configOverridesTable");
        for (let i = 0, row; row = table.rows[i]; i++) {
            if (row.cells[0].innerText !== undefined && row.cells[0].innerText.trim() === key.trim()) {
                return row;
            }
        }
        return table.insertRow(table.rows.length - 1);
    }

    const deleteConfigKey = (key) => {
        document.body.style.cursor = 'wait';
        var data = {
            key: key
        };
        fetch('/api/v1/system-table/_delete', {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            document.body.style.cursor = 'default'
            document.getElementById("deleteKeyDialog" + key).close();
            if (response.ok) {
                return response.json();
            }
            return Promise.reject(response);

        })
        .then(data => {

            const row = findOrCreateRowWithKey(key);
            row.innerHTML = "";
            row.remove();
        })
        .catch((response) => {
            document.body.style.cursor = 'default'
            console.log(response.status, response.statusText);
            document.getElementById("deleteKeyDialog" + key).close();
            alert("Error: " + response.status + " " + response.statusText);
            response.json().then((json) => {
                console.log("Error:", json);
            })
        });
    }

    const showDeleteDialog = (key) => {
        const deleteDialog = document.createElement("dialog");
        deleteDialog.id = "deleteKeyDialog" + key;
        var div = document.createElement("div");
        div.style.padding = "20px";
        div.style.textAlign = "center";
        div.innerHTML = "Are you sure you want to delete the key:<br> <b>" + key + "</b>";
        deleteDialog.appendChild(div);
        div = document.createElement("div");
        div.style.display = "grid";
        div.style.gridTemplateColumns = "auto auto";
        div.style.gridGap = "50px";
        div.style.margin = "20 auto";
        var cancelButton = document.createElement("button");
        cancelButton.className = "dijit dijitReset dijitInline dijitButton dijitButtonFlat";
        cancelButton.style.cursor = "pointer";
        cancelButton.innerHTML = "Cancel";
        cancelButton.onclick = () => {
            document.getElementById("deleteKeyDialog" + key).close();
            document.getElementById("deleteKeyDialog" + key).remove();
        }
        div.appendChild(cancelButton);
        var deleteButton = document.createElement("button");
        deleteButton.className = "dijit dijitReset dijitInline dijitButton dijitButtonDanger";
        deleteButton.style.cursor = "pointer";
        deleteButton.innerHTML = "Delete";
        deleteButton.onclick = () => deleteConfigKey(key);
        div.appendChild(deleteButton);
        deleteDialog.appendChild(div);
        document.body.appendChild(deleteDialog);
        deleteDialog.showModal();
    }



    const addConfigKey = () => {

        const key = document.getElementById("overrideKey").value;
        const value = document.getElementById("overrideValue").value;
        if (key === "" || value === "" || key.trim().length == 0 || value.trim().length == 0) {
            alert("Key and Value are required");
            return;
        }
        document.body.style.cursor = 'wait'
        const data = {
            key: key.trim(),
            value: value.trim()
        };
        fetch('/api/v1/system-table/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            document.body.style.cursor = 'default'
            if (response.ok) {
                return response.json();
            }
            return Promise.reject(response);

        })
        .then(data => {
            document.body.style.cursor = 'default'
            const table = document.getElementById("configOverridesTable");
            const insertRow = createSystemTableRow(key, value);
            var existingRow = findOrCreateRowWithKey(key);
            existingRow.innerHTML = insertRow.innerHTML;
            document.getElementById("deleteButton" + key).onclick = () => {
                showDeleteDialog(key)
            }

            document.getElementById("overrideKey").value = "";
            document.getElementById("overrideValue").value = "";
        })
        .catch((response) => {
            document.body.style.cursor = 'default'
            console.log(response.status, response.statusText);
            alert("Error: " + response.status + " " + response.statusText);
        });
    }

    // Adds a row to the table
    const createPropertyRow = (key, value) => {
        const tr = document.createElement("tr");
        tr.id = "overrideRow" + key;

        const th = document.createElement("th");
        th.className = "propTh";
        console.log("envLoadedKeys", envLoadedKeys);

        th.innerHTML = key;
        tr.appendChild(th);

        const td = document.createElement("td");
        td.className = "propTd";

        td.innerHTML = value;
        tr.appendChild(td);
        return tr;
    }

    // Adds a delete button to the row
    const createSystemTableRow = (key, value) => {

        const tr = createPropertyRow(key, value);
        
        if(envLoadedKeys.includes(key)) {
            var cellVal = tr.cells.item(0);
            cellVal.style.textDecoration = "line-through";
            cellVal =tr.cells.item(1);
            cellVal.style.textDecoration = "line-through";
        }
        const td2 = document.createElement("td");
        td2.style.textAlign = "right";
        td2.style.maxWidth = "100px";

        const deleteButton = document.createElement("button");
        deleteButton.id = "deleteButton" + key;
        deleteButton.innerHTML = "Delete";
        deleteButton.className = "dijit dijitReset dijitInline dijitButton";
        deleteButton.style.padding = "10px 30px";

        deleteButton.style.cursor = "pointer";

        deleteButton.onclick = () => {
            showDeleteDialog(key)
        }

        td2.appendChild(deleteButton);
        tr.appendChild(td2);
        return tr;

    }


    const showSystemVars = () => {
        const keys = ["release", "jvm", "host", "system", "environment", "configOverrides"];

        const currentDiv = document.getElementById("systemInfoDiv");
        currentDiv.innerHTML = "";
        fetch('/api/v1/jvm')
        .then(response => response.json())
        .then(data => {
            const headerDiv = document.createElement("h2");
            headerDiv.innerHTML = "Version: " + data.release.version + " (" + data.release.buildDate + ")";
            currentDiv.appendChild(headerDiv);

            const template = `
            <div><%= LanguageUtil.get(pageContext,"Info") %>:</div>
            <div><a href="#propDiv${keys[0]}">${keys[0]}</a></div>
            <div><a href="#propDiv${keys[1]}">${keys[1]}</a></div>
            <div><a href="#propDiv${keys[2]}">${keys[2]}</a></div>
            <div><a href="#propDiv${keys[3]}">${keys[3]}</a></div>
            <div><a href="#propDiv${keys[4]}">${keys[4]}</a></div>
            <div><a href="#propDiv${keys[5]}">${keys[5]}</a></div>
            `

            const anchorDiv = document.createElement("div");

            anchorDiv.id = "anchorTOCDiv";

            anchorDiv.style.border = "";
            anchorDiv.innerHTML = template;
            currentDiv.appendChild(anchorDiv)

            // render info tables
            keys.filter(key => key !== "configOverrides").forEach(key => {
                const myDiv = document.createElement("div");
                myDiv.id = "propDiv" + key;
                const fieldSet = document.createElement("fieldset");
                fieldSet.className = "propFieldSet";
                const label = document.createElement("legend");
                label.className = "propLabel";
                label.innerHTML = key;
                const table = document.createElement("table");

                myDiv.appendChild(fieldSet);
                currentDiv.appendChild(myDiv);
                fieldSet.appendChild(label);

                table.className = "listingTable";
                fieldSet.appendChild(table)

                Object.entries(data[key]).forEach(([key, value]) => {
                    table.appendChild(createPropertyRow(key, value));

                })
            });
            keys.filter(key => key === "environment").forEach(key => {
                Object.entries(data[key]).forEach(([key, value]) => {
                    envLoadedKeys.push(key);
                });
            });

            // render override table
            keys.filter(key => key === "configOverrides").forEach(key => {
                const myDiv = document.createElement("div");
                myDiv.id = "propDiv" + key;
                const fieldSet = document.createElement("fieldset");
                fieldSet.className = "propFieldSet";
                const label = document.createElement("legend");
                label.className = "propLabel";
                label.innerHTML = "<%= LanguageUtil.get(pageContext,"Config Overrides") %>";
                const table = document.createElement("table");

                myDiv.appendChild(fieldSet);
                currentDiv.appendChild(myDiv);
                fieldSet.appendChild(label);
                table.id = "configOverridesTable";
                table.className = "listingTable";
                fieldSet.appendChild(table)

                Object.entries(data[key]).forEach(([key, value]) => {
                    table.appendChild(createSystemTableRow(key, value));
                })

                const overrideTable = document.getElementById("configOverridesTable");
                const tr = document.createElement("tr");
                overrideTable.appendChild(tr);

                const th = document.createElement("th");
                th.style.verticalAlign = "top";
                th.className = "propTh";
                th.innerHTML = `
                    <input type='text' placeholder='Key Name' id='overrideKey' maxlength='255' class='dijit dijitReset dijit dijitReset dijitInline dijitLeft dijitTextBox' style='vertical-align: top' />
                    <br>

                `;
                tr.appendChild(th);

                const td = document.createElement("td");
                td.className = "propTd";
                td.innerHTML = "<textarea id='overrideValue' placeholder='Key Value' padding='20px' rows='10' class='dijit dijitReset dijit dijitReset dijitInline dijitTextArea'   />";
                tr.appendChild(td);

                const td2 = document.createElement("td");
                td2.style.textAlign = "right";
                td2.style.verticalAlign = "top";
                const addButton = document.createElement("button");
                addButton.innerHTML = "Add";
                addButton.className = "dijit dijitReset dijitInline dijitButton";
                addButton.style.padding = "10px 30px";

                addButton.style.cursor = "pointer";
                addButton.onclick = () => addConfigKey();
                td2.appendChild(addButton);
                tr.appendChild(td2);

            });

        });
    }





</script>

<div id="systemInfoDiv"></div>
