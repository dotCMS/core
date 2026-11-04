import { Injector } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentRequest } from './dot-edit-content-request.model';

/**
 * What a field (Relationship, a block editor) hands its host, or a panel hands the opener, when it
 * wants other content opened (#38303). The host decides whether that means a new panel, a stacked
 * one, or an in-place reload.
 */
export interface DotOpenContentEvent {
    request: DotEditContentRequest;

    /** Called on every `saved` of the panel opened for this request. */
    onSaved?: (contentlet: DotCMSContentlet) => void;

    /** Who asked, when it matters to the host: a Relationship field or a block editor. */
    source?: 'relationship' | 'block-editor';

    /**
     * The asking panel's own injector, set by a panel when it hands the event to the opener. The
     * stacked panel opens under it, so it belongs to that panel and closes with it. A field leaves it
     * unset: its host opens with its own.
     */
    injector?: Injector;
}
