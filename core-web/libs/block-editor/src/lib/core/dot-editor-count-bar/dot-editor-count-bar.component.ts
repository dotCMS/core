import { Component, input } from '@angular/core';

import { CharacterCountStorage } from '@tiptap/extension-character-count';

@Component({
    selector: 'dot-editor-count-bar',
    templateUrl: './dot-editor-count-bar.component.html',
    styleUrls: ['./dot-editor-count-bar.component.css'],
    standalone: true
})
export class DotEditorCountBarComponent {
    characterCount = input.required<CharacterCountStorage>();
    charLimit = input<number>();
    readingTime = input<number>();
}
