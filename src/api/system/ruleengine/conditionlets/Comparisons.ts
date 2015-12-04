import {Inject, EventEmitter} from 'angular2/angular2';

import {EntitySnapshot} from "../../../persistence/EntityBase";
import {EntityMeta} from "../../../persistence/EntityBase";
import {ApiRoot} from "../../../persistence/ApiRoot";
import {CwModel} from "../../../util/CwModel";

let noop = (...arg:any[])=> {
}

export class ComparisonModel {

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

export class ComparisonsModel {

    private _comparisons:any[]

    constructor(comparisons:any) {
        this._comparisons = []

        comparisons.forEach((comparison)=> {
            this._comparisons.push(new ComparisonModel(comparison.id, comparison.label))
        })
    }

    get comparisons():any[] {
        return this._comparisons
    }
}

export class ComparisonService {

    ref:EntityMeta

    constructor(@Inject(ApiRoot) apiRoot:ApiRoot) {
        this.ref = apiRoot.root.child('system/ruleengine/conditionlets/')
    }

    static fromSnapshot(snapshot:EntitySnapshot):ComparisonModel {
        return new ComparisonsModel(snapshot.val()[0])
    }

    get(conditionletID:string, cb:Function=noop) {
        this.ref.child(conditionletID).child('comparisons').once('value', (snap) => {
            let comparisonsModel = ComparisonService.fromSnapshot(snap)
            cb(comparisonsModel)
        }, (e)=> {
            throw e
        })
    }

}