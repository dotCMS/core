import { Route } from '@angular/router';

import { EditEmaEditorComponent } from './components/edit-ema-editor/edit-ema-editor.component';
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
                component: EditEmaEditorComponent
            },
            {
                path: 'rules',
                component: EditEmaEditorComponent
            },
            {
                path: 'experiments',
                component: EditEmaEditorComponent
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
