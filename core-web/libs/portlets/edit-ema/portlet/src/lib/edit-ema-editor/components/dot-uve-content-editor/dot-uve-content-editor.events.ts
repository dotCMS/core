import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContainerPayload, VTLFile } from '../../../shared/models';

/**
 * What a part of the editor asks for when the user wants to edit a contentlet.
 */
export interface DotUveEditContentRequest {
    /** The contentlet to open. */
    contentlet: DotCMSContentlet;
    /**
     * The container the contentlet was picked from. When it is set and the contentlet is on more
     * than one page, the user first chooses between editing it on every page and editing a copy
     * on this page only.
     */
    forkFrom?: ContainerPayload;
}

/**
 * Requests to open a content editor. Any part of the UVE editor may dispatch them; only
 * `DotUveContentEditorComponent` listens, and it alone decides which editor opens.
 */
export const uveContentEditorEvents = eventGroup({
    source: 'UVE Content Editor',
    events: {
        uveEditContentRequested: type<DotUveEditContentRequest>(),
        uveEditSourceRequested: type<VTLFile>()
    }
});
