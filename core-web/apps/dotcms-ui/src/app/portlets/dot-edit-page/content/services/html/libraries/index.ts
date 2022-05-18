// import AUTOSCROLLER_JS from './autoscroller.js.js';
import AUTOSCROLLER_JS from '@dotcms/app/portlets/dot-edit-page/content/services/html/libraries/autoscroller.js';
import DRAGULA_JS from './dragula.min.js';
import { EDIT_PAGE_JS } from '@dotcms/app/portlets/dot-edit-page/content/services/html/libraries/iframe-edit-mode.js';

const EDIT_MODE_DRAG_DROP = `
${DRAGULA_JS}
${AUTOSCROLLER_JS}
${EDIT_PAGE_JS}
`;

export const EDIT_PAGE_JS_DOJO_REQUIRE = `
require(['/html/js/dragula-3.7.2/dragula.min.js'], function(dragula) { 
    ${EDIT_MODE_DRAG_DROP}
});
`;

export default EDIT_MODE_DRAG_DROP;
