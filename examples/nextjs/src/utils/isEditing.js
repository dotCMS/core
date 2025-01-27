import { getUVEState } from '@dotcms/client';

export function isEditing() {
    return getUVEState()?.mode === 'edit';
}
