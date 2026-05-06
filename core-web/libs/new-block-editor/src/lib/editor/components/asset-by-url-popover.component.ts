import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    untracked
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    Validators
} from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { EditorPopoverComponent } from './editor-popover.component';

import { DOT_IMAGE_NODE_NAME } from '../extensions/nodes/image.extension';
import { DOT_VIDEO_NODE_NAME } from '../extensions/nodes/video.extension';
import { EditorPopoverService } from '../services/editor-popover.service';
import { EditorStore } from '../store/editor.store';
import { isValidHttpUrl } from '../utils/url.utils';

type AssetType = 'image' | 'video' | 'youtube';

interface TypeOption {
    label: string;
    value: AssetType;
    disabled: boolean;
}

function urlValidator(control: AbstractControl): ValidationErrors | null {
    const value = (control.value ?? '').toString().trim();
    if (!value) return null;
    return isValidHttpUrl(value) ? null : { invalidUrl: true };
}

@Component({
    selector: 'dot-asset-by-url-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ReactiveFormsModule,
        SelectButtonModule,
        InputTextModule,
        EditorPopoverComponent,
        DotMessagePipe
    ],
    template: `
        <dot-editor-popover popoverId="asset-by-url">
            <div
                [attr.aria-label]="'dot.block.editor.dialog.asset-by-url.aria-label' | dm"
                class="w-80 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                <form
                    [formGroup]="form"
                    class="flex flex-col gap-3 p-3"
                    (keydown.enter)="$event.preventDefault(); onApply()">
                    <p class="text-xs font-semibold text-gray-500 uppercase tracking-wide m-0">
                        {{ 'dot.block.editor.dialog.asset-by-url.title' | dm }}
                    </p>

                    <p-selectButton
                        formControlName="assetType"
                        [options]="typeOptions()"
                        [allowEmpty]="false"
                        optionLabel="label"
                        optionValue="value"
                        optionDisabled="disabled"
                        styleClass="text-xs" />

                    <div class="flex flex-col gap-1">
                        <label for="asset-by-url-input" class="text-xs font-medium text-gray-700">
                            {{ 'dot.block.editor.dialog.asset-by-url.field.url.label' | dm }}
                        </label>
                        <input
                            pInputText
                            id="asset-by-url-input"
                            type="url"
                            formControlName="url"
                            inputmode="url"
                            autocomplete="off"
                            [placeholder]="urlPlaceholder()"
                            class="w-full text-sm" />
                        @if (form.controls.url.touched && form.controls.url.errors?.['invalidUrl']) {
                            <span class="text-xs text-red-600">
                                {{ 'dot.block.editor.dialog.asset-by-url.field.url.invalid' | dm }}
                            </span>
                        }
                    </div>

                    <div class="flex justify-end gap-2 pt-1">
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); manager.close()"
                            class="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300">
                            {{ 'Cancel' | dm }}
                        </button>
                        <button
                            type="button"
                            (mousedown)="$event.preventDefault(); onApply()"
                            [disabled]="form.invalid"
                            data-testid="asset-by-url-insert"
                            class="rounded bg-indigo-500 px-3 py-1 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                            {{ 'Insert' | dm }}
                        </button>
                    </div>
                </form>
            </div>
        </dot-editor-popover>
    `
})
export class AssetByUrlPopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);
    private readonly store = inject(EditorStore);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly typeOptions = computed<TypeOption[]>(() => {
        const t = (key: string) => this.dotMessageService.get(key);
        return [
            {
                label: t('dot.block.editor.dialog.asset-by-url.type.image'),
                value: 'image',
                disabled: !this.store.isAllowed('image')
            },
            {
                label: t('dot.block.editor.dialog.asset-by-url.type.video'),
                value: 'video',
                disabled: !this.store.isAllowed('video')
            },
            {
                label: t('dot.block.editor.dialog.asset-by-url.type.youtube'),
                value: 'youtube',
                disabled: !this.store.isAllowed('youtube')
            }
        ];
    });

    readonly form = new FormGroup({
        assetType: new FormControl<AssetType>(this.firstAllowedType(), { nonNullable: true }),
        url: new FormControl<string>('', {
            nonNullable: true,
            validators: [Validators.required, urlValidator]
        })
    });

    private readonly assetTypeSignal = toSignal(this.form.controls.assetType.valueChanges, {
        initialValue: this.form.controls.assetType.value
    });

    protected readonly urlPlaceholder = computed(() =>
        this.dotMessageService.get(
            `dot.block.editor.dialog.asset-by-url.field.url.placeholder.${this.assetTypeSignal()}`
        )
    );

    constructor() {
        // Reset the form to its initial state whenever the popover closes so the next
        // open starts fresh — same lifecycle pattern as TablePopoverComponent.
        effect(() => {
            if (!this.manager.isOpen('asset-by-url')) {
                untracked(() => this.form.reset({ assetType: this.firstAllowedType(), url: '' }));
            }
        });
    }

    protected onApply(): void {
        if (this.form.invalid) return;
        const { assetType, url } = this.form.getRawValue();
        const trimmed = url.trim();
        const editor = this.editor();
        const chain = editor.chain().focus();

        switch (assetType) {
            case 'image':
                chain
                    .insertContent({
                        type: DOT_IMAGE_NODE_NAME,
                        attrs: { src: trimmed, title: null, alt: null }
                    })
                    .run();
                break;
            case 'video':
                chain
                    .insertContent({
                        type: DOT_VIDEO_NODE_NAME,
                        attrs: { src: trimmed, title: null }
                    })
                    .run();
                break;
            case 'youtube':
                chain.setYoutubeVideo({ src: trimmed }).run();
                break;
        }

        this.manager.close();
    }

    /**
     * Picks the first type the editor field actually allows so that fields locked to a
     * subset of media (e.g. allowedBlocks="image") don't open the popover with a disabled
     * default selected. Falls back to "image" when somehow none are allowed — the toolbar
     * button itself is hidden in that case, so the fallback is mostly defensive.
     */
    private firstAllowedType(): AssetType {
        const first = this.typeOptions().find((opt) => !opt.disabled);
        return first?.value ?? 'image';
    }
}
