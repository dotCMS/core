
// dotcmsFields: Custom Elements Define Library, ES Module/es5 Target

import { defineCustomElement } from './dotcmsfields.core.js';
import { COMPONENTS } from './dotcmsfields.components.js';

export function defineCustomElements(win, opts) {
  return defineCustomElement(win, COMPONENTS, opts);
}
