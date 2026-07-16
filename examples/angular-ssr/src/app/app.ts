import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DotCMSComposedPageResponse, DotCMSNavigationItem } from '@dotcms/types';
import { NavigationComponent } from './components/navigation/navigation.component';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { Blog, Destination, FileAsset } from './dotcms/types/contentlet.model';

type PageResponse = {
  content: {
    navigation: DotCMSNavigationItem;
    logoImage: FileAsset[];
    blogs: Blog[];
    destinations: Destination[];
  };
};

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, NavigationComponent, FooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly http = inject(HttpClient);

  content = signal<PageResponse['content'] | undefined>(undefined);

  ngOnInit() {
    const pageParams = {
      graphql: {
        content: {
          blogs: `
            search(query: "+contenttype:Blog +live:true", limit: 10) {
              title
              identifier
              ... on Blog {
                inode
                image {
                  fileName
                  fileAsset {
                    versionPath
                  }
                }
                urlMap
                modDate
                urlTitle
                teaser
                author {
                  firstName
                  lastName
                  inode
                }
              }
            }
          `,
          destinations: `
            search(query: "+contenttype:Destination +live:true", limit: 10) {
              title
              identifier
              ... on Destination {
                inode
                image {
                  fileName
                  fileAsset {
                    versionPath
                  }
                }
                urlMap
                modDate
                url
              }
            }
          `,
          logoImage: `
            FileAssetCollection(query: "+title:logo.png") {
              fileAsset {
                versionPath
              }
            }
          `,
          navigation: `
            DotNavigation(uri: "/", depth: 2) {
              children {
                folder
                href
                title
              }
            }
          `,
        },
      },
    };

    this.http
      .post<DotCMSComposedPageResponse<PageResponse>>('/data/page', {
        url: '/',
        params: pageParams,
      })
      .subscribe({
        next: (response) => {
          this.content.set(response?.content);
        },
        // Without an error handler, a failed `/data/page` call (e.g. a missing
        // DOTCMS_AUTH_TOKEN) throws during SSR and crashes the render, which
        // then surfaces as a cryptic "index.csr.html does not exist" fallback.
        // Log the real cause and degrade gracefully to the page shell instead.
        error: (error) => {
          console.error('Failed to load page data from /data/page:', error?.message ?? error);
          this.content.set(undefined);
        },
      });
  }
}
