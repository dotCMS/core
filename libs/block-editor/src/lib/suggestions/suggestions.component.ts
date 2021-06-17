import {
    Component,
    ComponentFactoryResolver,
    Input,
    OnInit,
    ViewChild,
    ViewContainerRef
} from '@angular/core';
import { Editor, Range } from '@tiptap/core';
import Suggestion from '@tiptap/suggestion';
import { SuggestionListComponent } from '../suggestion-list/suggestion-list.component';
import tippy from 'tippy.js';

// theses needs to be Angular Services
import { DotContentLetService } from '../services/dotContentLet.service';
import { SuggestionsService } from '../services/suggestions.service';

@Component({
    selector: 'dotcms-suggestions',
    templateUrl: './suggestions.component.html',
    styleUrls: ['./suggestions.component.scss']
})
export class SuggestionsComponent implements OnInit {
    @ViewChild('container', { read: ViewContainerRef })
    container!: ViewContainerRef;

    popup;

    @Input() editor!: Editor;

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private suggestionsService: SuggestionsService
    ) {}

    ngOnInit(): void {
        this.editor.registerPlugin(
            Suggestion({
                editor: this.editor,
                char: '/c',
                allowSpaces: true,
                startOfLine: true,
                command: ({
                    editor,
                    range,
                    props
                }: {
                    editor: Editor;
                    range: Range;
                    props: any;
                }) => {
                    editor
                        .chain()
                        .focus()
                        .insertContentAt(range, {
                            type: 'dotContent',
                            attrs: {
                                data: props
                            }
                        })
                        .run();
                },
                allow: ({ editor, range }) => {
                    return editor.can().insertContentAt(range, { type: 'dotContentAutoComplete' });
                },
                items: () => [],
                render: this.render.bind(this)
            })
        );
    }

    private render(): unknown {
        return {
            onStart: (props) => {
                console.log('onStart', props);

                this.suggestionsService.getContentTypes().subscribe((items) => {
                    const dynamicComponentFactory = this.componentFactoryResolver.resolveComponentFactory(
                        SuggestionListComponent
                    );

                    // add the component to the view
                    const componentRef = this.container.createComponent(dynamicComponentFactory);
                    componentRef.instance.items = items.map((item) => {
                        return {
                            label: item['name'],
                            icon: 'pi pi-fw pi-plus',
                            command: () => {
                                this.suggestionsService
                                    .getContentlets(item['variable'])
                                    .subscribe((contentlets) => {
                                        const newElements = contentlets.map((contentlet) => {
                                            return {
                                                label: contentlet['title'],
                                                icon: 'pi pi-fw pi-plus',
                                                command: () => {
                                                    props.command(contentlet);
                                                }
                                            };
                                        });
                                        componentRef.instance.items = newElements;
                                        componentRef.changeDetectorRef.detectChanges();
                                    });
                            }
                        };
                    });
                    componentRef.changeDetectorRef.detectChanges();

                    this.popup = tippy(this.editor.view.dom, {
                        appendTo: document.body,
                        // content: componentRef.location.nativeElement.querySelector('.p-menu'),
                        content: componentRef.location.nativeElement,
                        placement: 'auto-start',
                        getReferenceClientRect: props.clientRect,
                        showOnCreate: true,
                        interactive: true,
                        trigger: 'manual'
                    });
                });
            },
            onUpdate: (props) => {
                console.log('onUpdate: ', props);
            },
            onKeyDown(props) {
                console.log('onKeyDown', props);
            },
            onExit: () => {
                console.log('onExit');
                this.popup.destroy();
            }
        };
    }
}
