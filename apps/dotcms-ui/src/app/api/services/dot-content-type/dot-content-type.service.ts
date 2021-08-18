import { toArray, defaultIfEmpty, map, pluck, flatMap, filter, take } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { StructureTypeView, ContentTypeView } from '@models/contentlet';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Injectable()
export class DotContentTypeService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get a content type by id or variable name
     * @param idOrVar content type's id or variable name
     * @returns Content Type
     */
    getContentType(idOrVar: string): Observable<DotCMSContentType> {
        return this.coreWebService
            .requestView({
                url: `v1/contenttype/id/${idOrVar}`
            })
            .pipe(take(1), pluck('entity'));
    }

    /**
     *Get the content types from the endpoint
     *
     * @returns {Observable<DotCMSContentType[]>}
     * @memberof DotContentTypeService
     */
    getContentTypes(filter = '', page = 40): Observable<DotCMSContentType[]> {
        return this.coreWebService
            .requestView({
                url: `/api/v1/contenttype?filter=${filter}&orderby=modDate&direction=DESC&per_page=${page}`
            })
            .pipe(pluck('entity'));
    }

    /**
     * Gets all content types excluding the RECENT ones
     *
     * @returns Observable<StructureTypeView[]>
     */
    getAllContentTypes(): Observable<StructureTypeView[]> {
        return this.getBaseTypes()
            .pipe(
                flatMap((structures: StructureTypeView[]) => structures),
                filter((structure: StructureTypeView) => !this.isRecentContentType(structure))
            )
            .pipe(toArray());
    }

    /**
     * Get url by id
     *
     * @param string id
     * @returns Observable<string>
     * @memberof ContentletService
     */
    getUrlById(id: string): Observable<string> {
        return this.getBaseTypes().pipe(
            flatMap((structures: StructureTypeView[]) => structures),
            pluck('types'),
            flatMap((contentTypeViews: ContentTypeView[]) => contentTypeViews),
            filter(
                (contentTypeView: ContentTypeView) =>
                    contentTypeView.variable.toLocaleLowerCase() === id
            ),
            pluck('action')
        );
    }

    /**
     * Check is the content types is present in the object
     *
     * @param string id
     * @returns Observable<boolean>
     * @memberof ContentletService
     */
    isContentTypeInMenu(id: string): Observable<boolean> {
        return this.getUrlById(id).pipe(
            map((url: string) => !!url),
            defaultIfEmpty(false)
        );
    }

    private isRecentContentType(type: StructureTypeView): boolean {
        return type.name.startsWith('RECENT');
    }

    private getBaseTypes(): Observable<StructureTypeView[]> {
        return this.coreWebService
            .requestView({
                url: 'v1/contenttype/basetypes'
            })
            .pipe(pluck('entity'));
    }
}
