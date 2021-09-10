import { 
  Component,
  OnInit,
  ComponentFactoryResolver,
  Injector,
  ViewEncapsulation
} from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { ContentletBlock } from '@dotcms/block-editor';
import { ActionsMenu } from '@dotcms/block-editor';

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
                ActionsMenu(this.injector, this.resolver),
            ]
        });
    }
}
