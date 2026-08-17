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
    // The parent only renders this component inside its `@if (editor)` block, so both are
    // always bound by the time the template reads them.
    @Input() characterCount!: CharacterCountStorageShape;
    @Input() charLimit = NaN;
    @Input() readingTime = 0;

    constructor() {
        //
    }
}
