import { AfterViewInit, Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';

import { DotContentCompareTableData } from '@components/dot-content-compare/store/dot-content-compare.store';
import { DotBlockEditorComponent } from '@dotcms/block-editor';
import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-content-compare-table',
    templateUrl: './dot-content-compare-table.component.html',
    styleUrls: ['./dot-content-compare-table.component.scss']
})
export class DotContentCompareTableComponent implements AfterViewInit {
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;
    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;

    @Output() changeVersion = new EventEmitter<DotCMSContentlet>();
    @Output() changeDiff = new EventEmitter<boolean>();
    @Output() bringBack = new EventEmitter<string>();

    displayOptions = [
        { label: this.dotMessageService.get('diff'), value: true },
        { label: this.dotMessageService.get('plain'), value: false }
    ];
    testHTMLtoWorking = '';

    /*    testHTMLtoWorking = `
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
    `; */
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

    constructor(private dotMessageService: DotMessageService) {}

    ngAfterViewInit(): void {
        setTimeout(() => {
            this.blockEditor.editor.commands.setContent(
                JSON.parse(
                    `{"type":"doc","attrs":{"chartCount":2,"wordCount":1,"readingTime":1},"content":[{"type":"dotContent","attrs":{"data":{"publishDate":"2021-04-08 13:53:32.618","body":"<p>As with skiing, there are different styles of riding. Free-riding is all-mountain snowboarding on the slopes, in the trees, down the steeps and through the moguls. Freestyle is snowboarding in a pipe or park filled with rails, fun boxes and other features.<br /><br />Snowboarding parks are designed for specific skill levels, from beginner parks with tiny rails hugging the ground to terrain parks with roller-coaster rails, fun boxes and tabletops for more experienced snowboarders.<br /><br />Whether you're a first-timer or already comfortable going lip-to-lip in a pipe, there are classes and special clinics for you at our ski and snowboard resorts. Our resorts offer multiday clinics, so if you're headed to ski this winter, consider wrapping your vacation dates around a snowboarding clinic.</p>","inode":"d77576ce-6e3a-4cf3-b412-8e5209f56cae","host":"48190c8c-42c4-46af-8d1a-0cd5db894797","variantId":"DEFAULT","locked":false,"stInode":"778f3246-9b11-4a2a-a101-e7fdf111bdad","contentType":"Activity","altTag":"Snowboarding","identifier":"574f0aec-185a-4160-9c17-6d037b298318","image":"/dA/574f0aec-185a-4160-9c17-6d037b298318/image/box-info-2-270x270.jpg","urlTitle":"snowboarding","tags":"snowboarding,winterenthusiast:persona","folder":"SYSTEM_FOLDER","hasTitleImage":true,"sortOrder":0,"hostName":"demo.dotcms.com","modDate":"2021-04-08 13:53:32.618","imageMetaData":{"modDate":1680040290173,"sha256":"01bed04a0807b45245d38188da3bece44e42fcdd0cf8e8bfe0585e8bd7a61913","length":15613,"title":"box-info-2-270x270.jpg","version":20220201,"isImage":true,"fileSize":15613,"name":"box-info-2-270x270.jpg","width":270,"contentType":"image/jpeg","height":270},"description":"Snowboarding, once a prime route for teen rebellion, today is definitely mainstream. Those teens — both guys and Shred Bettys, who took up snowboarding in the late '80s and '90s now are riding with their kids.","title":"Snowboarding","baseType":"CONTENT","archived":false,"working":true,"live":true,"owner":"dotcms.org.1","imageVersion":"/dA/d77576ce-6e3a-4cf3-b412-8e5209f56cae/image/box-info-2-270x270.jpg","imageContentAsset":"574f0aec-185a-4160-9c17-6d037b298318/image","languageId":1,"URL_MAP_FOR_CONTENT":"/activities/snowboarding","url":"/content.2f6fe5b8-a2cc-4ecb-a868-db632d695fca","titleImage":"image","modUserName":"Admin User","urlMap":"/activities/snowboarding","hasLiveVersion":true,"modUser":"dotcms.org.1","__icon__":"contentIcon","contentTypeIcon":"paragliding","language":"en-US"}}},{"type":"paragraph","attrs":{"textAlign":"left"}}]}`
                )
            );
            this.testHTMLtoWorking = this.blockEditor.editor.view.dom.innerHTML;
        }, 0);
    }
}
