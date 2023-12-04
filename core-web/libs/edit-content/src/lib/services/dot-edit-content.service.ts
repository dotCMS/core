import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { DotContentTypeService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutTab
} from '@dotcms/dotcms-models';

import { TAB_FIELD_CLAZZ } from '../models/dot-edit-content-field.constant';
import { EditContentFormData } from '../models/dot-edit-content-form.interface';

@Injectable()
export class DotEditContentService {
    private readonly dotContentTypeService = inject(DotContentTypeService);
    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly http = inject(HttpClient);

    /**
     * Retrieves the content by its ID.
     * @param id - The ID of the content to retrieve.
     * @returns An observable of the DotCMSContentType object.
     */
    getContentById(id: string): Observable<DotCMSContentType> {
        return this.http.get(`/api/v1/content/${id}`).pipe(pluck('entity'));
    }

    /**
     * Returns an Observable of an array of DotCMSContentTypeLayoutRow objects representing the form data for a given content type.
     * @param idOrVar - The identifier or variable name of the content type to retrieve form data for.
     * @returns An Observable of an array of DotCMSContentTypeLayoutRow objects representing the form data for the given content type.
     */
    getContentTypeFormData(idOrVar: string): Observable<EditContentFormData> {
        return this.dotContentTypeService.getContentType(idOrVar).pipe(
            map(({ layout, fields }): EditContentFormData => {
                const tabs = this.getLayoutTabs(layout);

                return {
                    tabs,
                    layout,
                    fields
                };
            })
        );
    }

    /**
     * Saves a contentlet with the provided data.
     * @param data An object containing key-value pairs of data to be saved.
     * @returns An observable that emits the saved contentlet.
     * The type of the emitted contentlet is determined by the generic type parameter.
     */
    saveContentlet<T>(data: { [key: string]: string }): Observable<T> {
        return this.dotWorkflowActionsFireService.saveContentlet(data);
    }

    /**
     * Publishes a contentlet with the provided data.
     *
     * @private
     * @param {DotCMSContentTypeLayoutRow[]} layout
     * @return {*}  {DotCMSContentTypeLayoutTab[]}
     * @memberof DotEditContentService
     */
    private getLayoutTabs(layout: DotCMSContentTypeLayoutRow[]): DotCMSContentTypeLayoutTab[] {
        const initialTab = [
            {
                title: 'Content', //
                layout: []
            }
        ];

        const tabs = layout.reduce((acc, row) => {
            const { clazz, name } = row.divider;
            if (clazz === TAB_FIELD_CLAZZ) {
                acc.push({
                    title: name,
                    layout: []
                });
            } else {
                acc[acc.length - 1].layout.push(row);
            }

            return acc;
        }, initialTab);

        return tabs;
    }
}
