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

    /**
     * Who asked. An edit from a Relationship field replaces the content in place inside a panel;
     * anything else stacks.
     */
    source?: 'relationship' | 'block-editor';

    /**
     * The content the field is showing, for the in-place breadcrumb. Only the field knows it: for an
     * unsaved translation it is the version the translation came from, and the host's trail is
     * empty until the first hop. Absent → the host starts a fresh trail.
     */
    from?: { inode: string; title: string };
}
