export const INLINE_EDIT_BLOCK_EDITOR_SCRIPTS = `
     function emitEditBlockEditorEvent(event) {
           const customEvent = new CustomEvent('ng-event', { detail: {  name: 'edit-block-editor', data: event.target } });
           window.top.document.dispatchEvent(customEvent);
     };`;
