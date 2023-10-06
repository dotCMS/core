import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { switchMap } from 'rxjs/operators';

import { DotFormComponent } from '../../components/dot-form/dot-form.component';
import { DotEditContentService } from '../../dot-edit-content.service';
@Component({
    selector: 'dot-edit-content-form',
    standalone: true,
    imports: [CommonModule, DotFormComponent],
    templateUrl: './form.component.html',
    styleUrls: ['./form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotEditContentService]
})
export class FormComponent {
    private activatedRoute = inject(ActivatedRoute);

    public contentType = this.activatedRoute.snapshot.params['contentType'];
    public identifier = this.activatedRoute.snapshot.params['id'];

    private readonly dotEditContentService = inject(DotEditContentService);
    formData$ = this.identifier
        ? this.dotEditContentService.getContentType(this.identifier).pipe(
              switchMap((res) => {
                  if (res.contentType) {
                      return this.dotEditContentService.getContent(res.contentType);
                  }

                  throw new Error('No content type found');
              })
          )
        : this.dotEditContentService.getContent(this.contentType);

    saveContent(value: { [key: string]: string }) {
        this.dotEditContentService
            .saveContentlet({ ...value, inode: this.identifier, contentType: this.contentType })
            .subscribe({
                // eslint-disable-next-line no-console
                next: (res) => console.log(res)
            });
    }
}
