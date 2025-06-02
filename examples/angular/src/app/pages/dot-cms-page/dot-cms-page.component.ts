import { Component, OnInit, Signal, inject } from '@angular/core';

import { ErrorComponent } from '../../shared/components/error/error.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { NavigationComponent } from '../../shared/components/navigation/navigation.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

import { DotCMSLayoutBodyComponent } from '@dotcms/angular/next';
import { DotCMSPageAsset } from '@dotcms/types';
import { EditablePageService } from '../../services/editable-page.service';
import { DYNAMIC_COMPONENTS } from '../../shared/dynamic-components';
import { BASE_EXTRA_QUERIES } from '../../shared/queries';
import { ExtraContent } from '../../shared/contentlet.model';
import { PageState } from '../../shared/models';

type DotCMSPage = {
    pageAsset: DotCMSPageAsset;
    content: ExtraContent;
};

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
    readonly #editablePageService = inject<EditablePageService<DotCMSPage>>(EditablePageService);

    $pageState!: Signal<PageState<DotCMSPage>>;

    readonly components = DYNAMIC_COMPONENTS;

    ngOnInit() {
        this.$pageState = this.#editablePageService.initializePage({
            graphql: {
                ...BASE_EXTRA_QUERIES
            }
        });
    }
}
