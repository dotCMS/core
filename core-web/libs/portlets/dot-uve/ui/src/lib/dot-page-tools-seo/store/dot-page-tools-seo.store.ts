import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageTool, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { getRunnableLink } from '@dotcms/utils';

export interface DotPageToolsSeoState {
    pageTools: DotPageTool[];
}

/**
 * Handle page tools state
 * @export  DotPageToolsSeoStore
 */
@Injectable()
export class DotPageToolsSeoStore extends ComponentStore<DotPageToolsSeoState> {
    private dotPageToolsService = inject(DotPageToolsService);

    readonly tools$ = this.select<DotPageToolsSeoState>((state) => state);

    constructor() {
        const initialState: DotPageToolsSeoState = {
            pageTools: []
        };
        super(initialState);
    }

    /**
     * Get page tools
     *
     * @memberof DotPageToolsSeoStore
     */
    readonly getTools = this.effect((pageToolUrlParams$: Observable<DotPageToolUrlParams>) => {
        return pageToolUrlParams$.pipe(
            switchMap((pageToolUrlParams) => {
                return this.dotPageToolsService.get().pipe(
                    map((tools: DotPageTool[]) => {
                        const updatedTools = tools.map((tool) => {
                            return {
                                ...tool,
                                runnableLink: getRunnableLink(tool.runnableLink, pageToolUrlParams)
                            };
                        });

                        return updatedTools;
                    }),
                    tapResponse({
                        next: (tools: DotPageTool[]) => this.updatePageTools(tools),
                        error: (error: HttpErrorResponse) => console.error(error)
                    })
                );
            })
        );
    });

    /**
     * Update page tools
     *
     * @memberof DotPageToolsSeoStore
     */
    readonly updatePageTools = this.updater<DotPageTool[]>((state, pageTools) => {
        return {
            ...state,
            pageTools
        };
    });
}
