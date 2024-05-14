import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import { isInsideEditor } from '@dotcms/client';

import { DotCMSPageAsset } from '../../models';

export interface ComponentItem {
  component: Promise<any>;
  module?: Promise<any>;
}

export interface DotCMSPageContext extends DotCMSPageAsset {
  isInsideEditor: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class PageContextService {

  private componentsMap!: Record<string, ComponentItem>;
  private pageContext = new BehaviorSubject<DotCMSPageContext | null>(null);
  readonly pageContext$ = this.pageContext.asObservable() as Observable<DotCMSPageContext>;

  set componentMap(components: Record<string, ComponentItem>) {
    this.componentsMap = components;
  }

  get componentMap(): Record<string, ComponentItem> {
    return this.componentsMap;
  }

  /**
   * Set the context
   *
   * @protected
   * @param {DotCMSPageAsset} value
   * @memberof DotcmsContextService
   */
  setContext(value: DotCMSPageAsset) {
    this.pageContext.next({
      ...value,
      isInsideEditor: isInsideEditor()
    });
  }
}
