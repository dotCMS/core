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
    @Input() field: string;

    htmlCompareValue$: Observable<SafeHtml>;
    htmlWorkingValue$: Observable<SafeHtml>;

    constructor(private sanitizer: DomSanitizer) {}

    getSafeHtml(html: string): SafeHtml {
        return this.sanitizer.bypassSecurityTrustHtml(html);
    }

    ngAfterViewInit(): void {
        this.htmlCompareValue$ = this.blockEditorCompare?.valueChange.pipe(
            map(() => this.getSafeHtml(this.blockEditorCompare.editor.getHTML()))
        );
        this.htmlWorkingValue$ = this.blockEditor?.valueChange.pipe(
            map(() => this.getSafeHtml(this.blockEditor.editor.getHTML()))
        );
    }
}
