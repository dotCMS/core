import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
    standalone: true,
    imports: [RouterModule],
    selector: 'app-root',
    template: '<router-outlet />'
})
export class AppComponent {
    title = 'app-ssr';
}
