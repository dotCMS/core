import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, Input, ViewChild, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { map } from 'rxjs/operators';

import { DotSafeHtmlPipe, DotDiffPipe } from '@dotcms/ui';

import { BlockEditorMockComponent } from './block-editor-mock/block-editor-mock.component';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';

@Component({
    selector: 'dot-content-compare-block-editor',
    templateUrl: './dot-content-compare-block-editor.component.html',
    imports: [CommonModule, BlockEditorMockComponent, DotSafeHtmlPipe, DotDiffPipe]
})
export class DotContentCompareBlockEditorComponent implements AfterViewInit {
    private sanitizer = inject(DomSanitizer);

    @ViewChild('blockEditor') blockEditor: BlockEditorMockComponent;
    @ViewChild('blockEditorCompare') blockEditorCompare: BlockEditorMockComponent;

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
