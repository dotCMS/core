import { Observable, of, Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, filter, map, mergeMap, pluck, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

interface DotAddEditEvents {
    load?: ($event: Event) => void;
    keyDown?: ($event: KeyboardEvent) => void;
}

export interface DotEditorAction {
    header?: string;
    data: {
        [key: string]: string;
    };
    events?: DotAddEditEvents;
}

/**
 * Handle the url and events for add and edit contentlet components
 *
 * @export
 * @class DotContentletEditorService
 */
@Injectable()
export class DotContentletEditorService {
    private coreWebService = inject(CoreWebService);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    close$: Subject<boolean> = new Subject<boolean>();
    draggedContentType$: Subject<DotCMSContentType | DotCMSContentlet> = new Subject<
        DotCMSContentType | DotCMSContentlet
    >();

    private data: Subject<DotEditorAction> = new Subject();
    private _header: Subject<string> = new Subject();
    private _load: ($event: unknown) => void;
    private _keyDown: ($event: KeyboardEvent) => void;

    get addUrl$(): Observable<string> {
        return this.data.pipe(
            filter((action: DotEditorAction) => this.isAddUrl(action)),
            map((action: DotEditorAction) => this.geAddtUrl(action))
        );
    }

    get editUrl$(): Observable<string> {
        return this.data.pipe(
            filter((action: DotEditorAction) => this.isEditUrl(action)),
            mergeMap((action: DotEditorAction) => of(this.getEditUrl(action)))
        );
    }

    get createUrl$(): Observable<string> {
        return this.data.pipe(
            filter((action: DotEditorAction) => this.isCreateUrl(action)),
            map((action: DotEditorAction) => this.getCreateUrl(action))
        );
    }

    get header$(): Observable<string> {
        return this._header;
    }

    get loadHandler(): ($event: unknown) => void {
        return this._load;
    }

    get keyDownHandler(): ($event: KeyboardEvent) => void {
        return this._keyDown;
    }

    /**
     * Set url to create a contentlet
     *
     * @param DotEditorAction action
     * @memberof DotContentletEditorService
     */
    create(action: DotEditorAction): void {
        this.setData(action);
    }

    /**
     * Set data to add a contentlet
     *
     * @param DotEditorAction action
     * @memberof DotAddContentletServicex
     */
    add(action: DotEditorAction): void {
        this.setData(action);
    }

    /**
     * Set data to edit a contentlet
     *
     * @param DotEditorAction action
     * @memberof DotContentletEditorService
     */
    edit(action: DotEditorAction): void {
        this.setData(action);
    }

    /**
     * Clear data to add a contentlet
     *
     * @memberof DotAddContentletService
     */
    clear() {
        this.data.next(null);
        this._load = null;
        this._keyDown = null;
        this.close$.next(true);
    }

    /**
     * Call keydown handler
     *
     * @param KeyboardEvent $event
     * @memberof DotContentletEditorService
     */
    keyDown($event: KeyboardEvent): void {
        if (this._keyDown) {
            this._keyDown($event);
        }
    }

    /**
     * Call load handler
     *
     * @param unknown $event
     * @memberof DotContentletEditorService
     */
    load($event: unknown): void {
        if (this._load) {
            this._load($event);
        }
    }

    /**
     * Returns action url to display create contentlet dialog
     * @param {string} contentTypeVariable
     * @returns Observable<string>
     * @memberof DotContentletEditorService
     */
    getActionUrl(contentTypeVariable: string): Observable<string> {
        return this.coreWebService
            .requestView({
                url: `v1/portlet/_actionurl/${contentTypeVariable}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Set the content Type being dragged from the Content palette
     * @param {DotCMSContentType} contentType
     * @memberof DotContentletEditorService
     */
    setDraggedContentType(contentType: DotCMSContentType | DotCMSContentlet): void {
        this.draggedContentType$.next(contentType);
    }

    private bindEvents(events: DotAddEditEvents): void {
        if (events.load) {
            this._load = events.load;
        }

        if (events.keyDown) {
            this._keyDown = events.keyDown;
        }
    }

    private geAddtUrl(action: DotEditorAction): string {
        return action === null
            ? ''
            : `/html/ng-contentlet-selector.jsp?ng=true&container_id=${action.data.container}&add=${action.data.baseTypes}`;
    }

    private getCreateUrl(action: DotEditorAction): string {
        return action === null ? '' : action.data.url;
    }

    private getEditUrl(action: DotEditorAction): string {
        return action === null
            ? ''
            : [
                  `/c/portal/layout`,
                  `?p_p_id=content`,
                  `&p_p_action=1`,
                  `&p_p_state=maximized`,
                  `&p_p_mode=view`,
                  `&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet`,
                  `&_content_cmd=edit&inode=${action.data.inode}`
              ].join('');
    }

    private isAddUrl(action: DotEditorAction): boolean {
        return action === null || !!action.data.container;
    }

    private isCreateUrl(action: DotEditorAction): boolean {
        return action === null || !!action.data.url;
    }

    private isEditUrl(action: DotEditorAction): boolean {
        return action === null || !!action.data.inode;
    }

    private setData(action: DotEditorAction): void {
        if (action.events) {
            this.bindEvents(action.events);
        }

        if (action.header) {
            this._header.next(action.header);
        }

        this.data.next({
            data: action.data
        });
    }
}
