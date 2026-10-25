import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ActionPayload, ContainerPayload, VTLFile } from '../../../shared/models';

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
 * What a part of the editor asks for when the user wants to create a contentlet of a content type.
 */
export interface DotUveCreateContentRequest {
    /**
     * The content type to create: its variable, and the name to show when the content type cannot
     * name itself.
     */
    contentType: { variable: string; name: string };
    /**
     * The container position the new contentlet goes into once it is saved. Without it the
     * contentlet is only created, and the page reloads once it is saved.
     */
    into?: ActionPayload;
}

/**
 * What a container's "add" menu asks for: existing content, a widget or a form, to insert into a
 * position of that container.
 */
export interface DotUveAddContentRequest {
    /** What to add. */
    type: 'content' | 'widget' | 'form';
    /** The container position it goes into. */
    into: ActionPayload;
}

/**
 * Requests to open a content editor. Any part of the UVE editor may dispatch them; only
 * `DotUveContentEditorComponent` listens, and it alone decides which editor opens. When a request
 * carries a container position (`into`), the content editor also inserts what the user creates or
 * picks there.
 */
export const uveContentEditorEvents = eventGroup({
    source: 'UVE Content Editor',
    events: {
        uveEditContentRequested: type<DotUveEditContentRequest>(),
        uveEditSourceRequested: type<VTLFile>(),
        uveCreateContentRequested: type<DotUveCreateContentRequest>(),
        uveAddContentRequested: type<DotUveAddContentRequest>()
    }
});
