import { Component, EventEmitter, Input, Output } from '@angular/core';

import { DotContentCompareTableData } from '@components/dot-content-compare/store/dot-content-compare.store';
import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DomSanitizer} from '@angular/platform-browser'

@Component({
    selector: 'dot-content-compare-table',
    templateUrl: './dot-content-compare-table.component.html',
    styleUrls: ['./dot-content-compare-table.component.scss'],
})
export class DotContentCompareTableComponent {
    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;

    @Output() changeVersion = new EventEmitter<DotCMSContentlet>();
    @Output() changeDiff = new EventEmitter<boolean>();
    @Output() bringBack = new EventEmitter<string>();

    displayOptions = [
        { label: this.dotMessageService.get('diff'), value: true },
        { label: this.dotMessageService.get('plain'), value: false }
    ];

    testHTMLtoWorking = `
            <dot-contentlet-block _nghost-vat-c42="" contenteditable="false" draggable="true"><p-card _ngcontent-vat-c42=""
            class="p-element">
            <div class="p-card p-component">
                <div class="p-card-header"><dot-contentlet-thumbnail _ngcontent-vat-c42="" height="94" width="94" alt=""
                        icon-size="72px" cover="" class="hydrated">
                        <div class="thumbnail cover"
                            style="background-image: url(&quot;/dA/d77576ce-6e3a-4cf3-b412-8e5209f56cae/500w/20q?r=undefined&quot;);">
                            <img src="/dA/d77576ce-6e3a-4cf3-b412-8e5209f56cae/500w/20q?r=undefined" alt="" aria-label="">
                        </div>
                    </dot-contentlet-thumbnail><!----></div><!---->
                <div class="p-card-body">
                    <div class="p-card-title">
                        <h3 _ngcontent-vat-c42="" class="title">Snowboarding</h3><!---->
                    </div><!---->
                    <div class="p-card-subtitle"> <span _ngcontent-vat-c42="">Activity</span><!----></div><!---->
                    <div class="p-card-content"><!----><!----><!----><!----><!----></div>
                    <div class="p-card-footer">
                        <div _ngcontent-vat-c42="" class="state"><dot-state-icon _ngcontent-vat-c42="" size="16px"
                                aria-label="Published" class="hydrated" style="--size:16px;"></dot-state-icon><dot-badge
                                _ngcontent-vat-c42="" class="hydrated">en-us</dot-badge></div><!---->
                    </div><!---->
                </div>
            </div>
        </p-card></dot-contentlet-block>
        <p><br class="ProseMirror-trailingBreak"></p>
        <p data-placeholder="Type &quot;/&quot; for commmands" class="is-empty"><br class="ProseMirror-trailingBreak"></p>
    `;
    testHTMLtoCompare = `
        <dot-contentlet-block _nghost-qeb-c42="" contenteditable="false" draggable="true"><p-card _ngcontent-qeb-c42=""
            class="p-element">
            <div class="p-card p-component">
                <div class="p-card-header"><dot-contentlet-thumbnail _ngcontent-qeb-c42="" height="94" width="94" alt=""
                        icon-size="72px" cover="" class="hydrated">
                        <div class="thumbnail cover"
                            style="background-image: url(&quot;/dA/64ddfdd1-c155-402e-b670-576fed17d722/500w/20q?r=undefined&quot;);">
                            <img src="/dA/64ddfdd1-c155-402e-b670-576fed17d722/500w/20q?r=undefined" alt="" aria-label="">
                        </div>
                    </dot-contentlet-thumbnail><!----></div><!---->
                <div class="p-card-body">
                    <div class="p-card-title">
                        <h3 _ngcontent-qeb-c42="" class="title">Hiking</h3><!---->
                    </div><!---->
                    <div class="p-card-subtitle"> <span _ngcontent-qeb-c42="">Activity</span><!----></div><!---->
                    <div class="p-card-content"><!----><!----><!----><!----><!----></div>
                    <div class="p-card-footer">
                        <div _ngcontent-qeb-c42="" class="state"><dot-state-icon _ngcontent-qeb-c42="" size="16px"
                                aria-label="Published" class="hydrated" style="--size:16px;"></dot-state-icon><dot-badge
                                _ngcontent-qeb-c42="" class="hydrated">en-us</dot-badge></div><!---->
                    </div><!---->
                </div>
            </div>
        </p-card></dot-contentlet-block>
    <p><br class="ProseMirror-trailingBreak"></p>
    <p data-placeholder="Type &quot;/&quot; for commmands" class="is-empty"><br class="ProseMirror-trailingBreak"></p>
    `;

    //testSanitizeHTML = this.sanitizer.bypassSecurityTrustHtml(this.testHTMLtoWorking)

    constructor(private dotMessageService: DotMessageService, private sanitizer: DomSanitizer) {}
}
