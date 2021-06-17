import { Component, Injector, ViewEncapsulation } from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import { ContentletBlockExtension } from '../extentions/contentlet-block.extension';

@Component({
    selector: 'dot-block-editor',
    templateUrl: './block-editor.component.html',
    styleUrls: ['./block-editor.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class BlockEditorComponent {
    items: any;
    selectedItem: any;
    editor: Editor;

    value = '<p>Hello, Tiptap!</p>'; // can be HTML or JSON, see https://www.tiptap.dev/api/editor#content

    constructor(private injector: Injector) {}

    ngOnInit() {
        this.editor = new Editor({
            extensions: [StarterKit, ContentletBlockExtension(this.injector)]
        });
    }

    handleSelected(event: any): void {
        this.selectedItem = event.option;
    }

    loadItems(items: any): void {
        this.items = items;
    }
}
