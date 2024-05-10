import { Injectable } from '@angular/core';

export interface ComponentItem {
  component: Promise<any>;
  module?: Promise<any>;
}

@Injectable({
  providedIn: 'root'
})
export class DotcmsPageService {

  set componentMap(components: Record<string, ComponentItem>) {
    this.componentsMap = components;
  }

  get componentMap(): Record<string, ComponentItem> {
    return this.componentsMap;
  }

  private componentsMap!: Record<string, ComponentItem>;
}
