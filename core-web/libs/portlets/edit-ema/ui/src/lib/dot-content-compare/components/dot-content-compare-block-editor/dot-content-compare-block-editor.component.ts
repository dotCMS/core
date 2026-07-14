import { Component, WritableSignal, effect, inject, input, signal, viewChild } from '@angular/core';
import { outputToObservable, toSignal } from '@angular/core/rxjs-interop';

import { Editor } from '@tiptap/core';

import { BlockEditorModule, DotBlockEditorComponent } from '@dotcms/block-editor';
import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { DotCMSEditorComponent } from '@dotcms/new-block-editor';
import { DotSafeHtmlPipe, DotDiffPipe } from '@dotcms/ui';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';

type AnyBlockEditor = DotCMSEditorComponent | DotBlockEditorComponent;

@Component({
    selector: 'dot-content-compare-block-editor',
    templateUrl: './dot-content-compare-block-editor.component.html',
    imports: [DotCMSEditorComponent, BlockEditorModule, DotSafeHtmlPipe, DotDiffPipe]
})
export class DotContentCompareBlockEditorComponent {
    private readonly dotPropertiesService = inject(DotPropertiesService);

    readonly blockEditor = viewChild<AnyBlockEditor>('blockEditor');
    readonly blockEditorCompare = viewChild<AnyBlockEditor>('blockEditorCompare');

    readonly data = input.required<DotContentCompareTableData>();
    readonly showDiff = input<boolean>(false);
    readonly showAsCompare = input<boolean>(false);
    readonly field = input.required<string>();

    /**
     * Resolves the `FEATURE_FLAG_NEW_BLOCK_EDITOR` flag — `undefined` while the HTTP request
     * is in flight, then `true` / `false` once it returns. Per the project-wide rule, a missing
     * flag resolves to `true` (`getFeatureFlag`), so the new editor renders unless the flag is
     * explicitly `false`. The template's truthy check still keeps the legacy editor in-flight.
     */
    readonly isNewBlockEditorEnabled = toSignal(
        this.dotPropertiesService.getFeatureFlag(FeaturedFlags.FEATURE_FLAG_NEW_BLOCK_EDITOR)
    );

    /** HTML of the working version, fed to the visible diff. Updated as the hidden editor loads. */
    readonly htmlWorkingValue = signal<string>('');

    /** HTML of the compared version, fed to the visible diff. Updated as the hidden editor loads. */
    readonly htmlCompareValue = signal<string>('');

    constructor() {
        // Re-bind whenever a hidden editor instance appears or swaps. `viewChild` re-fires when the
        // feature-flag `@if`/`@else` switches between the new and legacy editor (the flag resolves
        // after the initial render), and `getEditorInstance` reads the new editor's `editor` signal
        // so the effect also re-runs once its TipTap instance is built on the slow (customBlocks) path.
        effect((onCleanup) => {
            const cleanups = [
                this.bindEditorHtml(this.blockEditor(), this.htmlWorkingValue),
                this.bindEditorHtml(this.blockEditorCompare(), this.htmlCompareValue)
            ];
            onCleanup(() => cleanups.forEach((dispose) => dispose()));
        });
    }

    /**
     * Keeps `target` in sync with a hidden editor's rendered HTML.
     *
     * The compare view converts stored StoryBlock JSON to HTML purely to feed the `dotDiff` pipe,
     * so it must react to content that is set programmatically — not to user edits. It uses two
     * complementary triggers because the two editors surface "content is ready" differently:
     *
     * - The **new** editor ({@link DotCMSEditorComponent}) applies `[value]` with
     *   `emitUpdate: false`, so it never emits `valueChange`/`update` — but it always dispatches a
     *   TipTap `transaction`. Listening to `transaction` is what populates the diff on load
     *   (previously the field rendered empty / `null`; see issue #36550). Its `editor` is a signal,
     *   so the outer `effect` also re-runs once the instance is built on the slow-path.
     * - The **legacy** editor builds its TipTap instance asynchronously and applies content through
     *   `ngModel`, which fires `valueChange` on the initial load. Its `editor` is a plain property
     *   (not tracked by the effect), so `valueChange` is the trigger that first populates the diff.
     *
     * We also read `getHTML()` up-front in case the content was already applied before this bind.
     *
     * @returns a disposer that detaches every listener.
     */
    private bindEditorHtml(
        component: AnyBlockEditor | undefined,
        target: WritableSignal<string>
    ): () => void {
        if (!component) {
            target.set('');

            return () => undefined;
        }

        const recompute = () => target.set(this.getEditorInstance(component)?.getHTML() ?? '');
        recompute();

        const disposers: Array<() => void> = [];

        // Legacy load trigger (and any user edit on either editor).
        const sub = outputToObservable(component.valueChange).subscribe(recompute);
        disposers.push(() => sub.unsubscribe());

        // New editor's programmatic-content trigger (fires even with emitUpdate:false).
        const editor = this.getEditorInstance(component);
        if (editor) {
            editor.on('transaction', recompute);
            editor.on('update', recompute);
            disposers.push(() => {
                editor.off('transaction', recompute);
                editor.off('update', recompute);
            });
        }

        return () => disposers.forEach((dispose) => dispose());
    }

    /**
     * The new editor exposes `editor` as a signal getter (`editor()`); the legacy editor exposes
     * it as a plain property (`editor`). Resolve uniformly so the same callsite works for either.
     *
     * @deprecated Temporary shim while `FEATURE_FLAG_NEW_BLOCK_EDITOR` rollback is supported.
     * Remove this method (and the `AnyBlockEditor` union) once the legacy editor is dropped.
     */
    private getEditorInstance(component: AnyBlockEditor | undefined): Editor | undefined {
        if (!component) return undefined;
        const ed = (component as { editor: Editor | (() => Editor) }).editor;

        return typeof ed === 'function' ? ed() : ed;
    }
}
