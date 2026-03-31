import { of } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { RuleEngineModule } from '@dotcms/dot-rules';

export interface RulesDialogData {
    identifier: string;
}

/**
 * Dialog component that displays the rule engine for a given page contentlet.
 * Used from the Rules tab in the edit content sidebar.
 */
@Component({
    selector: 'dot-rules-dialog',
    templateUrl: './rules-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [RuleEngineModule],
    providers: [
        {
            provide: ActivatedRoute,
            useFactory: (config: DynamicDialogConfig<RulesDialogData>) => ({
                params: of({ pageId: config.data?.identifier ?? '' }),
                queryParams: of({})
            }),
            deps: [DynamicDialogConfig]
        }
    ]
})
export class DotRulesDialogComponent {
    readonly #config = inject(DynamicDialogConfig<RulesDialogData>);

    readonly identifier = this.#config.data?.identifier ?? '';
}
