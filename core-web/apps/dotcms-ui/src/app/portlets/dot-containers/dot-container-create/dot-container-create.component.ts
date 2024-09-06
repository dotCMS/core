import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pluck, take } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotContainerEntity } from '@dotcms/dotcms-models';

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
            .subscribe((container: DotContainerEntity) => {
                if (container?.container) this.containerId = container.container.identifier;
                else this.dotRouterService.goToCreateContainer();
            });
    }
}
