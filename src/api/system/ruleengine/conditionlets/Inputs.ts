import {Inject, EventEmitter} from 'angular2/angular2';

import {EntitySnapshot} from "../../../persistence/EntityBase";
import {EntityMeta} from "../../../persistence/EntityBase";
import {ApiRoot} from "../../../persistence/ApiRoot";
import {CwModel} from "../../../util/CwModel";

let noop = (...arg:any[])=> {
}

export class InputsModel {

    private _inputs: {}

    constructor(inputs:any) {
        this._inputs = { }

        inputs.forEach((input)=> {
            this._inputs[input.type] = input.label
        })
    }

    get inputs():{} {
        return this._inputs
    }
}

export class InputService {

    ref:EntityMeta

    constructor(@Inject(ApiRoot) apiRoot:ApiRoot) {
        this.ref = apiRoot.root.child('system/ruleengine/conditionlets/')
    }

    static fromSnapshot(snapshot:EntitySnapshot):InputsModel {
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