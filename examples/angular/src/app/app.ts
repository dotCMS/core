import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DotCMSNavigationItem } from '@dotcms/types';
import { NavigationComponent } from './components/navigation/navigation.component';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { Blog, Destination, FileAsset } from './dotcms/types/contentlet.model';
import { DotCMSClient } from '@dotcms/angular';

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
  private readonly client = inject(DotCMSClient);

  content = signal<PageResponse['content'] | undefined>(undefined);

  ngOnInit() {
    const pageParams = {
      graphql: {
        content: {
          blogs: `
            search(query: "+contenttype:Blog +live:true", limit: 3) {
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
            search(query: "+contenttype:Destination +live:true", limit: 3) {
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

    this.client.page.get<PageResponse>('/', pageParams)
      .then((response) => {
        this.content.set(response?.content);
      });
  }
}
