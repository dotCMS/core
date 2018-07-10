import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { StructureTypeView, ContentTypeView } from '../../../shared/models/contentlet';
import { flatMap, filter, pluck } from '../../../../../node_modules/rxjs/operators';

@Injectable()
export class DotContentletService {
    private MAIN_CONTENT_TYPES = ['CONTENT', 'WIDGET', 'FORM', 'FILEASSET', 'HTMLPAGE'];

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the content types from the endpoint
     *
     * @returns {Observable<StructureTypeView[]>}
     * @memberof ContentletService
     */
    getContentTypes(): Observable<StructureTypeView[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: 'v1/contenttype/basetypes'
            })
            .pluck('entity');
    }

    /**
     * Gets all content types excluding the RECENT ones
     *
     * @returns {Observable<StructureTypeView[]>}
     */
    getAllContentTypes(): Observable<StructureTypeView[]> {
        return this.getContentTypes()
            .pipe(
                flatMap((structures: StructureTypeView[]) => structures),
                filter((structure: StructureTypeView) => !this.isRecentContentType(structure))
            )
            .toArray();
    }

    /**
     * Get url by id
     *
     * @param {string} id
     * @returns {Observable<string>}
     * @memberof ContentletService
     */
    getUrlById(id: string): Observable<string> {
        return this.getContentTypes()
        .pipe(
            flatMap((structures: StructureTypeView[]) => structures),
            pluck('types'),
            flatMap((contentTypeViews: ContentTypeView[]) => contentTypeViews),
            filter((contentTypeView: ContentTypeView) => contentTypeView.variable.toLocaleLowerCase() === id),
            pluck('action')
        );
    }

    /**
     * Check is the content types is present in the object
     *
     * @param {string} id
     * @returns {Observable<boolean>}
     * @memberof ContentletService
     */
    isContentTypeInMenu(id: string): Observable<boolean> {
        return this.getUrlById(id)
            .defaultIfEmpty(false)
            .map((url: string) => !!url);
    }

    private isRecentContentType(type: StructureTypeView): boolean {
        return type.name.startsWith('RECENT');
    }
}
