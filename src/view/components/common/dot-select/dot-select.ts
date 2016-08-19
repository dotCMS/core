import {Component, Input, Output, EventEmitter, ElementRef, ViewChild, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {MdIcon} from '@angular2-material/icon/icon';

@Component({
    directives: [MdIcon],
    host: {
        '(document:click)': 'handleClick($event)',
    },
    moduleId: __moduleName,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotSelect),
            multi: true
        }
    ],
    selector: 'dot-select',
    styleUrls: ['dot-select.css'],
    templateUrl: ['dot-select.html']
})
export class DotSelect implements ControlValueAccessor {
    private elementRef;
    private isHover:boolean = false;
    private isOpen:boolean = false;
    private options:Array<DotOption> = [];
    private selectedText:string;

    propagateChange = (_: any) => {};

    @Input('value') _innerValue;
    @Input() disabled;

    @Output() change = new EventEmitter();

    @ViewChild('selectOptions') selectOptions: ElementRef;
    @ViewChild('dotSelect') dotSelect: ElementRef;

    constructor(myElement: ElementRef) {
        this.elementRef = myElement;
    }

    set innerValue(val) {
        this._innerValue = val;
        this.selectedText = this.options.filter((option) => option.value === val)[0].text || '';
        this.propagateChange(val);
    }

    get innerValue() {
        return this._innerValue;
    }

    ngOnInit() {
        this.dotSelect.nativeElement.style.width = this.selectOptions.nativeElement.offsetWidth + 'px';
        this.disabled = typeof(this.disabled) === 'string' ? true : this.disabled;
    }

    addOption(option:DotOption):void {
        this.options.push(option);
    }

    openClose():void {
        if (this.disabled) {
            return;
        }
        this.isOpen = !this.isOpen;
    }

    setValue(value:string):void {
        this.innerValue = value;
    }

    setOption(option):void {
        this.openClose();
        this.innerValue = option.value;

        this.options.forEach((o) => {
            o.selected = o.value === option.value;
        })

        this.change.emit(option);
    }

    toggleHoverState() {
        this.isHover = !this.isHover;
    }

    // Implementing ControlValueAccessor methods
    writeValue(value: any) {
        if (value) {
            this.innerValue = value;
        }
    }

    registerOnChange(fn) {
        this.propagateChange = fn;
    }

    registerOnTouched() {}

    // TODO: need to make this global because will be use in a lot of places
    private handleClick($event) {
        let clickedComponent = $event.target;
        let inside = false;
        do {
            if (clickedComponent === this.elementRef.nativeElement) {
                inside = true;
            }
            clickedComponent = clickedComponent.parentNode;
        } while (clickedComponent);

        if (!inside) {
            this.isOpen = false;
        }
    }

}

@Component({
    moduleId: __moduleName,
    selector: 'dot-option',
    templateUrl: ['dot-option.html'],
    styleUrls: ['dot-option.css'],
    host: {
        '(click)': 'onClick($event)',
    }
})

export class DotOption {
    @Input() value:string;
    @Input() selected:boolean;
    public node:ElementRef;
    public text:string;

    constructor(private select: DotSelect, myElement: ElementRef) {
        this.node = myElement;
    }

    ngAfterViewInit() {
        this.select.addOption(this);
        this.text = this.node.nativeElement.firstChild.innerHTML.trim();
    }

    onClick() {
        console.log('this.value', this.value);
        this.select.setOption({
            value: this.value,
            text: this.node.nativeElement.firstChild.innerHTML.trim()
        });
    }
}