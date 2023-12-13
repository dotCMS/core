import { Route } from '@angular/router';

import { EditEmaEditorComponent } from './components/edit-ema-editor/edit-ema-editor.component';
import { EditEmaExperimentsComponent } from './components/edit-ema-experiments/edit-ema-experiments.component';
import { EditEmaLayoutComponent } from './components/edit-ema-layout/edit-ema-layout.component';
import { EditEmaRulesComponent } from './components/edit-ema-rules/edit-ema-rules.component';
import { DotEmaComponent } from './feature/dot-ema.component';

export const DotEmaRoutes: Route[] = [
    {
        path: '',
        component: DotEmaComponent,

        children: [
            {
                path: 'content',
                component: EditEmaEditorComponent
            },
            {
                path: 'layout',
                component: EditEmaLayoutComponent
            },
            {
                path: 'rules',
                component: EditEmaRulesComponent
            },
            {
                path: 'experiments',
                component: EditEmaExperimentsComponent
            },
            {
                path: '**',
                redirectTo: 'content',
                pathMatch: 'full'
            },
            {
                path: '',
                redirectTo: 'content',
                pathMatch: 'full'
            }
        ]
    }
];
