import { Injectable } from '@angular/core';
import { Subject, Observable, of } from 'rxjs';
import { mergeMap, map, filter } from 'rxjs/operators';

interface DotAddEditEvents {
    load?: ($event: any) => void;
    keyDown?: ($event: any) => void;
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
    close$: Subject<boolean> = new Subject<boolean>();

    private data: Subject<DotEditorAction> = new Subject();
    private _header: Subject<string> = new Subject();
    private _load: ($event: any) => void;
    private _keyDown: ($event: KeyboardEvent) => void;

    constructor() {}

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

    get loadHandler(): ($event: any) => void {
        return this._load;
    }

    get keyDownHandler(): ($event: any) => void {
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
     * @param * $event
     * @memberof DotContentletEditorService
     */
    load($event: any): void {
        if (this._load) {
            this._load($event);
        }
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
