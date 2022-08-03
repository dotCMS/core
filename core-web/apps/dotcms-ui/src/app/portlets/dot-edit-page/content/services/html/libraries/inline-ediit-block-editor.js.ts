export const INLINE_EDIT_BLOCK_EDITOR_SCRIPTS = `
    function emmitEditBlockEditorEvent(event) {
        const customEvent = document.createEvent('CustomEvent');
        customEvent.initCustomEvent('ng-event', false, false,  {
            name: 'edit-block-editor',
            data: event.target
        });
        window.parent.document.dispatchEvent(customEvent)
    };
     `;
