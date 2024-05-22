import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DotcmsLayoutComponent, PageContextService } from '@dotcms/angular';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, DotcmsLayoutComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {

  service = inject(PageContextService);
  ngOnInit(){
    console.log(this.service);
  }
}
