import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotContainerEntity } from '@dotcms/dotcms-models';
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
            .subscribe((container: DotContainerEntity) => {
                if (container?.container) this.containerId = container.container.identifier;
                else this.dotRouterService.goToCreateContainer();
            });
    }
}
