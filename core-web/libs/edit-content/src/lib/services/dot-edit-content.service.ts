import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { DotContentTypeService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DotForm } from '../interfaces/dot-form.interface';

@Injectable()
export class DotEditContentService {
    private readonly dotContentTypeService = inject(DotContentTypeService);
    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private readonly http = inject(HttpClient);

    getContentById(id: string): Observable<DotCMSContentType> {
        return this.http.get(`/api/v1/content/${id}`).pipe(pluck('entity'));
    }

    getContentTypeFormData(idOrVar: string): Observable<DotForm[]> {
        return this.dotContentTypeService.getContentType(idOrVar).pipe(
            map((content: DotCMSContentType) => {
                const mappedData = content.layout.map((row) => {
                    const columns = row.columns?.map((column) => {
                        const fields = column.fields.map((field) => {
                            return {
                                id: field.id,
                                type: field.fieldType,
                                hint: field.hint,
                                label: field.name,
                                required: field.required,
                                regexCheck: field.regexCheck,
                                // url: field.url,
                                variable: field.variable
                            };
                        });

                        return { fields };
                    });

                    return { row: { columns } };
                });

                return mappedData as DotForm[];
            })
        );
    }

    saveContentlet<T>(data: { [key: string]: string }): Observable<T> {
        return this.dotWorkflowActionsFireService.saveContentlet(data);
    }
}
