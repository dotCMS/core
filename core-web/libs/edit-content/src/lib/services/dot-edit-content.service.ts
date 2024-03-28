import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { DotContentTypeService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentlet } from '@dotcms/dotcms-models';

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
    getContentById(id: string): Observable<DotCMSContentlet> {
        return this.http.get(`/api/v1/content/${id}`).pipe(pluck('entity'));
    }

    /**
     * Retrieves the content type by its ID or variable name.
     *
     * @param {string} idOrVar - The identifier or variable name of the content type to retrieve form data for.
     * @return {*}  {Observable<DotCMSContentType>}
     * @memberof DotEditContentService
     */
    getContentType(idOrVar: string): Observable<DotCMSContentType> {
        return this.dotContentTypeService.getContentType(idOrVar);
    }

    /**
     * Retrieves tags based on the provided name.
     * @param name - The name of the tags to retrieve.
     * @returns An Observable that emits an array of tag labels.
     */
    getTags(name: string) {
        const params = new HttpParams().set('name', name);

        return this.http.get('/api/v2/tags', { params }).pipe(
            pluck('entity'),
            map((res) => Object.values(res).map((obj) => obj.label))
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
}
