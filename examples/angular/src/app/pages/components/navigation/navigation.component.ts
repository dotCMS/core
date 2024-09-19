import { ChangeDetectionStrategy, Component, OnInit, inject, input } from '@angular/core';
import { Params, Router, RouterLink } from '@angular/router';
import { DotcmsNavigationItem } from '@dotcms/angular';

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [RouterLink],
  template: ` <nav>
    <ul class="flex space-x-4 text-white">
      <li>
        <a routerLink="/" [queryParams]="queryParams">Home</a>
      </li>
      @for(item of items(); track $index) {
        <li>
          <a
            [routerLink]="item.href"
            [queryParams]="queryParams"
            target="{{ item.target }}"
            >{{ item.title }}</a
          >
        </li>
      }
    </ul>
  </nav>`,
  styleUrl: './navigation.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavigationComponent implements OnInit{
  items = input.required<DotcmsNavigationItem[]>();

  private readonly router = inject(Router)
  
  protected queryParams!: Params;

  ngOnInit() {
    this.queryParams = this.router.getCurrentNavigation()?.extras.queryParams as Params;
  }
}
