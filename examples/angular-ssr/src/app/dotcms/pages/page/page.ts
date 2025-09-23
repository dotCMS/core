import { Component, inject, OnInit, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { DotCMSClient } from '@dotcms/angular';
import { DotCMSComposedPageResponse, DotCMSNavigationItem, DotCMSPageAsset } from '@dotcms/types';
import { filter, from, map, startWith, switchMap } from 'rxjs';

type PageResponse = { content: { navigation: DotCMSNavigationItem } };

@Component({
  standalone: true,
  selector: 'app-page',
  imports: [RouterLink],
  templateUrl: './page.html',
  styleUrl: './page.css',
})
export class Page implements OnInit {
  router = inject(Router);
  currentRoute = signal(this.router.url);

  client = inject(DotCMSClient);
  pageAsset = signal<DotCMSPageAsset | null>(null);
  navigation = signal<DotCMSNavigationItem[]>([]);

  ngOnInit() {

    const pageParams = {
      graphql: {
        content: {
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

    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map((event) => event.url)
      )
      .pipe(startWith(this.currentRoute().split('?')[0]))
      .pipe(switchMap((event) => from(this.client.page.get<PageResponse>(event, pageParams))))
      .pipe(filter(Boolean))
      .subscribe({
        next: (
          response: DotCMSComposedPageResponse<{
            content: { navigation: DotCMSNavigationItem };
          }>
        ) => {
          this.pageAsset.set(response?.pageAsset);
          this.navigation.set(response?.content?.navigation.children || []);
        },
      });
  }
}
