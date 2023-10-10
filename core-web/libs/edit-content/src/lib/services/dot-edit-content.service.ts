import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotContentTypeService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';

@Injectable()
export class DotEditContentService {
    private readonly dotContentTypeService = inject(DotContentTypeService);
    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly http = inject(HttpClient);

    getContentById(id: string): Observable<DotCMSContentType> {
        return this.http.get(`/api/v1/content/${id}`).pipe(pluck('entity'));
    }

    getContentTypeFormData(idOrVar: string): Observable<DotCMSContentTypeLayoutRow[]> {
        return this.dotContentTypeService.getContentType(idOrVar).pipe(pluck('layout'));
    }

    saveContentlet<T>(data: { [key: string]: string }): Observable<T> {
        return this.dotWorkflowActionsFireService.saveContentlet(data);
    }
}
