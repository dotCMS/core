// Autoexecuted script that listens for clicks on block editor content and sends a message to the parent window to open the inline editor.
// TODO: This should be part of the sdk-editor-vtl.js but we need to figure out how to know when client has enterpise license
(() => {
  const listenBlockEditorClick = () => {
    const editBlockEditorNodes = document.querySelectorAll(
      "[data-block-editor-content]"
    );
    if (!editBlockEditorNodes.length) {
      return;
    }
    editBlockEditorNodes.forEach((node) => {
      node.classList.add("dotcms__inline-edit-field");
      node.addEventListener("click", () => {
        const payload = Object.assign({}, node.dataset);
        window.parent.postMessage(
          {
            payload,
            action: "init-editor-inline-editing"
          },
          "*"
        );
      });
    });
  };
  
  if (document.readyState === "complete") {
    // The page is fully loaded
    listenBlockEditorClick();
  } else {
    window.addEventListener("load", () =>listenBlockEditorClick() );
  }
})();