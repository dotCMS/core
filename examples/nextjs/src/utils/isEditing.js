import { getUVEState } from '@dotcms/uve';

export function isEditing() {
    return getUVEState()?.mode === 'edit';
}
