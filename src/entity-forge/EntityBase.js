import XDebug from 'debug';
let log = XDebug('EntityForge.EntityBase');

import  {Check} from './Check.js'
import  {ConnectionManager} from './ConnectionManager.js'


let emptyFn = function () {
}


export class MetaManager {

  constructor() {
    this.metasByPath = new Map()
  }

  register(meta) {
    let instances = this.metasByPath.get(meta.path)
    if (!instances) {
      instances = new Set()
      this.metasByPath.put(meta.path, instances)
    }
    instances.add(meta)
  }


  notify(path, eventType) {

  }


}

export class EntitySnapshot {
  constructor(entityMeta, entity) {
    this.entityMeta = entityMeta;
    this.entity = entity
  }

  val() {
    return this.entity
  }

  key() {
    return this.entityMeta.key()
  }

  ref() {
    return this.entityMeta
  }

  exists(){
    return this.entity != null
  }

  child(key) {
    let childPath = this.entityMeta.path + '/' + key
    let childVal = null
    if (this.entity.hasOwnProperty(key)) {
      childVal = this.entity[key]
    }
    return new EntitySnapshot(new EntityMeta(childPath), childVal)
  }

  forEach(childAction) {
    Object.keys(this.entity).every((key) => {
      let snap = this.child(key)
      return childAction(snap) !== true // break if 'true' returned by callback.
    })
  }
}


export class EntityMeta {
  constructor(path) {
    this.path = path
    let idx = this.path.lastIndexOf('/')
    this.$key = idx < 0 ? this.path : this.path.substring(idx + 1, this.path.length)
  }

  child(key) {
    return new EntityMeta(this.path + "/" + key)
  }

  key() {
    return this.$key
  }

  /**
   * Replace the existing entry at this location with the provided data.
   * If 'data' is null this operation is effectively a remove.
   * The provided data object will be serialized using JSON.stringify.
   * @param data A literal, such as a string or number, or an object map with children of its own.
   * @param onComplete
   * @returns {*}
   */
  set(data, onComplete = emptyFn) {
    let persistPromise;
    if (data === null) {
      persistPromise = ConnectionManager.persistenceHandler.removeItem(this.path)
    } else {
      persistPromise = ConnectionManager.persistenceHandler.setItem(this.path, data)
    }
    return persistPromise.then((result) => {
      return onComplete(result)
    })
  }

  /**
   * Intended to do a merge, for now it just delegates to '#set'
   * @param data
   * @param onComplete
   * @returns {*}
   */
  update(data, onComplete = emptyFn) {
    return this.set(data, onComplete)
  }

  remove(onComplete = emptyFn) {
    return this.set(null, onComplete)
  }

  push(data, onComplete = emptyFn) {
    return ConnectionManager.persistenceHandler.setItem(this.path, data, true)
        .then((result) => {
          console.log("Push succeeded, creating snapshot")
          return new EntitySnapshot(new EntityMeta(result.path ), data)
        }).catch((e) => {
          console.log('Error creating snapshot', e)
          throw e
        })

  }

  once(eventType, callback = emptyFn, failureCallback = emptyFn) {
    return new Promise((resolve, reject) => {
      this.on(eventType, callback, failureCallback).then((result) => {
        this.off(eventType, callback)
        resolve(result)
      }).catch((e) => {
        this.off(eventType, callback)
        reject(e)
      })
    });
  }

  off(eventType = null, callback = null) {
    if (eventType === null) {
      // remove all
    }
    else if (callback === null) {
      // remove all for event type
    }
    else {

    }
  }

  on(eventType, callback, failureCallback = emptyFn) {
    switch (eventType) {
      case 'value':
        return this.onValue(callback)
        break
      case 'child_added':
        return this.onChildAdded(callback)
        break
      case 'child_changed':
        return this.onChildChanged(callback)
        break
      case 'child_moved':
        return this.onChildMoved(callback)
        break
      case 'child_removed':
        return this.onChildRemoved(callback)
        break
      default:
        throw new Error("Invalid event name: '" + eventType + "'.")
    }
  }

  onValue(callback) {
    return ConnectionManager.persistenceHandler.getItem(this.path).then((responseEntity) => {
      let snap = new EntitySnapshot(this, responseEntity)
      callback(snap)
      return snap
    }).catch((err) => {
      callback(err)
    })
  }

  onChildAdded(callback) {

  }

  onChildRemoved(callback) {

  }

  onChildChanged(callback) {

  }

  onChildMoved(callback) {

  }
}

