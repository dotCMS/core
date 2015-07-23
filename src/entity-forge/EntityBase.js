import XDebug from 'debug';
let log = XDebug('EntityForge.EntityBase');

import  {Check} from './Check.js'
import  {ConnectionManager} from './ConnectionManager.js'


let emptyFn = function () {
}


let Dispatcher = {
  queries: new Set(),
  register(meta) {
    this.queries.add(meta)
  },

  deregister(meta){
    this.queries.delete(meta)
  },

  notify(path, eventType, payload = null) {
    this.queries.forEach((query)=> {
      query.notify(path, eventType, payload)
    })
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

  exists() {
    return this.entity != null
  }

  child(key) {
    let childPath = this.entityMeta.path + '/' + key
    let childVal = null
    if (this.entity.hasOwnProperty(key)) {
      childVal = this.entity[key]
    }
    var childMeta = new EntityMeta(childPath);
    var childSnap = new EntitySnapshot(childMeta, childVal);
    childMeta.latestSnapshot = childSnap
    return childSnap
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
    this.pathTokens = path ? path.split("/") : []
    this.latestSnapshot = null
    let idx = this.path.lastIndexOf('/')
    this.$key = idx < 0 ? this.path : this.path.substring(idx + 1, this.path.length)
    this.watches = {
      value: new Set(),
      child_added: new Set(),
      child_removed: new Set(),
      child_changed: new Set()
    }
    Dispatcher.register(this)
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
    if (data === null) {
      return this.remove(onComplete)
    }
    let prom = ConnectionManager.persistenceHandler.setItem(this.path, data)
    prom.then(() => {
      Dispatcher.notify(this.path, "change", new EntitySnapshot(this, data))
      onComplete()
    }).catch((e) => onComplete(e))
    return prom
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
    let prom = ConnectionManager.persistenceHandler.removeItem(this.path)
    prom.then(() => {
      Dispatcher.notify(this.path, 'removed', this.latestSnapshot)
      onComplete()
    }).catch((e) => onComplete(e))
    return prom
  }

  push(data, onComplete = emptyFn) {
    return ConnectionManager.persistenceHandler.setItem(this.path, data, true)
        .then((result) => {
          log("Push succeeded, creating snapshot")
          let childMeta = new EntityMeta(result.path);
          let snap = new EntitySnapshot(childMeta, data)
          childMeta.latestSnapshot = snap
          Dispatcher.notify(result.path, 'added', snap)
          return snap
        }).catch((e) => {
          log('Error creating snapshot', e)
          throw e
        })
  }


  _sync() {
    ConnectionManager.persistenceHandler.getItem(this.path).then((responseEntity) => {
      let snap = new EntitySnapshot(this, responseEntity)
      Dispatcher.notify(this.path, 'changed', snap)
      return snap
    }).catch((e) => {
      log(e)
      throw e
    })
  }

  once(eventType, callback = emptyFn, failureCallback = emptyFn) {
    this.on(eventType, (snap) => {
      this.off(eventType, callback)
      callback(snap)
    }, (e) => {
      this.off(eventType, callback)
      failureCallback()
    })
  }


  off(eventType, callback = null) {
    if (callback === null) {
      this.watches[eventType].clear()
    }
    else {
      this.watches[eventType].delete(callback)
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
      {
        let e = new Error("Invalid event name: '" + eventType + "'.")
        log(e)
        throw e
      }

    }
  }

  onValue(callback) {
    this.watches.value.add(callback)
    this._sync()
    return callback
  }

  onChildAdded(callback) {
    this.watches.child_added.add(callback)
  }

  onChildRemoved(callback) {
    this.watches.child_removed.add(callback)

  }

  onChildChanged(callback) {
    this.watches.child_changed.add(callback)

  }

  onChildMoved(callback) {

  }

  notify(path, eventType, payload) {
    try {
      let isSelf = this.path.length == path.length && this.path == path
      // this. path = /foo/abc/
      //  --   path = /foo/abc/123
      let isDescendant = !isSelf && path.startsWith(this.path)
      let isAncestor = !isSelf && !isDescendant && this.path.startsWith(path)
      if (isSelf) {
        switch (eventType) {
          case 'added':
          case 'changed':
            log('added/changed: ', this.path)
            this.latestSnapshot = payload
            this.watches.value.forEach((cb) => {
              cb(payload)
            })
            break;
          default :
            console.log("Unhandled event on self: ", eventType, this.path)
        }
      } else if (isDescendant) {
        let isChild = path.split("/").length === (this.pathTokens.length + 1)
        if (isChild) {
          switch (eventType) {
            case 'changed':
              log('child changed')
              this.watches.child_changed.forEach((cb) => {
                cb(payload)
              })
              break;
            case 'added':
              log('child added', payload)
              this.watches.child_added.forEach((cb) => {
                cb(payload)
              })
              break;
            case 'removed':
              log('child removed', payload)
              this.watches.child_removed.forEach((cb) => {
                cb(payload)
              })
              break;
            case 'moved':
              log('moved')
              break;

          }
        }
      }
    }
    catch (e) {
      log("Notification error: ", e)
    }
  }
}

