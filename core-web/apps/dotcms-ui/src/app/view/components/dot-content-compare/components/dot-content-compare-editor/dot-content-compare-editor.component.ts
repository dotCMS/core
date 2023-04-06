import { AfterViewInit, Component, Input, ViewChild } from '@angular/core';

import { DotContentCompareTableData } from '@components/dot-content-compare/store/dot-content-compare.store';
import { DotBlockEditorComponent } from '@dotcms/block-editor';

@Component({
    selector: 'dot-content-compare-editor',
    templateUrl: './dot-content-compare-editor.component.html'
})
export class DotContentCompareEditorComponent implements AfterViewInit {
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;

    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;
    @Input() field: string;
    @Input() label: boolean;

    HTMLWorking: string;
    HTMLCompare: string;
    ngAfterViewInit(): void {
        setTimeout(() => {
            this.blockEditor?.editor?.commands.setContent(this.data.working[this.field]);
            this.HTMLWorking = this.blockEditor?.editor?.getHTML();

            this.blockEditor?.editor?.commands.setContent(this.data.compare[this.field]);
            this.HTMLCompare = this.blockEditor?.editor.getHTML();
        }, 0);
    }
}
