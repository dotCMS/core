import { PluginKey } from 'prosemirror-state';
import { Subject } from 'rxjs';

import {
    ComponentRef,
    Directive,
    OnDestroy,
    OnInit,
    ViewContainerRef,
    inject,
    input
} from '@angular/core';

import { Editor } from '@tiptap/core';

import { DotUploadFileService } from '@dotcms/data-access';

import { FloatingButtonComponent } from './floating-button.component';
import { DotFloatingButtonPlugin } from './plugin/floating-button.plugin';

/**
 * Stable plugin key for the floating button ProseMirror plugin.
 * Persisted in stored documents -- do not rename.
 */
export const FLOATING_BUTTON_PLUGIN_KEY = new PluginKey('floating-button');

/**
 * Directive that owns the lifecycle of the floating "Import to dotCMS" button.
 *
 * Place this on a host element inside the editor template. On init it creates
 * a {@link FloatingButtonComponent}, registers the {@link DotFloatingButtonPlugin}
 * into the editor, and tears both down on destroy.
 *
 * Replaces the legacy {@link DotFloatingButton} factory function, moving plugin
 * registration from the host component into Angular's directive lifecycle.
 *
 * @example
 * ```html
 * <div dotFloatingButton [editor]="editor"></div>
 * ```
 */
@Directive({
    selector: '[dotFloatingButton]',
    standalone: true
})
export class DotFloatingButtonDirective implements OnInit, OnDestroy {
    private readonly vcr = inject(ViewContainerRef);
    private readonly uploadService = inject(DotUploadFileService);

    /** The Tiptap editor instance. Required. */
    readonly editor = input.required<Editor>();

    /** Plugin key identifier. Defaults to `'floating-button'`. */
    readonly pluginKey = input<string>('floating-button');

    private componentRef: ComponentRef<FloatingButtonComponent> | null = null;
    private readonly click$ = new Subject<void>();

    ngOnInit(): void {
        const editor = this.editor();
        if (!editor) {
            throw new Error('Required: Input `editor`');
        }

        this.componentRef = this.vcr.createComponent(FloatingButtonComponent);
        const element = this.componentRef.location.nativeElement;

        // Forward component output via Subject so the plugin never subscribes to the
        // component's OutputRef (avoids NG0953 when the plugin view is created asynchronously).
        this.componentRef.instance.byClick.subscribe(() => this.click$.next());

        editor.registerPlugin(
            DotFloatingButtonPlugin({
                pluginKey: FLOATING_BUTTON_PLUGIN_KEY,
                editor,
                element,
                component: this.componentRef,
                onClick$: this.click$.asObservable(),
                dotUploadFileService: this.uploadService
            })
        );
    }

    ngOnDestroy(): void {
        this.click$.complete();
        this.editor().unregisterPlugin(FLOATING_BUTTON_PLUGIN_KEY);
        this.componentRef?.destroy();
        this.componentRef = null;
    }
}
