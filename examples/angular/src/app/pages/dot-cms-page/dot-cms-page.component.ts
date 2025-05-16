import { Component, DestroyRef, OnInit, inject } from '@angular/core';

import { ActivatedRoute } from '@angular/router';

import { ErrorComponent } from '../../shared/components/error/error.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { NavigationComponent } from '../../shared/components/navigation/navigation.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

import { DotCMSLayoutBodyComponent } from '@dotcms/angular/next';
import { DotCMSPageAsset } from '@dotcms/types';
import { ExtraContent, FooterContent } from '../../shared/models';
import { EditablePageService } from '../../services/editable-page.service';
import { DYNAMIC_COMPONENTS } from '../../shared/components';

@Component({
    selector: 'app-dotcms-page',
    standalone: true,
    imports: [
        DotCMSLayoutBodyComponent,
        HeaderComponent,
        NavigationComponent,
        FooterComponent,
        ErrorComponent,
        LoadingComponent
    ],
    providers: [EditablePageService],
    templateUrl: './dot-cms-page.component.html'
})
export class DotCMSPageComponent implements OnInit {
    readonly #editablePageService =
        inject<EditablePageService<DotCMSPageAsset, ExtraContent>>(EditablePageService);

    readonly #destroyRef = inject(DestroyRef);
    readonly #activateRoute = inject(ActivatedRoute);

    readonly $context = this.#editablePageService.$context;

    readonly components = DYNAMIC_COMPONENTS;

    ngOnInit() {
        this.#editablePageService
            .initializePage({
                activateRoute: this.#activateRoute,
                destroyRef: this.#destroyRef
            })
            .subscribe();
    }
}
