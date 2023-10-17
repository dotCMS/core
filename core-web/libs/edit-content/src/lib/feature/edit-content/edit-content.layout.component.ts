import { EMPTY } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { switchMap } from 'rxjs/operators';

import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentService } from '../../services/dot-edit-content.service';
@Component({
    selector: 'dot-edit-content-form-layout',
    standalone: true,
    imports: [CommonModule, DotEditContentFormComponent],
    templateUrl: './edit-content.layout.component.html',
    styleUrls: ['./edit-content.layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotEditContentService]
})
export class EditContentLayoutComponent {
    private activatedRoute = inject(ActivatedRoute);

    public contentType = this.activatedRoute.snapshot.params['contentType'];
    public identifier = this.activatedRoute.snapshot.params['id'];

    private readonly dotEditContentService = inject(DotEditContentService);
    isContentSaved = false;
    formData$ = this.identifier
        ? this.dotEditContentService.getContentById(this.identifier).pipe(
              switchMap(({ contentType }) => {
                  if (contentType) {
                      this.contentType = contentType;

                      return this.dotEditContentService.getContentTypeFormData(contentType);
                  } else {
                      return EMPTY;
                  }
              })
          )
        : this.dotEditContentService.getContentTypeFormData(this.contentType);

    /**
     * Saves the contentlet with the given values.
     * @param value - An object containing the key-value pairs of the contentlet to be saved.
     */
    saveContent(value: { [key: string]: string }) {
        this.dotEditContentService
            .saveContentlet({ ...value, inode: this.identifier, contentType: this.contentType })
            .subscribe({
                next: () => {
                    this.isContentSaved = true;
                    setTimeout(() => (this.isContentSaved = false), 3000);
                }
            });
    }
}
