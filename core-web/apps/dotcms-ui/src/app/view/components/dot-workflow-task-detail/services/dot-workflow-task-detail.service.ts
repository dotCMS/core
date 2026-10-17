import { BehaviorSubject, Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, mergeMap } from 'rxjs/operators';

import { GlobalStore } from '@dotcms/store';

export interface DotTaskAction {
    header?: string;
    id: string;
}

/**
 * Handle the url and events for add and edit workflow tasks components
 *
 * @export
 * @class DotWorkflowTaskDetailService
 */
@Injectable()
export class DotWorkflowTaskDetailService {
    readonly #globalStore = inject(GlobalStore);

    private data: BehaviorSubject<DotTaskAction> = new BehaviorSubject(null);
    private _header: BehaviorSubject<string> = new BehaviorSubject('');

    get viewUrl$(): Observable<string> {
        return this.data.pipe(mergeMap((action: DotTaskAction) => this.getViewUrl(action)));
    }

    get header$(): Observable<string> {
        return this._header;
    }

    /**
     * Set data to edit a workflow task
     *
     * @param DotTaskAction action
     * @memberof DotContentletEditorService
     */
    view(action: DotTaskAction): void {
        this.setData(action);
    }

    /**
     * Clear data to add a workflow task
     *
     * @memberof DotAddContentletService
     */
    clear() {
        this.data.next(null);
    }

    private getViewUrl(action: DotTaskAction): Observable<string> {
        return action === null
            ? of('')
            : this.#globalStore.ensureLoaded().pipe(
                  map(() => {
                      // The legacy workflow portlet is addressed by the layout that contains it.
                      const layoutId = this.#globalStore.layoutIdForPortlet('workflow');

                      return [
                          `/c/portal/layout`,
                          `?p_l_id=${layoutId}`,
                          `&p_p_id=workflow`,
                          `&p_p_action=1`,
                          `&p_p_state=maximized`,
                          `&p_p_mode=view`,
                          `&_workflow_struts_action=/ext/workflows/edit_workflow_task`,
                          `&_workflow_cmd=view`,
                          `&_workflow_taskId=${action.id}`
                      ].join('');
                  })
              );
    }

    private setData(action: DotTaskAction): void {
        if (action.header) {
            this._header.next(action.header);
        }

        this.data.next({
            id: action.id
        });
    }
}
