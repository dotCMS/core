import { AsyncPipe, NgComponentOutlet } from '@angular/common';

import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { getContainersData } from '../../utils';
import { ActivatedRoute } from '@angular/router';
import {
  ComponentItem,
  DotcmsPageService,
} from '../../services/dotcms-page/dotcms-page.service';
import { NoComponentComponent } from '../../components/no-component/no-component.component';

@Component({
  selector: 'dotcms-container',
  standalone: true,
  imports: [AsyncPipe, NgComponentOutlet, NoComponentComponent],
  template: `
    @if(contentlets.length){
        @for (contentlet of contentlets; track $index;) {
            <div data-testid="dot-contentlet" data-dot-object="contentlet">
            <ng-container
                *ngComponentOutlet="
                (componentsMap[contentlet?.contentType]?.component | async) ||
                    NoComponentComponent;
                inputs: { contentlet }
                "
            ></ng-container>
            </div>
        }
    } @else {
        This container is empty.
    }
  `,
  styleUrl: './container.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContainerComponent implements OnInit {
  @Input({ required: true }) container!: any;

  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly dotCMSPageService: DotcmsPageService = inject(DotcmsPageService);
  protected readonly NoComponentComponent = NoComponentComponent;

  protected contentlets: any[] = [];
  protected componentsMap!: Record<string, ComponentItem>;

  ngOnInit() {
    const { containers } = this.route.snapshot.data['context'].page;
    const { contentlets } = getContainersData(containers, this.container);
    this.componentsMap = this.dotCMSPageService.componentMap;
    this.contentlets = contentlets;
  }
}
