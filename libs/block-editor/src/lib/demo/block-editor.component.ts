import {
    Component,
    ComponentFactoryResolver,
    Injector,
    OnInit,
    ViewEncapsulation
} from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { ActionsMenu } from '../extensions/actions-menu.extension';
import { ContentletBlock } from '../extensions/blocks/contentlet-block/contentlet-block.extension';
import { DragHandler } from '../extensions/dragHandler.extention';

import { ImageUpload } from '../extensions/imageUpload.extention';
import { ImageBlock } from '../extensions/blocks/image-block/image-block.extention';
import BubbleMenu from '@tiptap/extension-bubble-menu';

// Marks Extensions
import { TextAlign } from '@tiptap/extension-text-align';
import { Underline } from '@tiptap/extension-underline';

@Component({
    selector: 'dotcms-block-editor',
    templateUrl: './block-editor.component.html',
    styleUrls: ['./block-editor.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class BlockEditorComponent implements OnInit {
    editor: Editor;

    value = '<p>Hello, Tiptap!</p>'; // can be HTML or JSON, see https://www.tiptap.dev/api/editor#content

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
