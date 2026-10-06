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

import { DOT_IMAGE_NODE_NAME } from '../../extensions/nodes/image.extension';
import { DOT_VIDEO_NODE_NAME } from '../../extensions/nodes/video.extension';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';
import { isValidHttpUrl } from '../../utils/url.utils';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

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
    templateUrl: './asset-by-url-popover.component.html'
})
export class AssetByUrlPopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);
    private readonly store = inject(EditorStore);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly typeOptions = computed<TypeOption[]>(() => {
        const msg = (key: string) => this.dotMessageService.get(key);
        return [
            {
                label: msg('dot.block.editor.dialog.asset-by-url.type.image'),
                value: 'image',
                disabled: !this.store.isAllowed('image')
            },
            {
                label: msg('dot.block.editor.dialog.asset-by-url.type.video'),
                value: 'video',
                disabled: !this.store.isAllowed('video')
            },
            {
                label: msg('dot.block.editor.dialog.asset-by-url.type.youtube'),
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
