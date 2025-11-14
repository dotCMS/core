<script type="application/javascript" src="/html/portlet/ext/dotai/dotai.js"></script>
<script>

    async function fetchNewAIConfiguration(modelName) {

            let ok = false;
            try {
                const response = await fetch("/api/v1/ai/configuration/chat/models/site/SYSTEM_HOST");
                console.log('models response: ', response);
                if (response.ok) {

                    const data =  await response.json();
                    console.log('models data: ', data);
                    // if there any models availables
                    if (data && data.entity && data.entity.length > 0) {
                        console.log("Models from endpoint:", data.entity);
                        data.entity.forEach(model => {
                            const newOption = document.createElement("option");
                            newOption.value = model.vendorModelPath;
                            newOption.text = `${model.vendor} - ${model.name}`;
                            modelName.appendChild(newOption);
                        });

                        ok = true;
                    }
                }
            } catch (err) {
                console.warn("Error loading the models, usign fallback:", err);
            }
    }

    async function fetchNewAIEmbeddingConfiguration(embeddingModelName) {

        let ok = false;
        try {
            const response = await fetch("/api/v1/ai/configuration/embedding/models/site/SYSTEM_HOST");
            console.log('models response: ', response);
            if (response.ok) {

                const data =  await response.json();
                console.log('embedding models data: ', data);
                // if there any models availables
                if (data && data.entity && data.entity.length > 0) {
                    console.log("Models from endpoint:", data.entity);
                    data.entity.forEach(model => {
                        const newOption = document.createElement("option");
                        newOption.value = model.vendorModelPath;
                        newOption.text = `${model.vendor} - ${model.name}`;
                        embeddingModelName.appendChild(newOption);
                    });

                    ok = true;
                }
            }
        } catch (err) {
            console.warn("Error loading the models, usign fallback:", err);
        }
    }


    dojo.addOnLoad(function () {
        console.log("dojo add On Load dotAI");
        setUpValuesFromPreferences();
        refreshIndexes()
            .then(() => {
                writeIndexesToDropdowns();
                writeIndexManagementTable();
            });

        refreshConfigs().then(() => {
            writeConfigTable();

            console.log("Calling writeModelToDropdown....");
            const modelName = document.getElementById("modelName");
            let options = modelName.getElementsByTagName('option');
            for (let i = options.length - 1; i >= 1; i--) {
                modelName.removeChild(options[i]);
            }

            let ok = fetchNewAIConfiguration(modelName);

            if(!ok) {
                writeModelToDropdown();
            }

            if (dotAiState.config["apiKey"] != "*****") {
                document.getElementById("openAIKeyWarn").style.display = "block";
            }

            const modelEmbeddingName = document.getElementById("embeddingModelName");
            let embedOptions = modelEmbeddingName.getElementsByTagName('option');
            for (let i = embedOptions.length - 1; i >= 1; i--) {
                modelEmbeddingName.removeChild(options[i]);
            }
            fetchNewAIEmbeddingConfiguration(modelEmbeddingName);
        });
        showResultTables();
    });
</script>
<link rel="stylesheet" type="text/css" href="/html/portlet/ext/dotai/dotai.css">

<div id="openAIKeyWarn"
     style="display: none;padding:20px; border-radius: 10px;color:indianred;border:1px solid indianred;margin:20px auto;max-width: 800px;text-align: center">
    Your OpenAI API key is not set. Please add a valid API key in your <a
        href="/dotAdmin/#/apps/dotAI/edit/SYSTEM_HOST"
        target="_top">App screen</a>.
</div>
<div id="container">
    <input id="tab-1" type="radio" name="tab-group" checked="checked" onclick="changeTabs()"/>
    <label for="tab-1" class="p-tabview p-tabview-nav p-tabview-nav-link">Search and Chat with dotCMS</label>

    <input id="tab-2" type="radio" name="tab-group" onclick="changeTabs()"/>
    <label for="tab-2">Image Playground</label>


    <input id="tab-3" type="radio" name="tab-group" onclick="changeTabs()"/>
    <label for="tab-3">Manage Embeddings/Indexes</label>

    <input id="tab-4" type="radio" name="tab-group" onclick="changeTabs();"/>
    <label for="tab-4">Config Values</label>

