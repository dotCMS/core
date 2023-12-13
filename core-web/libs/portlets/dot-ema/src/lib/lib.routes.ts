import { Route } from '@angular/router';

import { DotEmaComponent } from './dot-ema-shell/dot-ema.component';
import { EditEmaEditorComponent } from './edit-ema-editor/edit-ema-editor.component';
import { EditEmaExperimentsComponent } from './edit-ema-experiments/edit-ema-experiments.component';
import { EditEmaLayoutComponent } from './edit-ema-layout/edit-ema-layout.component';
import { EditEmaRulesComponent } from './edit-ema-rules/edit-ema-rules.component';

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
