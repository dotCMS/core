import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DotCMSComposedPageResponse, DotCMSNavigationItem } from '@dotcms/types';
import { NavigationComponent } from './components/navigation/navigation.component';
import { HeaderComponent } from './components/header/header.component';

type PageResponse = { content: { navigation: DotCMSNavigationItem } };

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, NavigationComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly http = inject(HttpClient);

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

    this.http
      .post<DotCMSComposedPageResponse<PageResponse>>('/api/page', { url: '/', params: pageParams })
      .subscribe((response) => {
        this.navigation.set(response?.content?.navigation.children || []);
      });
  }
}
