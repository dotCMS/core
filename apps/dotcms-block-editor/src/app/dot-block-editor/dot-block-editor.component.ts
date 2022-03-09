import {
    Component,
    OnInit,
    ComponentFactoryResolver,
    Injector,
    ViewEncapsulation
} from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import {
    ActionsMenu,
    BubbleLinkFormExtension,
    ContentletBlock,
    DotBubbleMenuExtension,
    DragHandler,
    ImageBlock,
    ImageUpload,
    BubbleMenuComponent
} from '@dotcms/block-editor';

// Marks Extensions
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import { TextAlign } from '@tiptap/extension-text-align';
import { Underline } from '@tiptap/extension-underline';
import { ViewContainerRef } from '@angular/core';

@Component({
    selector: 'dotcms-block-editor',
    templateUrl: './dot-block-editor.component.html',
    styleUrls: ['./dot-block-editor.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class DotBlockEditorComponent implements OnInit {
    editor: Editor;

    value = ''; // can be HTML or JSON, see https://www.tiptap.dev/api/editor#content

    constructor(
        private injector: Injector,
        private resolver: ComponentFactoryResolver,
        public viewContainerRef: ViewContainerRef
    ) {}

    ngOnInit() {
        this.editor = new Editor({
            extensions: [
                StarterKit,
                ContentletBlock(this.injector),
                ImageBlock(this.injector),
                ActionsMenu(this.viewContainerRef),
                DragHandler(this.injector, this.resolver),
                ImageUpload(this.injector, this.resolver),
                BubbleLinkFormExtension(this.injector, this.resolver),
                DotBubbleMenuExtension(BubbleMenuComponent, this.viewContainerRef),
                // Marks Extensions
                Underline,
                TextAlign.configure({ types: ['heading', 'paragraph', 'listItem', 'dotImage'] }),
                Highlight.configure({ HTMLAttributes: { class: 'highlighted' } }),
                Link.configure({ openOnClick: true })
            ]
        });
    }
}
