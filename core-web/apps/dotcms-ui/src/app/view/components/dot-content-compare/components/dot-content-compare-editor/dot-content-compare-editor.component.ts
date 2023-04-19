import { AfterViewInit, Component, ElementRef, Input, ViewChild } from '@angular/core';

import { DotContentCompareTableData } from '@components/dot-content-compare/store/dot-content-compare.store';
import { DotDiffPipe } from '@dotcms/app/view/pipes';
import { DotBlockEditorComponent } from '@dotcms/block-editor';

@Component({
    selector: 'dot-content-compare-editor',
    templateUrl: './dot-content-compare-editor.component.html'
})
export class DotContentCompareEditorComponent implements AfterViewInit {
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;
    @ViewChild('blockEditorCompare') blockEditorCompare: DotBlockEditorComponent;
    @ViewChild('HtmlCompare') htmlCompare: ElementRef;
    @ViewChild('HtmlWorking') htmlWorking: ElementRef;

    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;
    @Input() field: string;
    @Input() label: boolean;

    private dotDiffPipe = new DotDiffPipe();

    htmlCompareValue: string;
    htmlWorkingValue: string;

    ngAfterViewInit(): void {
        this.blockEditor.value = this.data.working[this.field];
        this.blockEditor?.valueChange.subscribe(() => {
            this.htmlWorkingValue = this.blockEditor.editor.getHTML();
            if (this.htmlWorking) {
                this.htmlWorking.nativeElement.innerHTML = this.htmlWorkingValue;
            }
        });

        this.blockEditorCompare.value = this.data.compare[this.field];
        this.blockEditorCompare?.valueChange.subscribe(() => {
            this.htmlWorkingValue = this.blockEditorCompare.editor.getHTML();
            if (this.htmlCompare) {
                this.htmlCompare.nativeElement.innerHTML = this.dotDiffPipe.transform(
                    this.htmlWorkingValue,
                    this.htmlCompareValue,
                    true
                );
            }
        });
    }
}
