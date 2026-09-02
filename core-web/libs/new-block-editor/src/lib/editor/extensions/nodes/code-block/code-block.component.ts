import { AngularNodeViewComponent, TiptapNodeViewContentDirective } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Select, SelectChangeEvent } from 'primeng/select';

import { FULLSCREEN_AWARE_OVERLAY_OPTIONS } from '../../../config.utils';

interface LanguageOption {
    label: string;
    value: string;
}

@Component({
    selector: 'dot-code-block-node-view',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [TiptapNodeViewContentDirective, Select, FormsModule],
    template: `
        <div class="code-block">
            <p-select
                size="small"
                filter="true"
                class="code-block__lang"
                appendTo="body"
                [overlayOptions]="overlayOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="Select Code Language"
                [options]="languageOptions()"
                [ngModel]="currentLanguage()"
                (onChange)="onLanguageChange($event)" />
            <pre><code tiptapNodeViewContent></code></pre>
        </div>
    `
})
export class DotCodeBlockNodeViewComponent extends AngularNodeViewComponent {
    /**
     * The language `<p-select>` panel appends to `document.body`; lift its base z-index above
     * the fullscreen editor shell's `z-[9998]` backdrop so the dropdown stays clickable in
     * fullscreen. See {@link FULLSCREEN_AWARE_OVERLAY_OPTIONS}.
     */
    protected readonly overlayOptions = FULLSCREEN_AWARE_OVERLAY_OPTIONS;

    /** Empty string maps to the "auto" option; lowlight will auto-detect. */
    protected readonly currentLanguage = computed(
        () => (this.node().attrs['language'] as string | null) ?? ''
    );

    /**
     * Build the dropdown options once per language list change. The "auto" entry
     * uses an empty-string value so we can clear the `language` attribute back to
     * null when selected — lowlight then auto-detects.
     */
    protected readonly languageOptions = computed<LanguageOption[]>(() => {
        const ext = this.extension() as { options: { lowlight: { listLanguages(): string[] } } };
        const langs = ext.options.lowlight.listLanguages();
        return [{ label: 'auto', value: '' }, ...langs.map((l) => ({ label: l, value: l }))];
    });

    protected onLanguageChange(event: SelectChangeEvent): void {
        const value = event.value as string;
        this.updateAttributes()({ language: value || null });
    }
}
