import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pluck, take } from 'rxjs/operators';

import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotRouterService } from '@dotcms/data-access';
import { DotApp, DotAppsSaveData, DotAppsSecret } from '@dotcms/dotcms-models';
import { DotKeyValue } from '@shared/models/dot-key-value-ng/dot-key-value-ng.model';

@Component({
    selector: 'dot-apps-configuration-detail',
    templateUrl: './dot-apps-configuration-detail.component.html',
    styleUrls: ['./dot-apps-configuration-detail.component.scss']
})
export class DotAppsConfigurationDetailComponent implements OnInit {
    apps: DotApp;

    dynamicVariables: DotKeyValue[] = [];
    formData: { [key: string]: string };
    formFields: DotAppsSecret[];
    formValid = false;

    constructor(
        private route: ActivatedRoute,
        private dotRouterService: DotRouterService,
        private dotAppsService: DotAppsService
    ) {}

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

    private getSecrets(
        secrets: DotAppsSecret[],
        includeDinamicFields: boolean = false
    ): DotAppsSecret[] {
        return secrets.filter((secret: DotAppsSecret) => secret.dynamic === includeDinamicFields);
    }

    private transformSecretsToKeyValue(secrets: DotAppsSecret[]): DotKeyValue[] {
        return secrets.map(({ name, hidden, value }: DotAppsSecret) => {
            return { key: name, hidden: hidden, value: value };
        });
    }
}
