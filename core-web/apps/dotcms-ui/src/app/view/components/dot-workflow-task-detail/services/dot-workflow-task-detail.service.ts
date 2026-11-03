import { BehaviorSubject, Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, mergeMap } from 'rxjs/operators';

import { DotMenuService } from '../../../../api/services/dot-menu.service';

export interface DotTaskAction {
    header?: string;
    id: string;
}

/** The task the dialog shows, and how many times it has been viewed again since its first view. */
interface DotTaskView {
    id: string;
    reload?: number;
}

/**
 * Handle the url and events for add and edit workflow tasks components
 *
 * @export
 * @class DotWorkflowTaskDetailService
 */
@Injectable()
export class DotWorkflowTaskDetailService {
    private dotMenuService = inject(DotMenuService);

    private data: BehaviorSubject<DotTaskView> = new BehaviorSubject(null);
    private _header: BehaviorSubject<string> = new BehaviorSubject('');

    get viewUrl$(): Observable<string> {
        return this.data.pipe(mergeMap((view: DotTaskView) => this.getViewUrl(view)));
    }

    get header$(): Observable<string> {
        return this._header;
    }

    /**
     * Shows a workflow task in the dialog. Viewing the task the dialog already shows loads it
     * again: its URL gains a reload count the task screen ignores, since the dialog's iframe
     * reloads only when its URL changes. The task component does this after its content is saved
     * or closed in the side panel, so the detail shows the content's current step (#38303,
     * FR-025a).
     *
     * @param action The task, and the dialog's header.
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

    /**
     * The task screen's URL for a view, with the reload count when the task is viewed again.
     *
     * @param view The task to show, or `null` for none.
     */
    private getViewUrl(view: DotTaskView): Observable<string> {
        return view === null
            ? of('')
            : this.dotMenuService.getDotMenuId('workflow').pipe(
                  map((portletId: string) => {
                      return [
                          `/c/portal/layout`,
                          `?p_l_id=${portletId}`,
                          `&p_p_id=workflow`,
                          `&p_p_action=1`,
                          `&p_p_state=maximized`,
                          `&p_p_mode=view`,
                          `&_workflow_struts_action=/ext/workflows/edit_workflow_task`,
                          `&_workflow_cmd=view`,
                          `&_workflow_taskId=${view.id}`,
                          view.reload ? `&_workflow_reload=${view.reload}` : ''
                      ].join('');
                  })
              );
    }

    /**
     * Shows the task, counting a view of the task already shown as a reload.
     *
     * @param action The task, and the dialog's header.
     */
    private setData(action: DotTaskAction): void {
        if (action.header) {
            this._header.next(action.header);
        }

        const shown = this.data.getValue();
        const reload = shown && shown.id === action.id ? (shown.reload ?? 0) + 1 : 0;

        this.data.next({
            id: action.id,
            ...(reload ? { reload } : {})
        });
    }
}
