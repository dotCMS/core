import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';

import { IframeComponent } from '@components/_common/iframe/iframe-component';
import { DotLayout, FeaturedFlags } from '@dotcms/dotcms-models';

import { DotTemplateItem } from '../store/dot-template.store';

@Component({
    selector: 'dot-template-builder',
    templateUrl: './dot-template-builder.component.html',
    styleUrls: ['./dot-template-builder.component.scss']
})
export class DotTemplateBuilderComponent implements OnInit {
    @Input() item: DotTemplateItem;
    @Input() didTemplateChanged: boolean;
    @Output() saveAndPublish = new EventEmitter<DotTemplateItem>();
    @Output() updateTemplate = new EventEmitter<DotTemplateItem>();
    @Output() save = new EventEmitter<DotTemplateItem>();
    @Output() cancel = new EventEmitter();
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();
    @ViewChild('historyIframe') historyIframe: IframeComponent;
    permissionsUrl = '';
    historyUrl = '';
    featureFlag = FeaturedFlags.FEATURE_FLAG_TEMPLATE_BUILDER;

    ngOnInit() {
        this.permissionsUrl = `/html/templates/permissions.jsp?templateId=${this.item.identifier}&popup=true`;
        this.historyUrl = `/html/templates/push_history.jsp?templateId=${this.item.identifier}&popup=true`;
    }

    /**
     * Update template and publish it
     *
     * @param {DotLayout} layout
     * @memberof DotTemplateBuilderComponent
     */
    onLayoutChange(layout: DotLayout) {
        this.updateTemplate.emit({
            ...this.item,
            layout
        } as DotTemplateItem);

        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }
    }

    /**
     * Update template and publish it
     *
     * @param {DotTemplateItem} item
     * @memberof DotTemplateBuilderComponent
     */
    onTemplateItemChange(item: DotTemplateItem) {
        this.updateTemplate.emit(item);
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }
    }
}
