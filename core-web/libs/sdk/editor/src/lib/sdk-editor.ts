import {
  CUSTOMER_ACTIONS,
  postMessageToEditor,
} from './actions/customer.actions';

//I have two proposals: standalone functions and a object
//This is the Object, we gain the advantage to have shorts and clearly functions names
//because the object name is the glibal description
//like dotSdkCustomerActions.[AN CUSTOMER ACTION]
export const dotSdkCustomerActions = {
  setURL(pathname: string) {
    postMessageToEditor({
      action: CUSTOMER_ACTIONS.SET_URL,
      payload: {
        url: pathname === '/' ? 'index' : pathname?.replace('/', ''),
      },
    });
  },

  setBounds(positionData: Object) {
    postMessageToEditor({
      action: CUSTOMER_ACTIONS.SET_BOUNDS,
      payload: positionData,
    });
  },

  setContentlet(e: PointerEvent) {
    let target = e.target as HTMLElement;

    if (target.dataset?.['dot'] !== 'contentlet') {
      target = target.closest('[data-dot="contentlet"]') as HTMLElement;
    }

    if (!target) {
      return;
    }

    const { x, y, width, height } = target.getBoundingClientRect();

    const contentletPayload = JSON.parse(target.dataset?.['content'] ?? '{}');

    postMessageToEditor({
      action: CUSTOMER_ACTIONS.SET_CONTENTLET,
      payload: {
        x,
        y,
        width,
        height,
        payload: contentletPayload,
      },
    });
  },

  eventScrollHandler() {
    postMessageToEditor({
      action: CUSTOMER_ACTIONS.IFRAME_SCROLL,
    });
  },

  checkIsInsideEditor() {
    postMessageToEditor({
      action: CUSTOMER_ACTIONS.PING_EDITOR, // This is to let the editor know that the page is ready
    });
  },

  didContentChange() {
    return new MutationObserver((mutationsList) => {
      for (const { addedNodes, removedNodes, type } of mutationsList) {
        if (type === 'childList') {
          const didNodesChanged = [
            ...Array.from(addedNodes),
            ...Array.from(removedNodes),
          ].filter(
            (node) => (node as HTMLDivElement).dataset?.['dot'] === 'contentlet'
          ).length;

          if (didNodesChanged) {
            postMessageToEditor({
              action: CUSTOMER_ACTIONS.CONTENT_CHANGE,
            });
          }
        }
      }
    });
  },
};

//The standalone functions proposal is more simple
//because the customer actions doesnt need any comunication between them
//but the names are more verbose
//I Have this proposal of naming
export const notifyDotEditorOfSetBounds = () => {};

export const notifyDotEditorOfSetURL = () => {};

export const sendCustomerActionSetURL = () => {};

export const dotTriggerSetURL = () => {};

export const dotOnCustomerActionSetURL = () => {};

export const dotOnSetURL = () => {};
