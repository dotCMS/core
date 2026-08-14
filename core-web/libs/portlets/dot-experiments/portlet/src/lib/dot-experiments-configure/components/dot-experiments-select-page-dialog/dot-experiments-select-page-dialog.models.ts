import { DotPageBrowserState } from '@dotcms/data-access';

import { SelectPageDialogRow, TagSeverity } from '../../../shared/models';

/**
 * Data handed to the Select A Page dialog through `DynamicDialogConfig`.
 *
 * Both fields are optional: the dialog falls back to the current site held by `GlobalStore`,
 * which is what every caller inside the Experiments portlet wants. They exist so a caller that
 * already resolved another site does not force a second lookup.
 */
export interface DotExperimentsSelectPageDialogData {
    /** Identifier of the site to browse. Defaults to the current site. */
    hostId?: string;
    /** Hostname of `hostId` — page search is scoped by hostname, not by identifier. */
    hostname?: string;
}

/**
 * A row of the dialog's table: the payload the dialog closes with, plus everything the template
 * would otherwise have to derive.
 *
 * It extends {@link SelectPageDialogRow} rather than wrapping it so the selected row can be handed
 * back as-is. `state` is the three-value narrowing the shared contract declares; `pageState` keeps
 * the richer state the browser service reports, which is what the State column renders.
 */
export interface SelectPageDialogViewRow extends SelectPageDialogRow {
    /** Publication state as reported by `DotPagesBrowserService`, before narrowing into `state`. */
    pageState: DotPageBrowserState;
    /** i18n key of the State column's tag. */
    stateLabelKey: string;
    /** PrimeNG severity of the State column's tag. */
    stateSeverity: TagSeverity;
    /**
     * What the Template column shows. No importable service resolves a template identifier to its
     * name from a portlet library, so this is a shortened identifier and `template` holds the full
     * one for the cell's title attribute.
     */
    templateLabel: string;
}
