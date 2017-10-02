import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { RequestMethod } from '@angular/http';
import { StructureType, StructureTypeView, ContentTypeView } from '../../shared/models/contentlet';

@Injectable()
export class DotContentletService {
    private MAIN_CONTENT_TYPES = ['CONTENT', 'WIDGET', 'FORM', 'FILEASSET', 'HTMLPAGE'];
    private structureTypeView: StructureTypeView[];
    private types$: Observable<StructureTypeView[]>;

    constructor(private coreWebService: CoreWebService) {
    }

    /**
     * Get the content types from the endpoint
     *
     * @returns {Observable<StructureTypeView[]>}
     * @memberof ContentletService
     */
    getContentTypes(): Observable<StructureTypeView[]> {
        if (!this.types$) {
            this.types$ = this.coreWebService
                .requestView({
                    method: RequestMethod.Get,
                    url: 'v1/contenttype/basetypes'
                })
                .publishLast()
                .refCount()
                .pluck('entity');
        }
        return this.types$;
    }

    /**
     * Get the main content types
     *
     * @returns {Observable<StructureTypeView[]>}
     * @memberof ContentletService
     */
    getMainContentTypes(): Observable<StructureTypeView[]> {
        return this.getContentTypes()
            .flatMap((structures: StructureTypeView[]) => structures)
            .filter((structure: StructureTypeView) => this.isMainContentType(structure))
            .toArray();
    }

    /**
     * Get the extra content types
     *
     * @returns {Observable<StructureTypeView[]>}
     * @memberof ContentletService
     */
    getMoreContentTypes(): Observable<StructureTypeView[]> {
        return this.getContentTypes()
            .flatMap((structures: StructureTypeView[]) => structures)
            .filter((structure: StructureTypeView) => this.isMoreContentType(structure))
            .toArray();
    }

    /**
     * Get the recent content types
     *
     * @returns {Observable<StructureTypeView[]>}
     * @memberof ContentletService
     */
    getRecentContentTypes(): Observable<StructureTypeView[]> {
        return this.getContentTypes()
            .flatMap((structures: StructureTypeView[]) => structures)
            .filter((structure: StructureTypeView) => this.isRecentContentType(structure))
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
            .flatMap((structures: StructureTypeView[]) => structures)
            .pluck('types')
            .flatMap((contentTypeViews: ContentTypeView[]) => contentTypeViews)
            .filter((contentTypeView: ContentTypeView) => contentTypeView.variable.toLocaleLowerCase() === id)
            .pluck('action');
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

    /**
     * Clear cache and return new content types.
     *
     * @returns {Observable<StructureTypeView[]>}
     * @memberof ContentletService
     */
    reloadContentTypes(): Observable<StructureTypeView[]> {
        this.types$ = null;
        return this.getContentTypes();
    }

    private isRecentContentType(type: StructureTypeView): boolean {
        return type.name.startsWith('RECENT');
    }

    private isMainContentType(type: StructureTypeView): boolean {
        return this.MAIN_CONTENT_TYPES.includes(type.name);
    }

    private isMoreContentType(type: StructureTypeView): boolean {
        return !this.isRecentContentType(type) && !this.isMainContentType(type);
    }
}
