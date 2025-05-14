import { Component, DestroyRef, OnInit, inject } from '@angular/core';

import { ActivatedRoute } from '@angular/router';

import { ErrorComponent } from './components/error/error.component';
import { LoadingComponent } from './components/loading/loading.component';
import { HeaderComponent } from './components/header/header.component';
import { NavigationComponent } from './components/navigation/navigation.component';
import { FooterComponent } from './components/footer/footer.component';

import { DotCMSLayoutBodyComponent } from '@dotcms/angular/next';
import { DotCMSPageAsset } from '@dotcms/types';
import { FooterContent } from '../shared/models';
import { EditablePageService } from './services/editable-page.service';
import { DYNAMIC_COMPONENTS } from './components';

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
    templateUrl: './pages.component.html',
    styleUrl: './pages.component.css'
})
export class DotCMSPagesComponent implements OnInit {
    readonly #editablePageService =
        inject<EditablePageService<DotCMSPageAsset, FooterContent>>(EditablePageService);

    readonly #destroyRef = inject(DestroyRef);
    readonly #activateRoute = inject(ActivatedRoute);

    readonly $context = this.#editablePageService.$context;

    readonly $components = this.#editablePageService.$components;

    ngOnInit() {
        this.#editablePageService
            .initializePage({
                activateRoute: this.#activateRoute,
                destroyRef: this.#destroyRef,
                components: DYNAMIC_COMPONENTS
            })
            .subscribe();
    }
}
