import { UVE_MODE } from '@dotcms/types';

export interface MODE_SELECTOR_ITEM {
    label: string;
    description: string;
    id: UVE_MODE;
}

export const MENU_ITEMS_MAP: Record<UVE_MODE, MODE_SELECTOR_ITEM> = {
    [UVE_MODE.EDIT]: {
        label: 'uve.editor.mode.draft',
        description: 'uve.editor.mode.draft.description',
        id: UVE_MODE.EDIT
    },
    [UVE_MODE.PREVIEW]: {
        label: 'uve.editor.mode.preview',
        description: 'uve.editor.mode.preview.description',
        id: UVE_MODE.PREVIEW
    },
    [UVE_MODE.LIVE]: {
        label: 'uve.editor.mode.published',
        description: 'uve.editor.mode.published.description',
        id: UVE_MODE.LIVE
    },
    [UVE_MODE.UNKNOWN]: {
        label: 'uve.editor.mode.unknown',
        description: 'uve.editor.mode.unknown.description',
        id: UVE_MODE.UNKNOWN
    }
};
