import { Component, inject } from '@angular/core';

import { ErrorComponent } from '../../shared/components/error/error.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { NavigationComponent } from '../../shared/components/navigation/navigation.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

import { DotCMSLayoutBodyComponent } from '@dotcms/angular';
import { DotCMSPageAsset } from '@dotcms/types';
import { EditablePageService } from '../../services/editable-page.service';
import { DYNAMIC_COMPONENTS } from '../../shared/dynamic-components';
import { buildExtraQuery } from '../../shared/queries';
import { ExtraContent } from '../../shared/contentlet.model';
import { JsonPipe } from '@angular/common';

type DotCMSPage = {
  pageAsset: DotCMSPageAsset;
  content: ExtraContent;
};

@Component({
  selector: 'app-dotcms-page',
  imports: [
    JsonPipe,
    DotCMSLayoutBodyComponent,
    HeaderComponent,
    NavigationComponent,
    FooterComponent,
    ErrorComponent,
    LoadingComponent,
  ],
  providers: [EditablePageService],
  templateUrl: './dot-cms-page.component.html',
})
export class DotCMSPageComponent {
  readonly #editablePageService =
    inject<EditablePageService<DotCMSPage>>(EditablePageService);

  readonly components = DYNAMIC_COMPONENTS;

  $pageState = this.#editablePageService.initializePage({
    graphql: {
      ...buildExtraQuery(),
    },
  });
}
