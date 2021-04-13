export const INLINE_TINYMCE_SCRIPTS = `
    function handleInlineEditEvents(editor) {
        editor.on("focus blur", (e) => {
            const { target: ed, type: eventType } = e;

            const content = ed.getContent();
            const dataset = ed.targetElm.dataset;
            const element = ed.targetElm;

            const data = {
                dataset,
                innerHTML: content,
                element,
                eventType,
                isNotDirty: ed.isNotDirty,
            }

            // For full editor we are adding pointer-events: none to all it children, 
            // this is the way we can capture the click to init in the editor itself, after the editor 
            // is initialized and clicked we set the pointer-events: auto so users can use the editor as intended.
            if (eventType === "focus" && dataset.mode) {
                ed.bodyElement.classList.add("active");
            }

            if (eventType === "blur" && ed.bodyElement.classList.contains("active")) {
                ed.bodyElement.classList.remove("active");
            }

            if (eventType === "blur") {
                e.stopImmediatePropagation();
                ed.destroy(false);
            }

            window.contentletEvents.next({
                name: "inlineEdit",
                data,
            });
        });
    }

    const defaultConfig = {
        menubar: false,
        inline: true,
        valid_styles: {
            "*": "font-size,font-family,color,text-decoration,text-align",
        },
        powerpaste_word_import: "clean",
        powerpaste_html_import: "clean",
        setup: (editor) => handleInlineEditEvents(editor)
    };

    const tinyMCEConfig = {
    minimal: {
        plugins: ["link", "autolink"],
        toolbar: "bold italic underline | link",
        valid_elements: "strong,em,span[style],a[href]",
        content_css: ["//fonts.googleapis.com/css?family=Lato:300,300i,400,400i"],
        ...defaultConfig,
    },
    full: {
        plugins: ["link", "lists", "autolink", "hr", "charmap"],
        style_formats: [
        { title: "Paragraph", format: "p" },
        { title: "Header 1", format: "h1" },
        { title: "Header 2", format: "h2" },
        { title: "Header 3", format: "h3" },
        { title: "Header 4", format: "h4" },
        { title: "Header 5", format: "h5" },
        { title: "Header 6", format: "h6" },
        { title: "Pre", format: "pre" },
        { title: "Code", format: "code" },
        ],
        toolbar: [
        "styleselect | undo redo | bold italic underline | forecolor backcolor | alignleft aligncenter alignright alignfull | numlist bullist outdent indent | hr charmap removeformat | link",
        ],
        ...defaultConfig,
    },
    };

    document.addEventListener("click", function (event) {
        
    const { target: { dataset } } = event;

    const dataSelector =
        '[data-inode="' +
        dataset.inode +
        '"][data-field-name="' +
        dataset.fieldName +
        '"]';

    // if the mode is truthy we initialize tinymce
        if (dataset.mode) {

            event.stopPropagation();
            event.preventDefault();

            tinymce
            .init({
                ...tinyMCEConfig[dataset.mode],
                selector: dataSelector,
            })
            .then(([ed]) => {
                ed?.editorCommands.execCommand("mceFocus");
            });
        }
    });
`;