</div>

<div id="content">
    <div id="content-1">
        <h2>Semantic Content Search and Chat</h2>

        <div style="display: grid;grid-template-columns: 45% 55%;">
            <div style="border-right:1px solid #eeeeee;margin-right:40px;padding-right: 40px">
                <form action="POST" id="chatForm" onsubmit="return false;">
                    <table class="aiSearchResultsTable">
                        <tr>
                            <th style="width:30%">
                                Content index to search:
                            </th>
                            <td>
                                <select name="indexName" id="indexNameChat" style="min-width:400px;">
                                    <option disabled="true" placeholder="Select an Index">Select an Index</option>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <th>
                                <label>Response Type:</label>
                            </th>
                            <td>
                                <div style="padding-bottom:10px;">
                                    <input type="radio" checked="true" id="searchResponseType" name="responseType"
                                           value="search" onchange="showResultTables()">
                                    <label for="searchResponseType">Semantic Search &nbsp; &nbsp; - dotCMS Only</label>
                                </div>
                                <div style="padding-bottom:10px;">
                                    <input type="radio" id="streamingResponseType" name="responseType" value="stream"
                                           onchange="showResultTables()">
                                    <label for="streamingResponseType">Streaming Chat &nbsp; &nbsp; &nbsp; &nbsp; -
                                        OpenAI + dotCMS
                                        Supporting Content</label>
                                </div>
                                <div>
                                    <input type="radio" id="restJsonResponseType" name="responseType" value="json"
                                           onchange="showResultTables()">
                                    <label for="restJsonResponseType">REST/JSON Chat &nbsp; &nbsp; - OpenAI + dotCMS
                                        Supporting Content</label>
                                </div>
                            </td>
                        </tr>
                    </table>

                    <table class="aiSearchResultsTable">
                        <tr>
                            <th style="width:30%">
                                <b>Prompt:</b>
                            </th>
                            <td><span class="clearPromptX" id="searchQueryX" onclick="clearPrompt('searchQuery')"
                                      style="visibility: hidden">&#10006;</span>
                                <textarea class="prompt" name="prompt" id="searchQuery"
                                          onkeyup="showClearPrompt('searchQuery')"
                                          onchange="showClearPrompt('searchQuery')"
                                          placeholder="Search text or phrase"></textarea>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="text-align: center">
                                <div style="padding:10px;height:75px; text-align: center">
                                    <div class="loader" style="display:none;height:40px;padding:10px;"
                                         id="loaderChat"></div>
                                    <button id="submitChat" class="button dijit dijitReset dijitInline dijitButton"
                                            onclick="doSearchChatJson()">
                                        Submit &nbsp; &nbsp; <i>&rarr;</i>
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </table>

                    <button  class="button dijit dijitReset dijitInline dijitButton"
                             onclick="toggleAdvancedSearchOptionsTable()">Advanced
                        &nbsp; <i id="showAdvancedArrow" class="pi pi-chevron-right aiChevron"></i>
                    </button>
                    <div style="margin:-20px 0px 25px 0px;border-top:1px solid #eeeeee;"></div>


                    <table id="advancedSearchOptionsTable" style="display: none" class="aiSearchResultsTable">
                        <tr>
                            <th style="width:30%">
                                Model:
                            </th>
                            <td>
                                <select name="model" id="modelName" style="min-width:400px;">
                                    <option disabled="true" placeholder="Select a Model">Select a Model</option>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <th style="width:30%">
                                Embedding Model:
                            </th>
                            <td>
                                <select name="embeddingModel" id="embeddingModelName" style="min-width:400px;">
                                    <option disabled="true" placeholder="Select a Model">Select an embedding Model</option>
                                </select>
                            </td>
                        </tr>
                        <th style="width:30%">
                            Temperature:
                        </th>
                        <td>
                            <input name="temperature" type="number" step="0.1" value="1" min="0" max="2"
                                   style="min-width:100px;"><br>
                            (determines the randomness of the response. 0 = deterministic, 2 = most random
                        </td>
                        </tr>
                        <tr>
                            <th>
                                Response length:
                            </th>
                            <td>
                                <input type="number" step="1" value="500" min="10" max="2048"
                                       style="min-width:100px;"
                                       name="responseLengthTokens" id="responseLengthTokens"><br>
                                The general length of response you would like to generate. 75 words ~= 100 tokens
                            </td>
                        </tr>

                        <tr>
                            <th>Vector Operator:</th>
                            <td>

                                <input type="radio" name="operator" id="cosine" checked="true" value="cosine">
                                <label for="cosine">Cosine Similarity</label>
                                &nbsp; &nbsp;
                                <input type="radio" name="operator" id="distance" value="distance">
                                <label for="distance">Distance</label>
                                &nbsp; &nbsp;
                                <input type="radio" name="operator" id="product" value="product">
                                <label for="product">Inner Product</label>
                                <br>
                                Search stored embeddings using this operator<br>(probably best to leave this alone).
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Distance Threshold:
                            </th>
                            <td>
                                <input type="number" step="0.05" value=".25" name="threshold" min="0.05" max="100"
                                       style="min-width:100px;"><br>
                                The lower this number, the more semantically similar the results.
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Site:
                            </th>
                            <td>
                                <input type="text" value="" name="site"><br>
                                Limit search to content on this site - leave blank for all.
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Content Types:
                            </th>
                            <td>
                                <input type="text" value="" name="contentType" id="contentTypeSearch"><br>
                                Limit search to these content types - can be a comma separated list.
                            </td>
                        </tr>
                    </table>

                </form>
            </div>
            <div>


                <div>
                    <textarea id="answerChat" style="overflow: auto;white-space: pre-wrap;"></textarea>
                    <div id="semanticSearchResults"></div>
                </div>
            </div>
        </div>
    </div>


    <div id="content-3">
        <h2>Manage Embeddings / Indexes</h2>

        <div style="display: grid;grid-template-columns: 45% 55%;">
            <div style="border-right:1px solid #eeeeee;margin-right:40px;padding-right: 40px">

                <form id="createUpdateIndex" onsubmit="return false;">

                    <table style="width:100%">
                        <tr>
                            <th style="width:30%">
                                Index Name
                            </th>
                            <td>
                                <input type="text" name="indexName" value="default"><br>
                                Index Name to create or append
                            </td>
                        </tr>
                        <tr>
                            <th style="width:30%">
                                Content to Index by Query:
                            </th>
                            <td><span class="clearPromptX" id="contentQueryX" onclick="clearPrompt('contentQuery')"
                                      style="visibility: hidden">&#10006;</span>
                                <textarea class="prompt" name="query" id="contentQuery"
                                          onkeyup="showClearPrompt('contentQuery')"
                                          onchange="showClearPrompt('contentQuery')"
                                          placeholder="e.g. +contentType:blog"></textarea>
                            </td>
                        </tr>
                        <tr>
                            <th style="width:30%">

                            </th>
                            <td>
                                <input type="radio" name="indexAddOrDelete" id="indexAddCheckbox" checked onclick="changeBuildIndexName()"> <label for="indexAddCheckbox" >Add to Index</label>
                                &nbsp; &nbsp;
                                <input type="radio" name="indexAddOrDelete" id="indexDelCheckbox" onclick="changeBuildIndexName()"> <label for="indexDelCheckbox">Delete from Index</label>
                            </td>
                        </tr>


                    </table>

                    <table style="width:100%">
                        <tr>
                            <td colspan="2" style="text-align: center">
                                <div class="loader" style="display:none;height:40px;padding:10px;"id="loaderIndex"></div>
                                <button id="submitBuildIndexBtn" class="button dijit dijitReset dijitInline dijitButton"
                                        onclick="doBuildIndexWithDebounceBtn()">
                                    Build Index&nbsp; &nbsp <i>&rarr;</i>
                                </button>
            </div>
            </td>
            </tr>
            </table>


            <button  class="button dijit dijitReset dijitInline dijitButton"
                     onclick="toggleWhatToEmbedTable()">Advanced
                &nbsp; <i id="showOptionalEmbeddingsArrow" class="pi pi-chevron-right aiChevron"></i>
            </button>
            <div style="margin:-20px 0px 25px 0px;border-top:1px solid #eeeeee;"></div>

            <table style="display: none" id="whatToEmbedTable">
                <tr>
                    <td colspan="2" style="text-align:justify ">
                        <b>What To Embed (Optional)</b><br>
                        Three options. 1) You can specify what field or fields of your content you want to
                        include in the embeddings or 2) you can also use velocity to render your content for
                        embedding or 3) leave these blank and dotCMS will try to guess what fields to use
                        when generating embedddings. Without prompting, dotCMS will generate embeddings for
                        any WYSIWYG, StoryBlock, Textarea, File or Binary fields.
                    </td>
                </tr>
                <tr>
                    <th style="width:30%">
                        Velocity Template to embed:
                    </th>
                    <td><span class="clearPromptX" id="velocityTemplateX"
                              onclick="clearPrompt('velocityTemplate')"
                              style="visibility:hidden">&#10006;</span>
                        <textarea class="prompt" name="velocityTemplate" id="velocityTemplate"
                                  onkeyup="showClearPrompt('velocityTemplate')"
                                  onchange="showClearPrompt('velocityTemplate')"
                                  placeholder="e.g.&#10;$contentlet.shortDescription&#10;$contentlet.body.toHtml()"></textarea>
                        <br>
                        Use velocity to build exactly how you want to embed your content.
                    </td>
                </tr>
                <tr>
                    <th style="width:30%">
                        Or Field Variable(s)
                    </th>
                    <td>
                        <input type="text" value="" name="fields">
                        <br>
                        If you just specify a comma separated list of fields variables, dotCMS will use
                        their values when generating the embedding.
                    </td>
                </tr>
            </table>

            </form>
            <div id="buildResponse"></div>
        </div>

        <div>
            <h3>Indexes &nbsp;</h3>
            <table id="indexManageTable" style="width:80%">

            </table>


        </div>
    </div>
