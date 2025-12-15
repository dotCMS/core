import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { DotESContentService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCurrentUser,
    DotLanguage,
    DotPagination
} from '@dotcms/dotcms-models';

export interface DotCMSPagesPortletState {
    pages: DotCMSContentlet[];
    pagination: DotPagination;
    languages: DotLanguage[];
    currentUser?: DotCurrentUser;
    status: 'loading' | 'loaded' | 'error' | 'idle'; // replaces portletStatus
}

const initialState: DotCMSPagesPortletState = {
    pages: [],
    pagination: {
        currentPage: 1,
        perPage: 10,
        totalEntries: 0
    },
    languages: [],
    currentUser: null,
    status: 'loading'
};

const getPagesQuery = ({ languageId, archived }) => {
    const langQuery = languageId ? `+languageId:${languageId}` : '';
    const archivedQuery = archived ? `+deleted:true` : '+deleted:false';
    // const identifierQuery = identifier ? `+identifier:${identifier}` : '';
    // const keywordQuery = keyword
    //     ? `+(title:${keyword}* OR path:*${keyword}* OR urlmap:*${keyword}*)`
    //     : '';

    return `+working:true +(urlmap:* OR basetype:5) ${langQuery} ${archivedQuery}`;
};

export const DotCMSPagesStore = signalStore(
    withState(initialState),
    withComputed((store) => {
        return {
            $totalRecords: computed<number>(() => store.pagination.totalEntries())
        };
    }),
    withMethods((store) => {
        const dotESContentService = inject(DotESContentService);

        // const buildPagesQuery = () => {
        //     return `+working:true +(urlmap:* OR basetype:5) +deleted:false`;
        // };

        return {
            getPages: () => {
                patchState(store, { status: 'loading' });
                dotESContentService
                    .get({
                        itemsPerPage: 40,
                        offset: '0',
                        query: getPagesQuery({ languageId: '1', archived: false }),
                        sortField: 'modDate',
                        sortOrder: 'ASC'
                    })
                    .subscribe(({ jsonObjectView, resultsSize }) => {
                        const pages = jsonObjectView.contentlets;

                        patchState(store, {
                            status: 'loaded',
                            pages,
                            pagination: {
                                currentPage: 1,
                                perPage: 40,
                                totalEntries: resultsSize
                            }
                        });
                    });
            }
        };
    })
);
