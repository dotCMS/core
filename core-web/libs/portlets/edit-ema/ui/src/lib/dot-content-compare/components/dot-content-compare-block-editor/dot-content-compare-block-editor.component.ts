import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { AfterViewInit, Component, Input, ViewChild, inject } from '@angular/core';
import { outputToObservable, toSignal } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { map } from 'rxjs/operators';

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
    imports: [DotCMSEditorComponent, BlockEditorModule, DotSafeHtmlPipe, DotDiffPipe, AsyncPipe]
})
export class DotContentCompareBlockEditorComponent implements AfterViewInit {
    private sanitizer = inject(DomSanitizer);
    private readonly dotPropertiesService = inject(DotPropertiesService);

    @ViewChild('blockEditor') blockEditor: AnyBlockEditor;
    @ViewChild('blockEditorCompare') blockEditorCompare: AnyBlockEditor;

    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;
    @Input() showAsCompare: boolean;
    @Input() field: string;

    /**
     * Resolves the `FEATURE_FLAG_NEW_BLOCK_EDITOR` flag — `undefined` while the HTTP request
     * is in flight, then `true` / `false` once it returns. Per the project-wide rule, a missing
     * flag resolves to `true` (`getFeatureFlag`), so the new editor renders unless the flag is
     * explicitly `false`. The template's truthy check still keeps the legacy editor in-flight.
     */
    readonly isNewBlockEditorEnabled = toSignal(
        this.dotPropertiesService.getFeatureFlag(FeaturedFlags.FEATURE_FLAG_NEW_BLOCK_EDITOR)
    );

    htmlCompareValue$: Observable<SafeHtml>;
    htmlWorkingValue$: Observable<SafeHtml>;

    ngAfterViewInit(): void {
        this.htmlCompareValue$ = outputToObservable(this.blockEditorCompare?.valueChange).pipe(
            map(() => this.getEditorInstance(this.blockEditorCompare)?.getHTML() ?? '')
        );

        this.htmlWorkingValue$ = outputToObservable(this.blockEditor?.valueChange).pipe(
            map(() => this.getEditorInstance(this.blockEditor)?.getHTML() ?? '')
        );
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
