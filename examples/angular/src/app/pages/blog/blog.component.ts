import { Component, inject, OnInit, Signal } from '@angular/core';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { NavigationComponent } from '../../shared/components/navigation/navigation.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { ErrorComponent } from '../../shared/components/error/error.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { BlogPostComponent } from './blog-post/blog-post.component';
import { DotCMSPageAsset, DotCMSURLContentMap } from '@dotcms/types';
import { EditablePageService } from '../../services/editable-page.service';
import { ContentletImage, ExtraContent } from '../../shared/contentlet.model';
import { BASE_EXTRA_QUERIES } from '../../shared/queries';
import { PageState } from '../../shared/models';

export interface BlogContentlet extends DotCMSURLContentMap {
    blogContent: string;
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
    standalone: true,
    imports: [
        HeaderComponent,
        NavigationComponent,
        LoadingComponent,
        ErrorComponent,
        FooterComponent,
        BlogPostComponent
    ],
    providers: [EditablePageService],
    templateUrl: './blog.component.html'
})
export class BlogComponent implements OnInit {
    readonly #editablePageService = inject<EditablePageService<BlogPage>>(EditablePageService);

    $pageState!: Signal<PageState<BlogPage>>;

    ngOnInit() {
        this.$pageState = this.#editablePageService.initializePage({
            graphql: {
                ...BASE_EXTRA_QUERIES
            }
        });
    }
}
