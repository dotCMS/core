import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pluck, take } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotContainerEntity } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-container-create',
    templateUrl: './dot-container-create.component.html',
    styleUrls: ['./dot-container-create.component.scss'],
    standalone: false
})
export class DotContainerCreateComponent implements OnInit {
    private activatedRoute = inject(ActivatedRoute);
    private dotRouterService = inject(DotRouterService);

    containerId = '';

    ngOnInit() {
        this.activatedRoute.data
            .pipe(pluck('container'), take(1))
            .subscribe((container: DotContainerEntity) => {
                if (container?.container) this.containerId = container.container.identifier;
                else this.dotRouterService.goToCreateContainer();
            });
    }
}
