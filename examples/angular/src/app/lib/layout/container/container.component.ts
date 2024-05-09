import { NgComponentOutlet } from '@angular/common';

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'dotcms-container',
  standalone: true,
  imports: [NgComponentOutlet],
  template: `
    @if(contentlets.length){
        @for (contentlet of contentlets; track $index) {
            <div
                data-testid="dot-contentlet"
                data-dot-object="contentlet">
                <!-- <ng-container *ngComponentOutlet="(COMPONENTS[contentlet.contentType]  | async) || NoComponent; inputs: {contentlet}" ></ng-container> -->
            </div>
        }
    } @else {
        This container is empty.
    }
  `,
  styleUrl: './container.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContainerComponent {
  @Input({ required: true }) container!: any;

  protected readonly contentlets = [];
}
