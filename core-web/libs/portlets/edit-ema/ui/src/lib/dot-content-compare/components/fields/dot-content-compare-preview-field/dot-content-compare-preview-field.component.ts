import { Component, Input, OnChanges } from '@angular/core';

@Component({
    selector: 'dot-content-compare-preview-field',
    templateUrl: './dot-content-compare-preview-field.component.html',
    styleUrls: ['./dot-content-compare-preview-field.component.scss'],
    standalone: false
})
export class DotContentComparePreviewFieldComponent implements OnChanges {
    @Input() fileURL: string;
    @Input() label: string;
    imageError = false;

    ngOnChanges() {
        this.imageError = false;
    }

    handleError(): void {
        this.imageError = true;
    }
}
