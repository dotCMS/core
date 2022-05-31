export class CwAction {
    key: string;
    target: any;

    constructor(key: string, target: any) {
        this.key = key;
        this.target = target;
    }
}

export class AddAction extends CwAction {
    constructor(key: string, target: any) {
        super(key, target);
    }
}
