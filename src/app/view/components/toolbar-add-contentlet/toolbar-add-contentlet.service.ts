import { Injectable } from '@angular/core';
import { DotContentletService } from '../../../api/services/dot-contentlet.service';
import { Subject } from 'rxjs/Subject';
import { StructureTypeView } from '../../../shared/models/contentlet';
import { DotcmsEventsService } from 'dotcms-js/dotcms-js';

@Injectable()
export class ToolbarAddContenletService {
    main$: Subject<StructureTypeView[]> = new Subject();
    more$: Subject<StructureTypeView[]> = new Subject();
    recent$: Subject<StructureTypeView[]> = new Subject();

    constructor(
        private contentletService: DotContentletService,
        private dotcmsEventsService: DotcmsEventsService
    ) {
        this.loadData();
        dotcmsEventsService
            .subscribeToEvents([
                'SAVE_BASE_CONTENT_TYPE',
                'UPDATE_BASE_CONTENT_TYPE',
                'DELETE_BASE_CONTENT_TYPE'
            ])
            .subscribe(() => {
                this.reloadContentlets();
            });
    }

    private loadData() {
        this.contentletService
            .getMainContentTypes()
            .subscribe((structureTypeViews: StructureTypeView[]) => {
                this.main$.next(structureTypeViews);
            });

        this.contentletService
            .getMoreContentTypes()
            .subscribe((structureTypeViews: StructureTypeView[]) => {
                this.more$.next(structureTypeViews);
            });

        this.contentletService
            .getRecentContentTypes()
            .subscribe((structureTypeViews: StructureTypeView[]) => {
                this.recent$.next(structureTypeViews);
            });
    }

    private reloadContentlets() {
        this.contentletService.reloadContentTypes().subscribe(() => {
            this.loadData();
        });
    }
}
