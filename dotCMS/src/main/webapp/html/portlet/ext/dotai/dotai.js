const changeTabs = async () => {
    let ele = document.getElementsByName('tab-group');
    for (i = 0; i < ele.length; i++) {
        document.getElementById('content-' + (i + 1)).style.display = ele[i].checked ? "block" : "none";
        document.getElementById('content-' + (i + 1)).className = ele[i].checked ? +" dijitTabChecked" : "";

    }
    if (ele[0].checked) {
        tab1()
    } else if (ele[1].checked) {
        tab2()
    } else if (ele[2].checked) {
        tab3()
    } else if (ele[3].checked) {
        tab4()
    }
};

const dotAiState = {};


const refreshIndexes = async () => {
    dotAiState.indexes = [];
    return fetch("/api/v1/ai/embeddings/indexCount")
        .then(response => response.json())
        .then(options => {
            for (const [key, value] of Object.entries(options.indexCount)) {
                let entry = {}
                entry.name = key;
                entry.contents = value.contents;
                entry.fragments = value.fragments;
                entry.tokenTotal = value.tokenTotal;
                entry.tokensPerChunk = value.tokensPerChunk;
                entry.contentTypes=value.contentTypes!==null && value.contentTypes!=="" ? value.contentTypes.replaceAll(",","\n") : "";
                dotAiState.indexes.push(entry);

            }
        })

};


const refreshConfigs = async () => {
    dotAiState.config = {};

    return fetch("/api/v1/ai/completions/config")
        .then(response => response.json())
        .then(configProps => {
            const entity = {}
            for (const [key, value] of Object.entries(configProps)) {
                entity[key] = value
            }
            dotAiState.config = entity;
        });
};


const refreshTypesAndFields = async () => {
    const contentTypes = [];

    return fetch("/api/v1/contenttype?orderby=modDate&direction=DESC&per_page=40")
        .then(response => response.json())
        .then(options => {

            for (i = 0; i < options.entity.length; i++) {
                let type = options.entity[i];
                let entry = {};
                entry

            }
        })
};


const reinitializeDatabase = async () => {
    if (!confirm("Are you sure you want to recreate the whole db?  You will lose all saved embeddings.")) {
        return;
    }
    const contentTypes = [];

    await fetch("/api/v1/ai/embeddings/db", {
        method: 'DELETE',
        headers: {
            'Content-type': 'application/json'
        }
    })
        .then(response => response.json())
        .then(data => {
            document.getElementById("indexingMessages").innerHTML = "DB dropped/created:" + data.created;
            setTimeout(tab2, 4000);
        });
};


const writeIndexesToDropdowns = async () => {
    const indexName = document.getElementById("indexNameChat");

    // Clear all existing options
    indexName.innerHTML = '';

    let optionsAdded = 0; // Add a counter to track the number of options added

    // Add a new option for each index in dotAiState.indexes
    for (i = 0; i < dotAiState.indexes.length; i++) {
        if (dotAiState.indexes[i].name === "cache") {
            continue;
        }
        const newOption = document.createElement("option");
        newOption.value = dotAiState.indexes[i].name;
        newOption.text = `${dotAiState.indexes[i].name}   - (contents:${dotAiState.indexes[i].contents})`
        indexName.appendChild(newOption);
        optionsAdded++;
    }

    // If no options were added, add a "Select an Index" option
    if (optionsAdded === 0) {
        const defaultOption = document.createElement("option");
        defaultOption.text = 'Select an Index';
        defaultOption.disabled = true;
        indexName.appendChild(defaultOption);
    }
};

const writeModelToDropdown = async () => {

    // --- Fallback ---
    if (dotAiState && dotAiState.config && dotAiState.config.availableModels) {
        for (let i = 0; i < dotAiState.config.availableModels.length; i++) {
            const model = dotAiState.config.availableModels[i];
            if (model.type !== 'TEXT') continue;

            const newOption = document.createElement("option");
            newOption.value = model.name;
            newOption.text = model.current
                ? `${model.name} (default)`
                : model.name;
            if (model.current) newOption.selected = true;
            modelName.appendChild(newOption);
        }
    }
};



