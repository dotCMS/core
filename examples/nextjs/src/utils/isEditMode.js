import { getUVEState } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/uve/types';

/**
 * Check if the user is in edit mode
 * @returns {boolean}
 */
export function isEditMode() {
    return getUVEState()?.mode === UVE_MODE.EDIT;
}
