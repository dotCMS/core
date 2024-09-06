import { Subject } from 'rxjs';

import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { debounceTime, takeUntil } from 'rxjs/operators';

import { IframeComponent } from '@components/_common/iframe/iframe-component';
import { DotRouterService } from '@dotcms/data-access';

import { DotTemplateItem } from '../store/dot-template.store';

export const AUTOSAVE_DEBOUNCE_TIME = 5000;

@Component({
    selector: 'dot-template-builder',
    templateUrl: './dot-template-builder.component.html',
    styleUrls: ['./dot-template-builder.component.scss']
})
export class DotTemplateBuilderComponent implements OnInit, OnDestroy {
    readonly #dotRouterService = inject(DotRouterService);

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

    templateUpdate$ = new Subject<DotTemplateItem>();
    destroy$: Subject<boolean> = new Subject<boolean>();
    lastTemplate: DotTemplateItem;

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
    onTemplateItemChange(item: DotTemplateItem) {
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }

        this.#dotRouterService.forbidRouteDeactivation();
        this.lastTemplate = item;

        this.templateUpdate$.next(item);
    }

    private saveTemplateDebounce() {
        // Approach based on DotEditLayoutComponent, see that component for more info
        this.templateUpdate$
            .pipe(debounceTime(AUTOSAVE_DEBOUNCE_TIME), takeUntil(this.destroy$))
            .subscribe((templateItem) => {
                this.save.emit(templateItem);
            });
    }

    private subscribeOnChangeBeforeLeaveHandler(): void {
        this.#dotRouterService.pageLeaveRequest$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.save.emit(this.lastTemplate);
        });
    }
}
