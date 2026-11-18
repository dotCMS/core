import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContentletContext, SelectionMode } from '../../../models/relationship.models';

/**
 * Everything the "Add Relationships" dialog is opened with.
 *
 * Two consumers hand this over and they are not symmetric — see
 * `specs/37192-relationship-field-assetpicker/contracts/relationship-picker.contract.md`:
 *
 * - **Edit-time**, the Relationship field itself, which supplies the full parent context so the
 *   dialog can disable content already claimed by another parent.
 * - **Filter-time**, Content Drive's shared field-filter chip, reached through
 *   `DOT_RELATIONSHIP_PICKER`. It is choosing values to match against, not editing a relationship,
 *   so every parent-context field below is absent for it — deliberately, not by oversight.
 */
export interface AddRelationshipsInput {
    /**
     * The relationship's target content type. Pins every search the dialog issues, and is the one
     * thing no filter may widen.
     */
    contentTypeId: string;

    /**
     * The contentlets the field already relates — **the objects, not their ids**.
     *
     * This is the whole reason the dialog can be trusted with server-side paging. The previous
     * dialog took ids and rebuilt the pre-selection by filtering its *first search response*, so an
     * already-related item that response did not contain was never pre-checked and was silently
     * dropped on confirm. The caller already holds these contentlets, so handing them over removes
     * the failure mode rather than guarding against it.
     *
     * It also survives an index that has not caught up: per ADR-0018 the free-text filter resolves
     * through the search index, which lags asynchronously behind writes, so "search for it again"
     * is not a reliable way to find content that demonstrably exists.
     */
    selected: DotCMSContentlet[];

    /**
     * Pre-selection for callers that hold only **inodes**, not contentlets — best-effort.
     *
     * Exists for exactly one caller: Content Drive's field-filter chip, which reaches this dialog
     * through `DOT_RELATIONSHIP_PICKER`, whose signature is `open(field, selectedInodes: string[])`
     * and is frozen (contract C1). That chip does hold the contentlets, but the token has never
     * carried them.
     *
     * **Degraded on purpose, and safe to be.** These are resolved as results arrive, so an inode on
     * no page the editor visits is never marked selected. That is a real limitation for
     * {@link selected}, which is why edit-time does not use this path — losing a pre-check there
     * loses a *relationship*. Here it loses a highlight on a *filter value*, and the chip already
     * knows what it picked.
     */
    selectedInodes?: string[];

    /**
     * i18n key for the confirm action, when the default does not fit.
     *
     * Exists so there is **one** footer. Content Drive opens this dialog as a *filter*, where "Add
     * Relationships" is the wrong verb — it wants "Apply". It used to supply a whole replacement
     * footer through PrimeNG's `templates.footer`, which worked only because the old dialog's store
     * was `providedIn: 'root'`: a template rendered outside the dialog component's injector cannot
     * reach a component-provided store. This store is per-dialog on purpose (a root one leaks
     * across open dialogs), so the label travels instead of the component.
     */
    confirmLabel?: string;

    /** Whether the editor may hold one item or several. From the field's cardinality. */
    selectionMode: SelectionMode;

    /**
     * Parent context, used only to work out which content is already claimed by another parent.
     *
     * All optional together: filter-time supplies none of it, and the dialog must then skip the
     * lookup entirely rather than compute it from a partial set.
     */
    cardinality?: number;
    parentContentTypeId?: string;
    fieldVariable?: string;
    isParentField?: boolean;
    currentContentIdentifier?: string | null;

    /**
     * Seeds the locale and site chips from the contentlet being edited.
     *
     * A starting point the editor can change, never a restriction.
     */
    contentletContext?: ContentletContext;
}

/**
 * What the dialog closes with.
 *
 * The two outcomes are **not** interchangeable and collapsing them breaks the feature in one
 * direction or the other:
 *
 * - `DotCMSContentlet[]` — the editor confirmed. An **empty array is a real result**: it means they
 *   unchecked everything, and the relationship is emptied. The confirm action is never disabled, so
 *   this is reachable on purpose.
 * - `undefined` — the editor cancelled. The field's related content is left exactly as it was.
 *
 * Read `[]` as a cancel and confirming-empty does nothing; read a cancel as `[]` and cancelling
 * wipes the field.
 *
 * Note the asymmetry with `DotRelationshipPicker`, whose contract promises an empty list either way.
 * Content Drive's provider absorbs that translation; this type stays honest about the difference.
 */
export type AddRelationshipsResult = DotCMSContentlet[] | undefined;
