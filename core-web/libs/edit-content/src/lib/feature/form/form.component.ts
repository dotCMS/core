import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'dot-edit-content-form',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './form.component.html',
    styleUrls: ['./form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class FormComponent {
    private activatedRoute = inject(ActivatedRoute);

    public contentType = this.activatedRoute.snapshot.params['contentType'];
    public identifier = this.activatedRoute.snapshot.params['id'];
}
