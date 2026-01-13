import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { pluck, take } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotAppsService } from '@dotcms/data-access';
import { DotApp, DotAppsSaveData, DotAppsSecret } from '@dotcms/dotcms-models';
import { DotKeyValueComponent, DotMessagePipe } from '@dotcms/ui';

import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form/dot-apps-configuration-detail-form.component';

import { DotKeyValue } from '../../../shared/models/dot-key-value-ng/dot-key-value-ng.model';
import { DotAppsConfigurationHeaderComponent } from '../dot-apps-configuration-header/dot-apps-configuration-header.component';

@Component({
    selector: 'dot-apps-configuration-detail',
    templateUrl: './dot-apps-configuration-detail.component.html',
    styleUrls: ['./dot-apps-configuration-detail.component.scss'],
    imports: [
        ButtonModule,
        DotKeyValueComponent,
        DotAppsConfigurationHeaderComponent,
        DotAppsConfigurationDetailFormComponent,
        DotMessagePipe
    ]
})
export class DotAppsConfigurationDetailComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private dotRouterService = inject(DotRouterService);
    private dotAppsService = inject(DotAppsService);

    apps: DotApp;

    dynamicVariables: DotKeyValue[] = [];
    formData: { [key: string]: string };
    formFields: DotAppsSecret[];
    formValid = false;

    ngOnInit() {
        this.route.data.pipe(pluck('data'), take(1)).subscribe((app: DotApp) => {
            this.apps = app;
            this.formFields = this.getSecrets(app.sites[0].secrets);
            this.dynamicVariables = this.transformSecretsToKeyValue(
                this.getSecrets(app.sites[0].secrets, true)
            );
        });
    }

    /**
     * Saves the secrets configuration data of the app
     *
     * @memberof DotAppsConfigurationDetailComponent
     */
    onSubmit(): void {
        this.dotAppsService
            .saveSiteConfiguration(
                this.apps.key,
                this.apps.sites[0].id,
                this.getTransformedFormData()
            )
            .pipe(take(1))
            .subscribe(() => {
                this.goToApps(this.apps.key);
            });
    }

    /**
     * Redirects to app configuration listing page
     *
     * @param string key
     * @memberof DotAppsConfigurationDetailComponent
     */
    goToApps(key: string): void {
        this.dotRouterService.goToAppsConfiguration(key);
    }

    /**
     *
     *
     * @param {DotKeyValue[]} variable
     * @memberof DotAppsConfigurationDetailComponent
     */
    updateVariables(variables: DotKeyValue[]): void {
        this.dynamicVariables = [...variables];
    }

    private getTransformedFormData(): DotAppsSaveData {
        const params = {};
        for (const key of Object.keys(this.formData)) {
            params[key] = {
                hidden: this.formFields.filter((item) => item.name === key)[0].hidden,
                value: this.formData[key].toString()
            };
        }

        this.dynamicVariables.forEach((item: DotKeyValue) => {
            params[item.key] = {
                hidden: item.hidden || false,
                value: item.value.toString()
            };
        });

        return params;
    }

    private getSecrets(secrets: DotAppsSecret[], includeDinamicFields = false): DotAppsSecret[] {
        return secrets.filter((secret: DotAppsSecret) => secret.dynamic === includeDinamicFields);
    }

    private transformSecretsToKeyValue(secrets: DotAppsSecret[]): DotKeyValue[] {
        return secrets.map(({ name, hidden, value }: DotAppsSecret) => {
            return { key: name, hidden: hidden, value: value };
        });
    }
}
