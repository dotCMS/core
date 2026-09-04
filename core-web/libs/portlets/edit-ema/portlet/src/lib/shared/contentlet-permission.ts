import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

/**
 * Message key shared by every surface that explains a refused contentlet edit:
 * the pencil tooltip, the Quick Edit tooltip, the quick-edit panel notice and
 * the inline-editing toast. One key so all four read identically.
 */
export const NO_EDIT_PERMISSION_MESSAGE_KEY = 'uve.contentlet.no.edit.permission';

const CONTENTLET_SELECTOR = '[data-dot-object="contentlet"]';

/**
 * Whether the contentlet owning `element` may be edited by the current user.
 *
 * Reads `data-dot-can-edit` off the nearest contentlet wrapper, which the
 * Velocity container renderer stamps from a WRITE-level permission check.
 * Fail-open: only the literal `"false"` denies, because headless and
 * SDK-rendered pages never emit the attribute.
 */
export function canEditOwningContentlet(element: Element | null | undefined): boolean {
    const wrapper = element?.closest(CONTENTLET_SELECTOR) as HTMLElement | null;

    return wrapper?.dataset?.['dotCanEdit'] !== 'false';
}

/**
 * Same check, for callers that only have the contentlet's inode and a document
 * to search — the Block Editor inline flow arrives as a `postMessage` payload
 * rather than a DOM event, so there is no element to walk up from.
 *
 * Matches on the dataset rather than building a selector from the inode, so an
 * unexpected value cannot break the query.
 */
export function canEditContentletByInode(
    doc: Document | null | undefined,
    inode: string | undefined
): boolean {
    if (!doc || !inode) {
        return true;
    }

    const wrapper = Array.from(doc.querySelectorAll(CONTENTLET_SELECTOR)).find(
        (element) => (element as HTMLElement).dataset?.['dotInode'] === inode
    ) as HTMLElement | undefined;

    return wrapper?.dataset?.['dotCanEdit'] !== 'false';
}

/**
 * Tell the user an action was refused for lack of edit permission.
 *
 * Inline-editable fields have no control to grey out and no hover target for a
 * tooltip, so a silent no-op is indistinguishable from a broken editor. The
 * refusal has to say why.
 */
export function notifyNoEditPermission(
    messageService: MessageService,
    dotMessageService: DotMessageService
): void {
    messageService.add({
        severity: 'warn',
        detail: dotMessageService.get(NO_EDIT_PERMISSION_MESSAGE_KEY),
        life: 3000
    });
}
