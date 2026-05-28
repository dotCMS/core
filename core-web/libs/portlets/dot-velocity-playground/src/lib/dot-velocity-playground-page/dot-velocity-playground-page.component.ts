import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SplitterModule } from 'primeng/splitter';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DOT_MONACO_BASE_OPTIONS, DOT_MONACO_RAW_OPTIONS, DotMessagePipe } from '@dotcms/ui';

import { DotVelocityPlaygroundStore } from './store/dot-velocity-playground.store';

import {
    ensureVelocityLanguageRegistered,
    VELOCITY_LANGUAGE_ID
} from '../monaco/register-velocity';
import { DotVelocityPlaygroundService } from '../services/dot-velocity-playground.service';

@Component({
    selector: 'dot-velocity-playground-page',
    imports: [
        FormsModule,
        MonacoEditorModule,
        SplitterModule,
        ButtonModule,
        ToggleSwitchModule,
        SelectModule,
        TagModule,
        TooltipModule,
        MessageModule,
        DotMessagePipe
    ],
    providers: [DotVelocityPlaygroundStore, DotVelocityPlaygroundService],
    templateUrl: './dot-velocity-playground-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 bg-white' }
})
export class DotVelocityPlaygroundPageComponent {
    readonly store = inject(DotVelocityPlaygroundStore);
    readonly #messageService = inject(DotMessageService);

    readonly ComponentStatus = ComponentStatus;

    readonly splitterPt = { root: { class: 'border-0! rounded-none! flex-1 min-h-0' } };

    readonly $editorOptions = computed(() => ({
        ...DOT_MONACO_BASE_OPTIONS,
        language: VELOCITY_LANGUAGE_ID,
        wordWrap: this.store.wrapCode() ? 'on' : 'off'
    }));

    readonly $outputOptions = computed(() => ({
        ...DOT_MONACO_RAW_OPTIONS,
        language: this.store.outputContentType(),
        wordWrap: this.store.wrapCode() ? 'on' : 'off',
        readOnly: true
    }));

    readonly $historyOptions = computed(() =>
        this.store.history().map((entry) => ({
            label: this.#formatHistoryLabel(entry),
            value: entry
        }))
    );

    onEditorInit(): void {
        ensureVelocityLanguageRegistered();
    }

    onRun(): void {
        if (!this.store.canRun()) return;
        this.store.runScript();
    }

    onHistoryChange(entry: string | null): void {
        if (entry == null) return;
        this.store.selectHistoryEntry(entry);
    }

    onSplitterResize(event: { sizes: number[] }): void {
        const [left, right] = event.sizes;
        if (typeof left === 'number' && typeof right === 'number') {
            this.store.setSplitterRatio([left, right]);
        }
    }

    #formatHistoryLabel(entry: string): string {
        const compact = entry.replace(/\s+/g, ' ').trim();
        return compact.length > 60
            ? `${compact.slice(0, 60)}…`
            : compact || this.#messageService.get('velocityPlayground.history.empty');
    }
}
