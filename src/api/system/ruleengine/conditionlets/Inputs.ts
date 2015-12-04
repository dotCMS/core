import {Inject, EventEmitter} from 'angular2/angular2';

import {EntitySnapshot} from "../../../persistence/EntityBase";
import {EntityMeta} from "../../../persistence/EntityBase";
import {ApiRoot} from "../../../persistence/ApiRoot";
import {CwModel} from "../../../util/CwModel";

let noop = (...arg:any[])=> {
}

export class InputModel {

    private _id:string
    private _label:string

    constructor(id:string, label:string) {
        this._id = id;
        this._label = label;
    }

    get id():string {
        return this._id;
    }

    get label():string {
        return this._label;
    }

}

export class InputsModel {

    private _inputs:any[]

    constructor(inputs:any) {
        this._inputs = []

        inputs.forEach((input)=> {
            this.inputs.push(new InputModel(input.id, input.label))
        })
    }

    get inputs():any[] {
        return this._inputs
    }
}

export class InputService {

    ref:EntityMeta

    constructor(@Inject(ApiRoot) apiRoot:ApiRoot) {
        this.ref = apiRoot.root.child('system/ruleengine/conditionlets/')
    }

    static fromSnapshot(snapshot:EntitySnapshot):InputModel {
        return new InputsModel(snapshot.val()[0][0].data)
    }

    get(conditionletID:string, comparisonID:string, cb:Function=noop) {
        this.ref.child(conditionletID).child('comparisons').child(comparisonID).child('inputs').once('value', (snap) => {
            let inputsModel = InputService.fromSnapshot(snap)
            cb(inputsModel)
        }, (e)=> {
            throw e
        })
    }

}