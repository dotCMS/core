import { Component, inject } from '@angular/core';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { NavigationComponent } from '../../shared/components/navigation/navigation.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { ErrorComponent } from '../../shared/components/error/error.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { BlogPostComponent } from './blog-post/blog-post.component';
import {
  BlockEditorContent,
  DotCMSPageAsset,
  DotCMSURLContentMap,
} from '@dotcms/types';
import { EditablePageService } from '../../services/editable-page.service';
import { ContentletImage, ExtraContent } from '../../shared/contentlet.model';
import { buildExtraQuery } from '../../shared/queries';
import { PageState } from '../../shared/models';

export interface BlogContentlet extends DotCMSURLContentMap {
  blogContent: BlockEditorContent;
  image: ContentletImage;
}

export interface BlogPageAsset extends DotCMSPageAsset {
  urlContentMap: BlogContentlet;
}

type BlogPage = {
  pageAsset: BlogPageAsset;
  content: ExtraContent;
};

@Component({
  selector: 'app-blog',
  imports: [
    HeaderComponent,
    NavigationComponent,
    LoadingComponent,
    ErrorComponent,
    FooterComponent,
    BlogPostComponent,
  ],
  providers: [EditablePageService],
  templateUrl: './blog.component.html',
})
export class BlogComponent {
  readonly #editablePageService =
    inject<EditablePageService<BlogPage>>(EditablePageService);

  $pageState = this.#editablePageService.initializePage({
    graphql: {
      ...buildExtraQuery(),
    },
  });
}
