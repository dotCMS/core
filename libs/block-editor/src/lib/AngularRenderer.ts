import {
  ApplicationRef, ComponentFactoryResolver, ComponentRef,
  ElementRef, Injector, Type
} from "@angular/core";

export class AngularRenderer<C> {
  private applicationRef: ApplicationRef
  private componentRef: ComponentRef<C>

  constructor(component: Type<C>, injector: Injector) {
    this.applicationRef = injector.get(ApplicationRef);

    const componentFactoryResolver = injector.get(ComponentFactoryResolver);
    const factory = componentFactoryResolver.resolveComponentFactory(component);

    this.componentRef = factory.create(injector, []);

    // Attach to the view so that the change detector knows to run
    this.applicationRef.attachView(this.componentRef.hostView);
  }

  get instance(): C {
    return this.componentRef.instance;
  }

  get elementRef(): ElementRef {
    return this.componentRef.injector.get(ElementRef);
  }

  get dom(): HTMLElement {
    return this.elementRef.nativeElement;
  }

  detectChanges():void {
    this.componentRef.changeDetectorRef.detectChanges();
  }

  destroy(): void {
    this.applicationRef.detachView(this.componentRef.hostView);
  }
}
