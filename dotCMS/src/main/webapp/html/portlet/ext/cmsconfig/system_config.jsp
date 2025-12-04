<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<style>
    #systemInfoDiv{
       margin: 20px 20px;
    }

    #anchorTOCDiv {
        max-width:600px;
        margin: 20px;
        font-size: 1.5rem;
        display:grid;
        text-align: center;
        grid-gap: 10px;

        grid-template-columns: auto auto auto auto auto auto auto auto auto auto auto auto;
    }

    #anchorTOCDiv div{
        font-size: 1rem;
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
        font-size:1.25rem;
        text-transform: capitalize;
        padding:10px;

        border:0px solid black;

    }


</style>


<script>
    const envLoadedKeys = new Array();
    const overrideData = new Map();
    const allConfigData =  new Map();

    const addToRewriteSystemTable = ()=> {

        var key = document.getElementById("overrideKey").value;
        var value = document.getElementById("overrideValue").value;
        if (key === "" || value === "" || key.trim().length == 0 || value.trim().length == 0) {
            alert("Key and Value are required");
            return;
        }
        key=key.trim();
        value=value.trim();
        if(_saveConfigKeyAPI(key,value)) {
            overrideData.set(key,value);

            renderOverrideTable();
        }

    }

    const removeRewriteSystemTable = (key)=> {

        if (key === "" || key.trim().length == 0 ) {
            alert("Key is required");
            return;
        }
        key=key.trim();

        document.getElementById("deleteKeyDialog").close();

        if(_deleteConfigKeyAPI(key)) {
            overrideData.delete(key);
            renderOverrideTable();
        }

    }


    // owasp recommended escaping
    const escapeHTML = (unsafe) => {
        return unsafe.replace(
            /[\u0000-\u002F\u003A-\u0040\u005B-\u0060\u007B-\u00FF]/g,
            c => '&#' + ('000' + c.charCodeAt(0)).slice(-4) + ';'
        )
    }






    const _deleteConfigKeyAPI = async (key) => {
        document.body.style.cursor = 'wait';
        var data = {
            key: key
        };
        var worked = await fetch('/api/v1/system-table/_delete', {
            method: 'DELETE',
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
            return true;
        })
        .catch((response) => {
            document.body.style.cursor = 'default'
            console.log("error response:" ,response);
            alert("Error in deleting: " + response );
            return false;
        });
        return worked;
    }

    const _saveConfigKeyAPI = async (key, value) => {

        document.body.style.cursor = 'wait'
        const data = {
            key: key,
            value: value
        };
        var worked = await fetch('/api/v1/system-table/', {
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
            return true;
        })
        .catch((response) => {
            document.body.style.cursor = 'default'
            console.log("error response:", response);
            alert("Error in saving: " + response);
            return false;
        });
        return worked;
    }

    const showDeleteDialog = (key) => {
        if(document.getElementById("deleteKeyDialog") != null) {
            document.getElementById("deleteKeyDialog").remove();
        }

        const deleteDialog = document.createElement("dialog");
        deleteDialog.id = "deleteKeyDialog";
        var div = document.createElement("div");
        div.style.padding = "20px";
        div.style.textAlign = "center";
        div.innerHTML = "<%= LanguageUtil.get(pageContext,"Are you sure you want to delete the key") %>:<br> <b>" + escapeHTML(key) + "</b>";
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
            document.getElementById("deleteKeyDialog").close();
            document.getElementById("deleteKeyDialog").remove();
        }
        div.appendChild(cancelButton);
        var deleteButton = document.createElement("button");
        deleteButton.className = "dijit dijitReset dijitInline dijitButton dijitButtonDanger";
        deleteButton.style.cursor = "pointer";
        deleteButton.innerHTML = "Delete";
        deleteButton.onclick = () => removeRewriteSystemTable(key);
        div.appendChild(deleteButton);
        deleteDialog.appendChild(div);
        document.body.appendChild(deleteDialog);
        deleteDialog.showModal();
    }



    // Adds a row to the table
    const createPropertyRow = (key, value) => {
        const tr = document.createElement("tr");
        tr.id = "overrideRow" + key;

        const th = document.createElement("th");
        th.className = "propTh";

        var escapedText = document.createTextNode(key);
        th.appendChild(escapedText);
        tr.appendChild(th);

        const td = document.createElement("td");
        td.className = "propTd";

        escapedText = document.createTextNode(value);
        td.appendChild(escapedText);
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
        deleteButton.id = "deleteButton" + escapeHTML(key);
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



     const initLoadConfigsAPI = async () => {
         const currentDiv = document.getElementById("systemInfoDiv");
         currentDiv.innerHTML = "";
         var worked = await fetch('/api/v1/jvm')
         .then(response => {
             if (response.ok) {
                 return response.json();
             }
             return Promise.reject(response);

         })
         .then(data => {
             Object.entries(data).forEach(([key, value]) => {

                 if (typeof value === 'object' && value !== null) {
                     let localmap = new Map();
                     Object.entries(value).forEach(([nestedKey, nestedValue]) => {
                         localmap.set(nestedKey, nestedValue);
                     });
                     allConfigData.set(key, localmap);
                 } else {
                     allConfigData.set(key, value);
                 }
             });

             for(const [key, value] of allConfigData.get("configOverrides")) {
                 overrideData.set(key, value);
             }
             for(const [key, value] of allConfigData.get("environment")) {
                 envLoadedKeys.push(key);
             }


             return true;
         })
         .catch((response) => {
             document.body.style.cursor = 'default'
             console.log("error response:", response);
             alert("Error in loading: " + response);
             return false;
         });


         if (worked) {
             renderSystemTables();
             renderOverrideTable();
         }
         else{
                currentDiv.innerHTML = "Error loading data";
         }


     }






    const renderSystemTables = () => {


        const keys = ["release", "jvm", "host", "system", "environment", "configOverrides"];
        const currentDiv = document.getElementById("systemInfoDiv");
        currentDiv.innerHTML = "";
        const headerDiv = document.createElement("h2");


        const release = "release";



        headerDiv.innerHTML = "Version: " + allConfigData.get("release").get("releaseInfo")
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
            myDiv.id = "propDiv" + escapeHTML(key);
            const fieldSet = document.createElement("fieldset");
            fieldSet.className = "propFieldSet";
            const label = document.createElement("legend");
            label.className = "propLabel";
            label.innerHTML = escapeHTML(key);
            const table = document.createElement("table");

            myDiv.appendChild(fieldSet);
            currentDiv.appendChild(myDiv);
            fieldSet.appendChild(label);

            table.className = "listingTable";
            fieldSet.appendChild(table)
            var myData = allConfigData.get(key);

            for (const [key, value] of myData) {
                table.appendChild(createPropertyRow(key, value));
            }
        });
    }

    const renderOverrideTable = () => {

        if(document.getElementById("propDivconfigOverrides") != null) {
            document.getElementById("propDivconfigOverrides").remove();
        }

        const currentDiv = document.getElementById("systemInfoDiv");
        const myDiv = document.createElement("div");
        myDiv.id = "propDivconfigOverrides" ;
        const fieldSet = document.createElement("fieldset");
        fieldSet.className = "propFieldSet";
        const label = document.createElement("legend");
        label.className = "propLabel";
        label.innerHTML = "<%= LanguageUtil.get(pageContext,"Config Overrides") %>";
        const table = document.createElement("table");

        myDiv.appendChild(fieldSet);
        currentDiv.appendChild(myDiv);
        fieldSet.appendChild(label);
        table.id = "configOverrides";
        table.className = "listingTable";
        fieldSet.appendChild(table)

        for (const [key, value] of overrideData) {
            table.appendChild(createSystemTableRow(key, value));
        }

        const overrideTable = document.getElementById("configOverrides");
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
        addButton.onclick = () => addToRewriteSystemTable();
        td2.appendChild(addButton);
        tr.appendChild(td2);

    }

</script>

<div id="systemInfoDiv"></div>
