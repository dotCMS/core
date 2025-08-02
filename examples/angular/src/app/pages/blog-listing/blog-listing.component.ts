import { Component, OnInit, Signal, computed, effect, inject, signal } from '@angular/core';

import { ErrorComponent } from '../../shared/components/error/error.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { NavigationComponent } from '../../shared/components/navigation/navigation.component';

import { SearchComponent } from './components/search/search.component';
import { BlogCardComponent } from './components/blog-card/blog-card.component';

import { DotCMSPageAsset } from '@dotcms/types';
import { EditablePageService } from '../../services/editable-page.service';
import { DYNAMIC_COMPONENTS } from '../../shared/dynamic-components';
import { BASE_EXTRA_QUERIES } from '../../shared/queries';
import { ExtraContent, Blog } from '../../shared/contentlet.model';
import { PageState } from '../../shared/models';
import { DotCMSClient } from '@dotcms/angular';

// Function to debounce calls
function debounce<T extends (...args: any[]) => void>(
    func: T,
    wait: number
): (...args: Parameters<T>) => void {
    let timeout: ReturnType<typeof setTimeout> | null = null;

    return function (...args: Parameters<T>): void {
        const later = () => {
            timeout = null;
            func(...args);
        };

        if (timeout !== null) {
            clearTimeout(timeout);
        }
        timeout = setTimeout(later, wait);
    };
}

type DotCMSPage = {
    pageAsset: DotCMSPageAsset;
    content: ExtraContent;
};

@Component({
    selector: 'app-blog-listing',
    standalone: true,
    imports: [
        HeaderComponent,
        NavigationComponent,
        ErrorComponent,
        LoadingComponent,
        SearchComponent,
        BlogCardComponent
    ],
    providers: [EditablePageService],
    templateUrl: './blog-listing.component.html'
})
export class BlogListingComponent implements OnInit {
    readonly #editablePageService = inject<EditablePageService<DotCMSPage>>(EditablePageService);
    // Use proper client injection via token
    private readonly client = inject(DotCMSClient);

    $pageState!: Signal<PageState<DotCMSPage>>;
    searchQuery = signal('');
    filteredBlogs = signal<Blog[]>([]);

    readonly components = DYNAMIC_COMPONENTS;

    readonly year = new Date().getFullYear();

    readonly navigation = computed(
        () => this.$pageState().pageResponse?.content?.navigation?.children || []
    );

    ngOnInit() {
        this.$pageState = this.#editablePageService.initializePage({
            graphql: {
                ...BASE_EXTRA_QUERIES
            }
        });
    }

    constructor() {
        effect(
            () => {
                this.updateFilteredBlogs();
            }
        );
    }

    onSearchQueryChange(query: string): void {
        this.searchQuery.set(query);
        this.debouncedSearch(query);
    }

    private debouncedSearch = debounce((query: string) => {
        this.updateFilteredBlogs();
    }, 500);

    private updateFilteredBlogs(): void {
        const query = this.searchQuery();
        const pageState = this.$pageState();

        if (!pageState.pageResponse?.content) {
            return;
        }

        const blogs = pageState.pageResponse.content.blogs || [];

        if (!query.length) {
            this.filteredBlogs.set(blogs);
            return;
        }

        // Use the properly injected DotCMS client to search
        this.client.content
            .getCollection('Blog')
            .limit(3)
            .query((qb: any) => qb.field('title').equals(`${query}*`))
            .sortBy([
                {
                    field: 'Blog.postingDate',
                    order: 'desc'
                }
            ])
            .then((response: any) => {
                this.filteredBlogs.set(response.contentlets);
            })
            .catch(() => {
                // Fallback to client-side filtering if the API call fails
                const filteredResults = blogs.filter((blog) =>
                    blog.title.toLowerCase().startsWith(query.toLowerCase())
                );
                this.filteredBlogs.set(filteredResults);
            });
    }
}
