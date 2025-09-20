import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';

import { DotCMSClient } from '@dotcms/angular';

@Component({
    imports: [RouterModule],
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrl: './app.component.scss'
})
export class AppComponent {
    private readonly client = inject(DotCMSClient);

    ngOnInit() {
        this.client.page.get('/').then(({ pageAsset }) => {
            console.log(pageAsset);
        });
    }
}
