import { Routes } from '@angular/router';

import { DotExperimentsReportsComponent } from '@portlets/dot-experiments/dot-experiments-reports/dot-experiments-reports.component';

export const DotExperimentsReportsRoutes: Routes = [
    {
        path: ':experimentId',
        component: DotExperimentsReportsComponent
    }
];
