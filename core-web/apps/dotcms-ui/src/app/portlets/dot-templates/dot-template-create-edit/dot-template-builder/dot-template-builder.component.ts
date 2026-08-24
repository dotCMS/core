import { Subject } from 'rxjs';

import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
    ChangeDetectionStrategy
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TabsModule } from 'primeng/tabs';

import { debounceTime, takeUntil } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotTemplateDesigner } from '@dotcms/dotcms-models';
import { TemplateBuilderComponent } from '@dotcms/template-builder';
import { DotMessagePipe } from '@dotcms/ui';

import { DotGlobalMessageComponent } from '../../../../view/components/_common/dot-global-message/dot-global-message.component';
import { IframeComponent } from '../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotTemplateAdvancedComponent } from '../dot-template-advanced/dot-template-advanced.component';
import { DotTemplateItem, DotTemplateItemDesign } from '../store/dot-template.store';

export const AUTOSAVE_DEBOUNCE_TIME = 5000;

/**
 * What the two editors emit. The advanced editor sends a whole `DotTemplateItem`; the designer
 * lib's `templateChange` sends a `DotTemplateDesigner` (`{ themeId, layout }`). Both end up in
 * `lastTemplate` and are forwarded to `save`.
 */
type DotTemplateEditorEvent = DotTemplateItem | DotTemplateDesigner;

@Component({
    selector: 'dot-template-builder',
    templateUrl: './dot-template-builder.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        DotMessagePipe,
        DotTemplateAdvancedComponent,
        TabsModule,
        IframeComponent,
        TemplateBuilderComponent,
        ButtonModule,
        DotGlobalMessageComponent
    ]
})
export class DotTemplateBuilderComponent implements OnInit, OnDestroy {
    readonly #dotRouterService = inject(DotRouterService);

    private _item!: DotTemplateItem;

    @Input()
    set item(value: DotTemplateItem) {
        this._item = value;
        this.lastTemplate = value;
    }
    get item(): DotTemplateItem {
        return this._item;
    }
    @Input() didTemplateChanged!: boolean;
    @Output() saveAndPublish = new EventEmitter<DotTemplateItem>();
    @Output() updateTemplate = new EventEmitter<DotTemplateItem>();
    @Output() save = new EventEmitter<DotTemplateEditorEvent>();
    @Output() cancel = new EventEmitter();
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();
    @ViewChild('historyIframe') historyIframe!: IframeComponent;
    permissionsUrl = '';
    historyUrl = '';

    templateUpdate$ = new Subject<DotTemplateEditorEvent>();
    destroy$: Subject<boolean> = new Subject<boolean>();
    lastTemplate!: DotTemplateEditorEvent;

    /**
     * Theme id for the designer, from whichever shape `lastTemplate` currently holds.
     *
     * `templateChange` on the builder lib emits a `DotTemplateDesigner` (`{ themeId, layout }`),
     * not a `DotTemplateItem`, so after the first edit `lastTemplate` carries `themeId` while the
     * value arriving via `[item]` carries `theme`. The template read `theme ?? themeId` to cover
     * both; this keeps that behaviour in one typed place.
     *
     * TODO(#37120): `lastTemplate` really does hold two unrelated shapes, and `save` emits
     * whichever is current. Reconciling them changes what reaches the store, so it needs its
     * own issue rather than a drive-by fix here.
     */
    protected get lastTemplateThemeId(): string | undefined {
        const template = this.#lastTemplateFields;

        return template.theme ?? template.themeId ?? undefined;
    }

    /** Identifier of the current template; absent on the designer's partial payload. */
    protected get lastTemplateIdentifier(): string | undefined {
        return this.#lastTemplateFields.identifier;
    }

    get #lastTemplateFields(): Partial<DotTemplateItemDesign & DotTemplateDesigner> {
        return this.lastTemplate as Partial<DotTemplateItemDesign & DotTemplateDesigner>;
    }

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
    onTemplateItemChange(item: DotTemplateEditorEvent) {
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }

        this.#dotRouterService.forbidRouteDeactivation();
        this.lastTemplate = item;
        // We intentionally do NOT call `updateTemplate.emit(item)` here. Doing so updates
        // the parent store's `working` state, which echoes back through `[item]="vm.working"`
        // → `[layout]="item.layout"` → ngOnChanges → updateOldRows (see
        // libs/template-builder/.../template-builder.component.ts ngOnChanges). That round-
        // trip happens synchronously on each templateChange and assumes state.row.y matches
        // newRows.y. After removeRow, state has gaps in y (e.g. [0, 2]) while parsed newRows
        // has sequential y ([0, 1]), so updateOldRows produced corrupt rows. Mirrors the EMA
        // pattern (edit-ema-layout.component.ts) — the layout only flows back to the
        // designer after a successful save (via onSaveTemplate updating original+working).
        // NOTE: the layout still echoes back on save-emit via the store's synchronous
        // updateWorkingTemplate in saveTemplate (store/dot-template.store.ts:199-202), but
        // only at debounce-fire time when the lib has been idle for 5s — narrowed, not
        // eliminated. If you ever change the save trigger to fire faster or in response
        // to a non-debounced event, re-evaluate this cycle.
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
