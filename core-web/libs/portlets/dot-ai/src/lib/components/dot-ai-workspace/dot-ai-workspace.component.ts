import { Component, input } from '@angular/core';

import { SplitterModule } from 'primeng/splitter';

import { DotAiSettingsPanelComponent } from '../dot-ai-settings-panel/dot-ai-settings-panel.component';

/**
 * The two-pane shell shared by Search and Chat: the tab's own content on the left, the shared
 * retrieval settings on the right.
 *
 * A splitter rather than a fixed aside — the established shape for a settings pane beside a
 * results pane in dot-query-tool, dot-es-search, dot-roles and dot-analytics. `stateKey`
 * persists the user's split across reloads (FR-019), so each tab passes its own.
 *
 * Both tabs had this shell byte-for-byte apart from that key, which meant the panel sizes and
 * the border override had to be kept in step by hand across two files.
 */
@Component({
    selector: 'dot-ai-workspace',
    imports: [SplitterModule, DotAiSettingsPanelComponent],
    templateUrl: './dot-ai-workspace.component.html',
    host: { class: 'block h-full' }
})
export class DotAiWorkspaceComponent {
    /** Distinct per tab, so the two splits are remembered separately. */
    readonly stateKey = input.required<string>();

    /** The dotCMS preset gives p-splitter a border and radius no consumer of it wants. */
    protected readonly splitterPt = { root: { class: 'border-0! rounded-none!' } };
}
