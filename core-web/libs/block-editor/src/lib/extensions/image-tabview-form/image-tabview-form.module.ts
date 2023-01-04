import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

// Modules
import { PrimengModule } from '../../shared/primeng.module';

// Service
import { DotImageService } from '../image-uploader/services/dot-image/dot-image.service';

// Components
import { DotImageCardSkeletonComponent } from './components/dot-image-card-skeleton/dot-image-card-skeleton.component';
import { DotImageCardComponent } from './components/dot-image-card/dot-image-card.component';
import { DotImageCardListComponent } from './components/dot-image-card-list/dot-image-card-list.component';
import { ImageTabviewFormComponent } from './image-tabview-form.component';

@NgModule({
    imports: [CommonModule, FormsModule, ReactiveFormsModule, PrimengModule],
    declarations: [
        ImageTabviewFormComponent,
        DotImageCardListComponent,
        DotImageCardComponent,
        DotImageCardSkeletonComponent
    ],
    providers: [DotImageService],
    exports: [ImageTabviewFormComponent]
})
export class ImageTabviewFormModule {}
