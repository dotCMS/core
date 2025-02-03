import { getUVEState } from '@dotcms/uve';

/**
 * Check if the user is in edit mode
 * @returns {boolean}
 */
export function isEditing() {
    return getUVEState()?.mode === 'edit';
}