</div>

<div id="content-2">
    <h2>Image Playground</h2>

    <div style="display: grid;grid-template-columns: 45% 55%;">
        <div style="border-right:1px solid #eeeeee;margin-right:40px;padding-right: 40px">
            <form action="POST" id="imageForm" onsubmit="return false;">
                <table class="aiSearchResultsTable">
                    <tr>
                        <th style="width:30%">
                            <b>Prompt:</b>
                        </th>
                        <td><span class="clearPromptX" id="imagePromptX" onclick="clearPrompt('imagePrompt')"
                                  style="visibility: hidden">&#10006;</span>
                            <textarea class="prompt" name="prompt" id="imagePrompt"
                                      onkeyup="showClearPrompt('imagePrompt')"
                                      onchange="showClearPrompt('imagePrompt')"
                                      placeholder="Image prompt"></textarea>
                        </td>
                    </tr>
                    <tr>
                        <th>
                            Size:
                        </th>
                        <td>
                            <select name="size" style="min-width:400px;">
                                <option value="1024x1024">1024x1024 (Square)</option>
                                <option value="1024x1792">1024x1792 (Vertical)</option>
                                <option value="1792x1024" selected>1792x1024 (Horizontal)</option>


                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" style="text-align: center">
                            <div style="padding:10px;height:75px; text-align: center">
                                <div class="loader" style="display:none;height:40px;padding:10px;" id="loaderImage"></div>
                                <button id="submitImage" class="button dijit dijitReset dijitInline dijitButton"
                                        onclick="doImageJson()">
                                    Submit &nbsp; &nbsp; <i>&rarr;</i>
                                </button>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <div>
                                <h4>Recent Prompts</h4>
                                <ol id="image-prompts">


                                </ol>


                            </div>


                        </td>
                    </tr>
                </table>


            </form>
        </div>
        <div>
            <div id="imageRequest">

            </div>
        </div>
    </div>
</div>

<div id="content-4">
    <h2>AI/Embeddings Config</h2>

    <div style="padding:20px;border:1px solid darkgray;max-width:800px;margin:30px;">
        These values can be changed by adding/editing them in the <a
            href="/dotAdmin/#/apps/dotAI/edit/SYSTEM_HOST"
            target="_top">App screen</a> either as a
        setting or
        as a custom property.
    </div>

    <div id="configTable" style="max-width: 800px">


    </div>
</div>
