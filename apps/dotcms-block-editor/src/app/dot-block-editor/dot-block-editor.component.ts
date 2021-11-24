import {
    Component,
    OnInit,
    ComponentFactoryResolver,
    Injector,
    ViewEncapsulation
} from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { ContentletBlock, ImageBlock, ImageUpload } from '@dotcms/block-editor';
import { ActionsMenu, DragHandler } from '@dotcms/block-editor';

// Marks Extensions
import { TextAlign } from '@tiptap/extension-text-align';
import { Underline } from '@tiptap/extension-underline';
import BubbleMenu from '@tiptap/extension-bubble-menu';

@Component({
    // eslint-disable-next-line
    selector: 'dot-block-editor',
    templateUrl: './dot-block-editor.component.html',
    styleUrls: ['./dot-block-editor.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class DotBlockEditorComponent implements OnInit {
    editor: Editor;

    value = ''; // can be HTML or JSON, see https://www.tiptap.dev/api/editor#content

    constructor(private injector: Injector, private resolver: ComponentFactoryResolver) {}

    ngOnInit() {
        this.editor = new Editor({
            extensions: [
                StarterKit,
                ContentletBlock(this.injector),
                ImageBlock(this.injector),
                ActionsMenu(this.injector, this.resolver),
                DragHandler(this.injector, this.resolver),
                ImageUpload(this.injector, this.resolver),
                BubbleMenu.configure({
                    element: document.querySelector('#bubbleMenu'),
                    tippyOptions: {
                        duration: 500,
                        maxWidth: 'none',
                        placement: 'bottom-start',
                        trigger: 'manual'
                    }
                }),
                // Marks Extensions
                Underline,
                TextAlign.configure({
                    types: ['heading', 'paragraph'],
                })
            ]
        });
    }
}