const writeConfigTable = async () => {

    const configTable = document.getElementById("configTable")
    //console.log("config", dotAiState.config)

    configTable.innerHTML = "";

    const table = document.createElement("table");
    table.className = "propTable";
    configTable.appendChild(table);

    for (const [key, value] of Object.entries(dotAiState.config)) {
        //console.log(key)
        const tr = document.createElement("tr");
        tr.style.borderBottom = "1px solid #eeeeee"
        const th = document.createElement("th");
        th.className = "propTh";
        const td = document.createElement("td");
        td.className = "propTd";
        table.appendChild(tr)
        tr.appendChild(th);
        tr.appendChild(td);
        th.innerHTML = key;
        td.innerHTML = value;
    }
};

const writeIndexManagementTable = async () => {
    const indexTable = document.getElementById("indexManageTable")
    const oldChunks = dotAiState.numberOfChunks !== undefined ? dotAiState.numberOfChunks : 0;
    indexTable.innerHTML = "";

    let tr = document.createElement("tr");
    tr.style.fontWeight = "bold";
    tr.style.textAlign = "left"
    tr.style.borderBottom = "1px solid black"
    let td1 = document.createElement("th");
    let td2 = document.createElement("th");
    let td3 = document.createElement("th");
    let td4 = document.createElement("th");
    let td5 = document.createElement("th");
    let td6 = document.createElement("th");

    td1.className = "hTable"
    td2.className = "hTable"
    td3.className = "hTable"
    td4.className = "hTable"
    td5.className = "hTable"
    td6.className = "hTable"

    td1.innerHTML = "Index"
    td2.innerHTML = "Chunks"
    td3.innerHTML = "Content"
    td4.innerHTML = "Tokens"
    td5.innerHTML = "Tokens per Chunk"


    tr.append(td1);
    tr.append(td2);
    tr.append(td3);
    tr.append(td4);
    tr.append(td5);
    tr.append(td6);

    indexTable.append(tr)
    let newChunks = 0;

    dotAiState.indexes.map(row => {
        //console.log("row", row)
        newChunks += row.fragments;
        const cost = row.name === 'cache' ? "(~$" + ((parseInt(row.tokenTotal) / 1000) * 0.0001).toFixed(2).toLocaleString() + ")" : "";

        tr = document.createElement("tr");
        tr.style.borderBottom = "1px solid #eeeeee"
        td1 = document.createElement("td");
        td1.style.textAlign = "center";
        td1.style.fontWeight = "bold";

        td2 = document.createElement("td");
        td2.style.textAlign = "center";
        td3 = document.createElement("td");
        td3.style.textAlign = "center";
        td4 = document.createElement("td");
        td4.style.textAlign = "center";
        td5 = document.createElement("td");
        td5.style.textAlign = "center";
        td6 = document.createElement("td");
        td6.style.textAlign = "center";
        td1.innerHTML = `<div title="${row.contentTypes}" style=cursor:pointer;">${row.name}</div>`;
        td2.innerHTML = row.fragments.toLocaleString();
        td3.innerHTML = row.contents;
        td4.innerHTML = `${row.tokenTotal.toLocaleString()} ${cost}`;
        td4.style.whiteSpace = "nowrap"
        td5.innerHTML = row.tokensPerChunk.toLocaleString();
        td6.innerHTML = `<a href="#" onclick="doDeleteIndex('${row.name}')">delete</a>`

        tr.append(td1);
        tr.append(td2);
        tr.append(td3);
        tr.append(td4);
        tr.append(td5);
        tr.append(td6);
        indexTable.append(tr)
    })


    tr = document.createElement("tr");
    td1 = document.createElement("td");
    td1.id = "indexingMessages"
    td1.colSpan = 5
    tr.append(td1);
    td1 = document.createElement("td");
    td1.style.textAlign = "center";
    td1.colSpan = 1;
    td1.innerHTML = `<a href="#" onclick="reinitializeDatabase()">rebuild db</a>`;
    tr.append(td1);
    indexTable.append(tr)

    dotAiState.numberOfChunks = newChunks;
    if (newChunks !== oldChunks) {
        //console.log("reloading: newChunks:" + newChunks + " oldChunks:" +  oldChunks)
        setTimeout(() => {
            refreshIndexes()
                .then(() => {
                    writeIndexManagementTable();
                })
        }, 5000);
    }
}

