import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { TabViewModule } from 'primeng/tabview';

import { map, take } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotContainerEntity } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContainerHistoryComponent } from './dot-container-history/dot-container-history.component';
import { DotContainerPermissionsComponent } from './dot-container-permissions/dot-container-permissions.component';
import { DotContainerPropertiesComponent } from './dot-container-properties/dot-container-properties.component';

import { DotGlobalMessageComponent } from '../../../view/components/_common/dot-global-message/dot-global-message.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@Component({
    selector: 'dot-container-create',
    templateUrl: './dot-container-create.component.html',
    styleUrls: ['./dot-container-create.component.scss'],
    imports: [
        DotPortletBaseComponent,
        TabViewModule,
        DotMessagePipe,
        DotContainerPropertiesComponent,
        DotContainerPermissionsComponent,
        DotContainerHistoryComponent,
        DotGlobalMessageComponent
    ]
})
export class DotContainerCreateComponent implements OnInit {
    private activatedRoute = inject(ActivatedRoute);
    private dotRouterService = inject(DotRouterService);

    containerId = '';

    ngOnInit() {
        this.activatedRoute.data
            .pipe(
                map((x: any) => x?.container),
                take(1)
            )
            .subscribe((container: DotContainerEntity) => {
                if (container?.container) this.containerId = container.container.identifier;
                else this.dotRouterService.goToCreateContainer();
            });
    }
}
