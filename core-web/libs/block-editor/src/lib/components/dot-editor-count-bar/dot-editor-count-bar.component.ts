import { Component, Input } from '@angular/core';

import { CharacterCountStorage } from '@tiptap/extension-character-count';

@Component({
    selector: 'dot-editor-count-bar',
    templateUrl: './dot-editor-count-bar.component.html',
    styleUrls: ['./dot-editor-count-bar.component.css'],
    standalone: false
})
export class DotEditorCountBarComponent {
    @Input() characterCount: CharacterCountStorage;
    @Input() charLimit: number;
    @Input() readingTime;

    constructor() {
        //
    }
}