const tab1 = () => {
    refreshIndexes()
        .then(() => {
            writeIndexesToDropdowns();
            writeIndexManagementTable();
        });
}

const tab3 = () => {
    refreshIndexes()
        .then(() => {
            writeIndexesToDropdowns();
            writeIndexManagementTable();
        });


};

const tab2 = () => {

    displayImagePrompts()
    document.getElementById("imagePrompt").value = (preferences().imageQuery) ? preferences().imageQuery : "" ;

};

const tab4 = () => {

    refreshConfigs().then(() => {
        writeConfigTable();
    });
};

const preferences = () => {
    const prefString = localStorage.getItem("com.dotcms.ai.settings") !== null ? localStorage.getItem("com.dotcms.ai.settings") : "{}";
    //console.log("loading prefs:", JSON.stringify(prefString))
    return JSON.parse(prefString)

};

const savePreferences = (prefs) => {

    //console.log("saving prefs:", JSON.stringify(prefs))
    return localStorage.setItem("com.dotcms.ai.settings", JSON.stringify(prefs));
};

const toggleAdvancedSearchOptionsTable = () => {
    const showingAdvanced = document.getElementById("advancedSearchOptionsTable").style.display;
    if (showingAdvanced === "none") {
        document.getElementById("advancedSearchOptionsTable").style.display = "block"
        document.getElementById("showAdvancedArrow").className += " rotated"
    } else {
        document.getElementById("advancedSearchOptionsTable").style.display = "none"
        document.getElementById("showAdvancedArrow").className = document.getElementById("showAdvancedArrow").className.replaceAll(" rotated", "");
    }

    const prefs = preferences();
    prefs.showAdvancedSearchOptionsTable = showingAdvanced === "none"
    savePreferences(prefs);


}

const toggleWhatToEmbedTable = () => {
    const showingAdvanced = document.getElementById("whatToEmbedTable").style.display;
    if (showingAdvanced === "none") {
        document.getElementById("whatToEmbedTable").style.display = "block"
        document.getElementById("showOptionalEmbeddingsArrow").className += " rotated"
    } else {
        document.getElementById("whatToEmbedTable").style.display = "none"
        document.getElementById("showOptionalEmbeddingsArrow").className = document.getElementById("showOptionalEmbeddingsArrow").className.replaceAll(" rotated", "");
    }
    const prefs = preferences();
    prefs.showWhatToEmbedTable = showingAdvanced === "none"
    savePreferences(prefs);
}

const clearPrompt = (idToClear) => {
    document.getElementById(idToClear).value = "";
    const prefs = preferences();
    prefs[idToClear] = null;
    savePreferences(prefs);
    showClearPrompt(idToClear);
}

const showClearPrompt = (idToClear) => {
    if (document.getElementById(idToClear).value && document.getElementById(idToClear).value !== "") {
        document.getElementById(idToClear + "X").style.visibility = "";
    } else {
        document.getElementById(idToClear + "X").style.visibility = "hidden";
    }
}


