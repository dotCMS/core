import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotContainer } from '@dotcms/app/shared/models/container/dot-container.model';
import { pluck, take } from 'rxjs/operators';

@Component({
    selector: 'dot-container-create',
    templateUrl: './dot-container-create.component.html',
    styleUrls: ['./dot-container-create.component.scss']
})
export class DotContainerCreateComponent implements OnInit {
    containerId = '';
    constructor(
        private activatedRoute: ActivatedRoute,
        private dotRouterService: DotRouterService
    ) {}

    ngOnInit() {
        this.activatedRoute.data
            .pipe(pluck('container'), take(1))
            .subscribe((container: DotContainer) => {
                if (container) this.containerId = container.identifier;
                else this.dotRouterService.goToPreviousUrl();
            });
    }
}
