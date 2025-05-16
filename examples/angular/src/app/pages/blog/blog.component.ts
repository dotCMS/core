import { Component, DestroyRef, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DYNAMIC_COMPONENTS } from '../components';

import { HeaderComponent } from '../components/header/header.component';
import { NavigationComponent } from '../components/navigation/navigation.component';
import { LoadingComponent } from '../components/loading/loading.component';
import { ErrorComponent } from '../components/error/error.component';
import { FooterComponent } from '../components/footer/footer.component';
import { BlogPostComponent } from './blog-post/blog-post.component';
import { DotCMSPageAsset, DotCMSURLContentMap } from '@dotcms/types';
import { EditablePageService } from '../services/editable-page.service';
import { ContentletImage, ExtraContent, FileAsset } from '../../shared/models';

export interface BlogContentlet extends DotCMSURLContentMap {
    blogContent: string;
    image: ContentletImage;
}

export interface BlogPageAsset extends DotCMSPageAsset {
    urlContentMap: BlogContentlet;
}

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
    templateUrl: './blog.component.html',
    styleUrl: './blog.component.css'
})
export class BlogComponent {
    readonly #route = inject(ActivatedRoute);
    readonly #destroyRef = inject(DestroyRef);

    readonly #editablePageService =
        inject<EditablePageService<BlogPageAsset, ExtraContent>>(EditablePageService);

    readonly $context = this.#editablePageService.$context;

    ngOnInit() {
        this.#editablePageService
            .initializePage({
                activateRoute: this.#route,
                destroyRef: this.#destroyRef,
                components: DYNAMIC_COMPONENTS
            })
            .subscribe();
    }
}
