import { Observable } from 'rxjs';

import { AsyncPipe, NgForOf, NgIf } from '@angular/common';
import { Component, Input, OnChanges } from '@angular/core';

import { ChipModule } from 'primeng/chip';
import { DialogModule } from 'primeng/dialog';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPageToolsSeoState, DotPageToolsSeoStore } from './store/dot-page-tools-seo.store';

@Component({
    selector: 'dot-page-tools-seo',
    providers: [DotPageToolsService, DotPageToolsSeoStore],
    imports: [NgForOf, AsyncPipe, DialogModule, DotMessagePipe, ChipModule, NgIf],
    templateUrl: './dot-page-tools-seo.component.html',
    styleUrls: ['./dot-page-tools-seo.component.scss'],
    standalone: true
})
export class DotPageToolsSeoComponent implements OnChanges {
    @Input() currentPageUrlParams: DotPageToolUrlParams;
    dialogHeader: string;
    tools$: Observable<DotPageToolsSeoState> = this.dotPageToolsSeoStore.tools$;
    visible = false;

    constructor(private dotPageToolsSeoStore: DotPageToolsSeoStore) {}

    ngOnChanges() {
        this.dotPageToolsSeoStore.getTools(this.currentPageUrlParams);
    }

    public toggleDialog(): void {
        this.visible = !this.visible;
    }
}
