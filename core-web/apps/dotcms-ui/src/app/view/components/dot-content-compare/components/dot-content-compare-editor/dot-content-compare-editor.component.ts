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

    testHTMLtoWorking = '';

    // Temporal HTML to test the component, it would be removed when the component is integrated.
    testHTMLtoCompare = '';

    ngAfterViewInit(): void {
        setTimeout(() => {
            this.blockEditor.editor.commands.setContent(this.data.working['blog']);
            this.testHTMLtoWorking = this.blockEditor.editor.getHTML();

            this.blockEditor.editor.commands.setContent(this.data.compare['blog']);
            this.testHTMLtoCompare = this.blockEditor.editor.getHTML();
        }, 0);
    }
}
