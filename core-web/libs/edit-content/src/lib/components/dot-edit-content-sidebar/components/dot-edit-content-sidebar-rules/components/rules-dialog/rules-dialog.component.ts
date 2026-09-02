import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

export interface RulesDialogData {
    identifier: string;
}

function buildRulesIframeUrl(identifier: string): string {
    return `/dotAdmin/#/fromCore/rules?realmId=${identifier}`;
}

/**
 * Dialog component that displays the rule engine for a given page contentlet.
 * Used from the Rules tab in the edit content sidebar.
 */
@Component({
    selector: 'dot-rules-dialog',
    templateUrl: './rules-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotMessagePipe]
})
export class DotRulesDialogComponent {
    readonly #config = inject(DynamicDialogConfig<RulesDialogData>);
    readonly #sanitizer = inject(DomSanitizer);

    readonly iframeSrc: SafeResourceUrl | null = (() => {
        const id = this.#config.data?.identifier;
        if (!id) return null;
        return this.#sanitizer.bypassSecurityTrustResourceUrl(buildRulesIframeUrl(id));
    })();
}