const setUpValuesFromPreferences = () => {
    const prefs = preferences();

    if (prefs.showWhatToEmbedTable && prefs.showWhatToEmbedTable !== false) {
        toggleWhatToEmbedTable();
    }

    if (prefs.showAdvancedSearchOptionsTable && prefs.showAdvancedSearchOptionsTable !== false) {
        toggleAdvancedSearchOptionsTable();
    }


    const textAreas = ["searchQuery", "contentQuery", "velocityTemplate"];
    for (i = 0; i < textAreas.length; i++) {
        const field = textAreas[i];
        if (prefs[field] && prefs[field] !== undefined && prefs[field] !== null) {
            document.getElementById(field).value = prefs[field];
        }
        showClearPrompt(textAreas[i])
    }
}


const showResultTables = () => {
    const searching = document.getElementById("searchResponseType").checked;
    if (searching) {
        document.getElementById("answerChat").style.display = "none";
        document.getElementById("semanticSearchResults").style.display = "block";
    } else {
        const prompt = "Current Prompt: \n\n" + dotAiState.config["com.dotcms.ai.completion.text.prompt"];
        document.getElementById("answerChat").placeholder = prompt.replaceAll('\\n', '\n').replaceAll("\\\"", "\"")
        document.getElementById("answerChat").style.display = "block";
        document.getElementById("semanticSearchResults").style.display = "none";
    }


    const prefs = preferences();
    prefs.showResultsSearching = searching;
    savePreferences(prefs);

}

const readImagePromptsFromPrefs = (prefs)=> {

    //console.log("prefs:", prefs)
    const storedData = prefs.imagePrompts;
    //console.log("storedData", storedData)
    if (storedData && Array.isArray(storedData)) {
        return storedData;
    }
    else if(storedData){
        return JSON.parse(storedData);
    } else {
        return [];
    }
}

const readImagePrompts = ()=> {

    return readImagePromptsFromPrefs(preferences())
}


const addPromptToArrayAndStore = (prefs) => {
    const inputValue=prefs.imageQuery;
    const oldData = readImagePromptsFromPrefs(prefs);

    const newData = []
    for(let i=0;i<oldData.length;i++){
        if(oldData[i] !== inputValue){
            newData.push(oldData[i]);
        }
        if(newData.length>15){
            break;
        }
    }
    newData.unshift(inputValue);

    prefs.imagePrompts = newData;
    console.log("prefs",prefs)
    savePreferences(prefs);
    displayImagePrompts();
}

const displayImagePrompts = () => {

    const dataArray = readImagePrompts();
    const listContainer = document.getElementById('image-prompts');
    listContainer.innerHTML = ''; // Clear previous content

    dataArray.forEach((item, index) => {
        const listItem = document.createElement('li');
        listItem.innerHTML = `<a href="javascript:loadPrompt(${index})">${item}</a>`;
        listContainer.appendChild(listItem);
    });
}

const loadPrompt = (num) =>{
    const dataArray = readImagePrompts();
    document.getElementById("imagePrompt").value = dataArray[num];
}

const doImageJson = () => {
    document.getElementById("submitImage").style.display = "none";
    document.getElementById("loaderImage").style.display = "block";
    setTimeout(function () {
        doImageJsonDebounced();
    }, 300);
}

