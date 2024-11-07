// Autoexecuted script that listens for clicks on block editor content and sends a message to the parent window to open the inline editor.
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
        const dataset = Object.assign({}, node.dataset);
        window.parent.postMessage(
          {
            action: "editor-inline-editing",
            payload: { dataset },
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