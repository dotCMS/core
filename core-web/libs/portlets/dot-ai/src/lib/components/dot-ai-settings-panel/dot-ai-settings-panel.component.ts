import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';

import { DOT_AI_VECTOR_OPERATOR, DotAiVectorOperator } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSiteComponent } from '@dotcms/ui';

import {
    DOT_AI_MIN_RESPONSE_TOKENS,
    DOT_AI_TEMPERATURE_RANGE
} from '../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../store/dot-ai.store';

/**
 * The retrieval-settings panel shared by Search and Chat.
 *
 * It writes straight into the store rather than owning a form, because the store is what both
 * tabs read and what `retrievalPayload` is assembled from — a local form would be a second
 * copy of the same state and would reset on tab switch (FR-016, FR-017).
 *
 * Markup uses the app's `.form` / `.field` utilities, which supply the label and hint
 * typography, so nothing here hand-rolls those classes.
 */
@Component({
    selector: 'dot-ai-settings-panel',
    imports: [
        FormsModule,
        SelectModule,
        InputNumberModule,
        InputTextModule,
        RadioButtonModule,
        DotSiteComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-settings-panel.component.html',
    host: { class: 'block h-full overflow-y-auto p-4' }
})
export class DotAiSettingsPanelComponent {
    protected readonly store = inject(DotAiStore);

    protected readonly operators: { label: string; value: DotAiVectorOperator }[] = [
        { label: 'dotai.settings.operator.cosine', value: DOT_AI_VECTOR_OPERATOR.COSINE },
        { label: 'dotai.settings.operator.distance', value: DOT_AI_VECTOR_OPERATOR.DISTANCE },
        // `innerProduct`, never the legacy `product` — see FR-024.
        {
            label: 'dotai.settings.operator.inner-product',
            value: DOT_AI_VECTOR_OPERATOR.INNER_PRODUCT
        }
    ];

    protected readonly temperatureRange = DOT_AI_TEMPERATURE_RANGE;
    protected readonly minResponseTokens = DOT_AI_MIN_RESPONSE_TOKENS;

    /** A cleared site means "all sites"; the payload turns null into an empty string. */
    protected onSiteChange(site: { identifier?: string } | string | null): void {
        const identifier = typeof site === 'string' ? site : (site?.identifier ?? null);

        this.store.setSettings({ settingsSite: identifier });
    }
}