const doImageJsonDebounced = async () => {
    const formDataRaw = new FormData(document.getElementById("imageForm"))

    const formData = Object.fromEntries(Array.from(formDataRaw.keys()).map(key => [key, formDataRaw.getAll(key).length > 1 ? formDataRaw.getAll(key) : formDataRaw.get(key)]))

    const prompt = document.getElementById("imagePrompt").value;

    formData.prompt = prompt;

    if (formData.prompt == undefined || formData.prompt.trim().length == 0) {
        alert("please enter a prompt");
        document.getElementById("submitImage").style.display = "";
        document.getElementById("loaderImage").style.display = "none";
        return;
    }



    const prefs = preferences();
    prefs.imageQuery = formData.prompt.trim();
    prefs.imageSize = formData.size.trim();
    addPromptToArrayAndStore(prefs);

    //console.log("formData", formData)

    const response = await fetch('/api/v1/ai/image/generate', {
        method: "POST", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    });





    response.json().then(json => {

        const temp =  json.response;
        const width = formData.size.split("x")[0];
        const height = formData.size.split("x")[1];
        const jsonString=JSON.stringify(json, 2);
        const rewrittenPrompt = json.revised_prompt;
        const imageTemplate =`
            <div style="width:100%;max-width:800px;position:relative;text-align:center;border:1px solid silver;padding:1rem;">
                <a href="/dA/${temp}/asset.png" target="_blank">
                    <img src="/dA/${temp}/asset.png" style="max-width:750px;max-height:750px;display: block;margin:auto;"  />
                </a>
                
                <div style="padding:1rem;margin:auto;text-align: center">
                    <button id="saveImageButton" class="button dijit dijitReset dijitInline dijitButton"
                            onclick="saveImage('${temp}')">
                        Save
                    </button><br>
                    <div id="imageSavedMessage">&nbsp;</div>
                </div>
                <div style="border:1px solid silver;padding:1rem;margin:auto;text-align: left">
                    <b>OpenAI Prompt (Rewritten):</b> <br>
                    ${rewrittenPrompt}
                </div>
                <div style="border:1px solid silver;padding:1rem;margin:auto;text-align: left">
                    <b>JSON Response:</b> <br>${jsonString}
                </div>
            </div>

    `



        document.getElementById("imageRequest").innerHTML = imageTemplate;
        document.getElementById("submitImage").style.display = "";
        document.getElementById("loaderImage").style.display = "none";
    });

}

const saveImage = async (tempId) => {


    const contentlets = [{
        baseType: 'dotAsset',
        asset: tempId,
        tags: 'dot:openai'
    }];

    console.log("newAsset", contentlets)

    const response = await fetch('/api/v1/workflow/actions/default/fire/PUBLISH', {
    method: "POST", body: JSON.stringify({ contentlets }), headers: {
        "Content-Type": "application/json"
    }
    })
    .then(response => response.json())
    .catch(data =>{
        console.log("error", data)
        document.getElementById("imageSavedMessage").innerHTML="error:" + data;
        setTimeout(function () {
            clearSaveMessage();
        }, 10000);


    })
    .then(data => {
        //console.log("worked", data);
        document.getElementById("imageSavedMessage").innerHTML="content saved";

        setTimeout(function () {
            clearSaveMessage();
        }, 3000);

    });

}

const clearSaveMessage =() =>{
    document.getElementById("imageSavedMessage").innerHTML="&nbsp;";
}



const doSearchChatJson = () => {
    document.getElementById("submitChat").style.display = "none";
    document.getElementById("loaderChat").style.display = "block";
    setTimeout(function () {
        doSearchChatJsonDebounced();
    }, 500);
}


const doSearchChatJsonDebounced = async () => {

    const formDataRaw = new FormData(document.getElementById("chatForm"))
    const formData = Object.fromEntries(Array.from(formDataRaw.keys()).map(key => [key, formDataRaw.getAll(key).length > 1 ? formDataRaw.getAll(key) : formDataRaw.get(key)]))

    const prompt = document.getElementById("searchQuery").value;
    formData.prompt = prompt;


    const responseType = formData.responseType
    delete formData.responseType;
    if (formData.prompt == undefined || formData.prompt.trim().length == 0) {
        alert("please enter a query/prompt");
        document.getElementById("submitChat").style.display = "";
        document.getElementById("loaderChat").style.display = "none";
        return;
    }

    const prefs = preferences();
    prefs.searchQuery = formData.prompt.trim();
    prefs.lastIndex = formData.indexName.trim();
    savePreferences(prefs);
    if (responseType === "search") {
        doSearch(formData)

    } else if (responseType === "json") {
        return doJsonResponse(formData);
    } else {
        return doChatResponse(formData);
    }
}


