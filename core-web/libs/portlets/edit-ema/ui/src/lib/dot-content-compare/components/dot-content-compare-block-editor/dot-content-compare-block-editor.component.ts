import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { AfterViewInit, Component, Input, ViewChild, inject } from '@angular/core';
import { outputToObservable } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { map } from 'rxjs/operators';

import { DotCMSEditorComponent } from '@dotcms/new-block-editor';
import { DotSafeHtmlPipe, DotDiffPipe } from '@dotcms/ui';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';

@Component({
    selector: 'dot-content-compare-block-editor',
    templateUrl: './dot-content-compare-block-editor.component.html',
    imports: [DotCMSEditorComponent, DotSafeHtmlPipe, DotDiffPipe, AsyncPipe]
})
export class DotContentCompareBlockEditorComponent implements AfterViewInit {
    private sanitizer = inject(DomSanitizer);

    @ViewChild('blockEditor') blockEditor: DotCMSEditorComponent;
    @ViewChild('blockEditorCompare') blockEditorCompare: DotCMSEditorComponent;

    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;
    @Input() showAsCompare: boolean;
    @Input() field: string;

    htmlCompareValue$: Observable<SafeHtml>;
    htmlWorkingValue$: Observable<SafeHtml>;

    ngAfterViewInit(): void {
        this.htmlCompareValue$ = outputToObservable(this.blockEditorCompare?.valueChange).pipe(
            map(() => this.blockEditorCompare.editor.getHTML())
        );

        this.htmlWorkingValue$ = outputToObservable(this.blockEditor?.valueChange).pipe(
            map(() => this.blockEditor.editor.getHTML())
        );
    }
}
