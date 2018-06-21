import { CommonModule } from '@angular/common';
import { NGFACES_MODULES } from '../../../../modules';
import { NgModule } from '@angular/core';
import { PatternLibraryComponent } from './pattern-library.component';
import { Routes, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

const routes: Routes = [
    {
        component: PatternLibraryComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ...NGFACES_MODULES,
        RouterModule.forChild(routes)
    ],
    declarations: [PatternLibraryComponent]
})
export class PatternLibraryModule {}