const doDeleteIndex = async (indexName) => {
    if (!confirm("Are you sure you want to delete " + indexName + "?")) {
        return;
    }
    let formData = {};
    formData.indexName = indexName;
    //console.log("formData", formData)
    const response = await fetch('/api/v1/ai/embeddings', {
        method: "DELETE", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    })
        .then(response => response.json())
        .then(data => {
            refreshIndexes()
                .then(() => {
                    writeIndexesToDropdowns();
                    writeIndexManagementTable();
                });
        });
}


const doBuildIndexWithDebounceBtn = () => {

    document.getElementById("submitBuildIndexBtn").style.display = "none";
    document.getElementById("loaderIndex").style.display = "block";
    setTimeout(function () {
        doBuildIndexWithDebounce();
    }, 500);
}

const doBuildIndexWithDebounce = async () => {

    const formDataRaw = new FormData(document.getElementById("createUpdateIndex"))
    const formData = Object.fromEntries(Array.from(formDataRaw.keys()).map(key => [key, formDataRaw.getAll(key).length > 1 ? formDataRaw.getAll(key) : formDataRaw.get(key)]))

    if (formData.indexName === null || formData.indexName.trim().length == 0) {
        alert("Index Name is required");
        return;
    }

    if (formData.query === null || formData.query.trim().length == 0) {
        alert("Query is required");
        return;
    }
    delete formData["indexAddOrDelete"];
    const prefs = preferences();
    prefs.lastIndex = formData.indexName.trim();
    prefs.contentQuery = formData.query.trim()
    prefs.velocityTemplate = formData.velocityTemplate.trim()
    savePreferences(prefs);

    if(document.getElementById("indexDelCheckbox").checked){
        return deleteFromIndex(formData);
    }




    const response = await fetch('/api/v1/ai/embeddings', {
        method: "POST", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    }).then(response => response.json())
        .then(data => {
            document.getElementById("indexingMessages").innerHTML = `Building index ${data.indexName} with ${data.totalToEmbed} to embed`
            setTimeout(clearIndexMessage, 5000);
        });

    resetBuildIndexLoader();
}

const deleteFromIndex = async (formData) => {

    let newData = {};
    newData.indexName = formData.indexName.trim();
    newData.deleteQuery=formData.query.trim();


    const response = await fetch('/api/v1/ai/embeddings', {
        method: "DELETE", body: JSON.stringify(newData), headers: {
            "Content-Type": "application/json"
        }
    }).then(response => response.json())
    .then(data => {
        document.getElementById("indexingMessages").innerHTML = `Deleting ${data.deleted} from index ${newData.indexName} `
        setTimeout(clearIndexMessage, 5000);
    });
    resetBuildIndexLoader();
}

const changeBuildIndexName = () =>{

    if(document.getElementById("indexDelCheckbox").checked){
        document.getElementById("submitBuildIndexBtn").innerHTML = document.getElementById("submitBuildIndexBtn").innerHTML.replace("Build", "Delete from");
    }else{
        document.getElementById("submitBuildIndexBtn").innerHTML = document.getElementById("submitBuildIndexBtn").innerHTML.replace("Delete from", "Build");
    }

}



const clearIndexMessage = async () => {
    document.getElementById("indexingMessages").innerHTML = "";
    refreshIndexes()
        .then(() => {
            writeIndexesToDropdowns();
            writeIndexManagementTable();
        });
}

const doJsonResponse = async (formData) => {

    const response = await fetch('/api/v1/ai/completions', {
        method: "POST", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    });
    response.json().then(json => {
        //console.log("json", json)
        document.getElementById("answerChat").value = json.openAiResponse.choices[0].message.content + "\n\n------\nJSON Response\n------\n" + JSON.stringify(json, null, 2);
    });
    resetLoader();
}


