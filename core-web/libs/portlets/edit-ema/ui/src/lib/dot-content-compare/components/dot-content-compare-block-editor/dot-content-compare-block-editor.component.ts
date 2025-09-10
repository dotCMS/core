import { Observable } from 'rxjs';

import { AfterViewInit, Component, Input, ViewChild, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { map } from 'rxjs/operators';

import { DotBlockEditorComponent } from '@dotcms/block-editor';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';

@Component({
    selector: 'dot-content-compare-block-editor',
    templateUrl: './dot-content-compare-block-editor.component.html',
    standalone: false
})
export class DotContentCompareBlockEditorComponent implements AfterViewInit {
    private sanitizer = inject(DomSanitizer);

    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;
    @ViewChild('blockEditorCompare') blockEditorCompare: DotBlockEditorComponent;

    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;
    @Input() showAsCompare: boolean;
    @Input() field: string;

    htmlCompareValue$: Observable<SafeHtml>;
    htmlWorkingValue$: Observable<SafeHtml>;

    ngAfterViewInit(): void {
        this.htmlCompareValue$ = this.blockEditorCompare?.valueChange.pipe(
            map(() => this.blockEditorCompare.editor.getHTML())
        );

        this.htmlWorkingValue$ = this.blockEditor?.valueChange.pipe(
            map(() => this.blockEditor.editor.getHTML())
        );
    }
}
