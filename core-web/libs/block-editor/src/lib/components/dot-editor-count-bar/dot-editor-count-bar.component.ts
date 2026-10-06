import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

// v3 stopped exporting CharacterCountStorage; mirror the shape locally.
interface CharacterCountStorageShape {
    characters: () => number;
    words: () => number;
}

@Component({
    selector: 'dot-editor-count-bar',
    templateUrl: './dot-editor-count-bar.component.html',
    styleUrls: ['./dot-editor-count-bar.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class DotEditorCountBarComponent {
    @Input() characterCount: CharacterCountStorageShape;
    @Input() charLimit: number;
    @Input() readingTime;

    constructor() {
        //
    }
}
