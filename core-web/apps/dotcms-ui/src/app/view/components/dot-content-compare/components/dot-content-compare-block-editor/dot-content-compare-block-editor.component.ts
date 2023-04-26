import { Observable } from 'rxjs';

import { AfterViewInit, Component, Input, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { map } from 'rxjs/operators';

import { DotContentCompareTableData } from '@components/dot-content-compare/store/dot-content-compare.store';
import { DotBlockEditorComponent } from '@dotcms/block-editor';

@Component({
    selector: 'dot-content-compare-block-editor',
    templateUrl: './dot-content-compare-block-editor.component.html'
})
export class DotContentCompareBlockEditorComponent implements AfterViewInit {
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;
    @ViewChild('blockEditorCompare') blockEditorCompare: DotBlockEditorComponent;

    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;
    @Input() showAsCompare: boolean;
    @Input() field: string;

    htmlCompareValue$: Observable<SafeHtml>;
    htmlWorkingValue$: Observable<SafeHtml>;

    constructor(private sanitizer: DomSanitizer) {}

    ngAfterViewInit(): void {
        this.htmlCompareValue$ = this.blockEditorCompare?.valueChange.pipe(
            map(() => this.blockEditorCompare.editor.getHTML())
        );

        this.htmlWorkingValue$ = this.blockEditor?.valueChange.pipe(
            map(() => this.blockEditor.editor.getHTML())
        );
    }
}
