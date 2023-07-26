import { Subject } from 'rxjs';

import {
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { debounceTime, map, takeUntil } from 'rxjs/operators';

import { IframeComponent } from '@components/_common/iframe/iframe-component';
import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { DotTemplateItem } from '../store/dot-template.store';

export const AUTOSAVE_DEBOUNCE_TIME = 1000;

@Component({
    selector: 'dot-template-builder',
    templateUrl: './dot-template-builder.component.html',
    styleUrls: ['./dot-template-builder.component.scss']
})
export class DotTemplateBuilderComponent implements OnInit, OnDestroy {
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
    readonly featureFlag = FeaturedFlags.FEATURE_FLAG_TEMPLATE_BUILDER;
    featureFlagIsOn$ = this.propertiesService
        .getKey(this.featureFlag)
        .pipe(map((result) => result && result === 'true'));
    templateUpdate$ = new Subject<DotTemplateItem>();
    destroy$: Subject<boolean> = new Subject<boolean>();
    lastTemplate: DotTemplateItem;

    constructor(
        private propertiesService: DotPropertiesService,
        private dotRouterService: DotRouterService
    ) {}

    ngOnInit() {
        this.permissionsUrl = `/html/templates/permissions.jsp?templateId=${this.item.identifier}&popup=true`;
        this.historyUrl = `/html/templates/push_history.jsp?templateId=${this.item.identifier}&popup=true`;
        this.saveTemplateDebounce();
        this.subscribeOnChangeBeforeLeaveHandler();
    }

    ngOnDestroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Update template and publish it
     *
     * @param {DotTemplateItem} item
     * @memberof DotTemplateBuilderComponent
     */
    onTemplateItemChange(item: DotTemplateItem, type: string) {
        console.log('onTemplateItemChange', type);
        this.updateTemplate.emit(item);
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }

        this.dotRouterService.forbidRouteDeactivation();
        this.lastTemplate = item;

        this.templateUpdate$.next(item);
    }

    saveTemplateDebounce() {
        // Approach based on DotEditLayoutComponent, see that component for more info
        this.templateUpdate$
            .pipe(debounceTime(AUTOSAVE_DEBOUNCE_TIME), takeUntil(this.destroy$))
            .subscribe((templateItem) => {
                console.log('templateUpdate$');
                this.saveAndPublish.emit(templateItem);
            });
    }

    private subscribeOnChangeBeforeLeaveHandler(): void {
        this.dotRouterService.pageLeaveRequest$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            console.log('pageLeaveRequest$');
            this.saveAndPublish.emit(this.lastTemplate);
        });
    }
}