const doChatResponse = async (formData) => {

    const stream = document.getElementById("streamingResponseType").checked;

    formData.stream = true;
    let line = "";
    let lines = [];
    try {
        const response = await fetch('/api/v1/ai/completions', {
            method: "POST", body: JSON.stringify(formData), headers: {
                "Content-Type": "application/json"
            }
        });
        document.getElementById("answerChat").value = "";
        // Read the response as a stream of data
        const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();
        if (!reader) return;

        while (true) {
            const {value, done} = await reader.read();
            if (done) {
                //console.log("got a done:" + done);
                break;
            }
            //console.log(value);
            lines = (line + value).split('\ndata: ');
            for (line of lines) {

                line = line.replace(/^data: /, '').trim();
                if (line.length === 0) continue; // ignore empty message
                if (line.startsWith(':')) continue; // ignore sse comment message

                if (line === '[DONE]') {
                    break;
                }
                try {
                    const json = JSON.parse(line);
                    line = "";
                    const value = json.choices[0].delta.content;
                    if (value === undefined) {
                        continue;
                    }
                    document.getElementById("answerChat").value += value;
                } catch (e) {
                    // line is half sent, will append to the next value
                    console.log("line:" + line);
                }
            }
        }
    } catch (e) {

        console.log("got an error:", e);
        console.log("line:" + line);
        console.log("lines:" + lines);
    }
    resetLoader();
};


const doSearch = async (formData) => {

    //console.log("formData", formData)
    const semanticSearchResults = document.getElementById("semanticSearchResults");
    semanticSearchResults.innerHTML = "";


    const table = document.createElement("table");
    table.className = "aiSearchResultsTable";
    semanticSearchResults.appendChild(table);

    const truncateString = (str, num) => {
        if (str.length <= num) {
            return str
        }
        return str.slice(0, num) + '...'
    }


    fetch("/api/v1/ai/search", {
        method: "POST", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    })
        .then(response => response.json())
        .then(data => {

            let tr = document.createElement("tr");
            tr.style.fontWeight = "bold";

            let td1 = document.createElement("th");
            let td2 = document.createElement("th");
            let td3 = document.createElement("th");
            let td4 = document.createElement("th");

            td1.className = "hTable"
            td2.className = "hTable"
            td3.className = "hTable"
            td4.className = "hTable"

            td1.innerHTML = "Title"
            td2.innerHTML = "Matches"
            td3.innerHTML = "Distance"
            td4.innerHTML = "Top Match"

            tr.append(td1);
            tr.append(td2);
            tr.append(td3);
            tr.append(td4);
            tr.style.borderBottom = "1px solid #bbbbbb"
            table.append(tr)


            data.dotCMSResults.map(row => {
                //console.log("row", row)
                tr = document.createElement("tr");
                tr.style.borderBottom = "1px solid #eeeeee"
                td1 = document.createElement("td");

                td2 = document.createElement("td");
                td2.style.textAlign = "center"
                td3 = document.createElement("td");
                td3.style.textAlign = "center"
                td4 = document.createElement("td");
                td4.style.minWidth = "400px;"

                td1.innerHTML = `<a href="/dotAdmin/#/c/content/${row.inode}" target="_top">${row.title}</a>`;
                td2.innerHTML = row.matches.length;
                td3.innerHTML = parseFloat(row.matches[0].distance).toFixed(2);
                td4.innerHTML = truncateString(row.matches[0].extractedText, 200);

                tr.append(td1);
                tr.append(td2);
                tr.append(td3);
                tr.append(td4);
                table.append(tr)
            })
            resetLoader()
        })
};

const resetLoader = () => {
    document.getElementById("submitChat").style.display = "";
    document.getElementById("loaderChat").style.display = "none";
}

const resetBuildIndexLoader = () => {
    document.getElementById("submitBuildIndexBtn").style.display = "";
    document.getElementById("loaderIndex").style.display = "none";
}
