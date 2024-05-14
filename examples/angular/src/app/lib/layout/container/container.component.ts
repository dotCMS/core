import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnChanges,
  computed,
  inject,
  signal,
} from '@angular/core';
import { AsyncPipe, NgComponentOutlet } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { isInsideEditor } from '@dotcms/client';

import { getContainersData } from '../../utils';
import {
  ComponentItem,
  DotcmsPageService,
} from '../../services/dotcms-page/dotcms-page.service';
import { NoComponentComponent } from '../../components/no-component/no-component.component';
import { DotCMSContainer, DotCMSContentlet } from '../../models';

interface DotContainer {
    acceptTypes: string,
    identifier: string,
    maxContentlets: number,
    uuid: string
    variantId?: string,
}

const EMPTY_CONTAINER_EDIT_MODE_STYLES = {
  width: '100%',
  backgroundColor: '#ECF0FD',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  color: '#030E32',
  height: '10rem',
};

@Component({
  selector: 'dotcms-container',
  standalone: true,
  imports: [AsyncPipe, NgComponentOutlet, NoComponentComponent],
  templateUrl: './container.component.html',
  styleUrl: './container.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContainerComponent implements OnChanges {
  @Input({ required: true }) container!: DotCMSContainer;

  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly dotCMSPageService: DotcmsPageService = inject(DotcmsPageService);
  protected readonly NoComponentComponent = NoComponentComponent;
  protected readonly emptyContainerStyles: Record<string, string> = EMPTY_CONTAINER_EDIT_MODE_STYLES;
  protected readonly isInsideEditor = isInsideEditor();

  protected contentlets: DotCMSContentlet[] = [];
  protected componentsMap!: Record<string, ComponentItem>;
  protected dotContainer = signal<DotContainer | null>(null);
  protected dotContainerAsString = computed(() => JSON.stringify(this.dotContainer()));

  ngOnChanges() {
    const { containers } = this.route.snapshot.data['context'].pageAsset;
    const { acceptTypes, maxContentlets, variantId, path, contentlets } = getContainersData(containers, this.container);
    const { identifier, uuid } = this.container;

    this.componentsMap = this.dotCMSPageService.componentMap;
    this.contentlets = contentlets;
    this.dotContainer.set({
      identifier: path ?? identifier,
      acceptTypes,
      maxContentlets,
      variantId,
      uuid
    });
  }
}
