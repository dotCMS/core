import {Inject, EventEmitter} from 'angular2/core';

import {EntitySnapshot} from "../../../persistence/EntityBase";
import {EntityMeta} from "../../../persistence/EntityBase";
import {ApiRoot} from "../../../persistence/ApiRoot";
import {CwModel} from "../../../util/CwModel";

let noop = (...arg:any[])=> {
}

export class ComparisonsModel {

    private _comparisons:{}

    constructor(comparisons:any) {
        this._comparisons = {}

        comparisons.forEach((comparison)=> {
            this._comparisons[comparison.type] = comparison.label
        })
    }

    get comparisons():{} {
        return this._comparisons
    }
}

export class ComparisonService {

    ref:EntityMeta

    constructor(@Inject(ApiRoot) apiRoot:ApiRoot) {
        this.ref = apiRoot.root.child('system/ruleengine/conditionlets/')
    }

    static fromSnapshot(snapshot:EntitySnapshot):ComparisonsModel {
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