import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { Observable } from 'rxjs';
import { DotTemplate } from '@portlets/dot-edit-page/shared/models';
import { take } from 'rxjs/operators';

@Injectable({
    providedIn: 'root'
})
export class DotTemplateListResolver implements Resolve<DotTemplate[]> {
    constructor(private dotTemplatesService: DotTemplatesService) {}

    resolve(): Observable<DotTemplate[]> {
        // TODO: Need to update to get the fist page.
        return this.dotTemplatesService.get().pipe(take(1));
    }
}
