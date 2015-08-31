import  {ConnectionManager} from './ConnectionManager.js'
import  {Check} from '../validation/Check.js'


let emptyFn = function () {
}

let Dispatcher = {
  queries: new Set(),
  paths: new Set(),
  register(meta) {
    this.queries.add(meta)
  },

  deregister(meta){
    this.queries.delete(meta)
  },

  snapshotCreated(snap){
    let url = snap.ref().toString()
    if(!this.paths.has(url)){
      this.notify(url, "added", snap)
      this.paths.add(url)
    }
  },

  notify(path, eventType, payload = null) {
    this.queries.forEach((query)=> {
      query.notify(path, eventType, payload)
    })
  }
}

let RootMaker = {

  gimme(){
    let root = {}
    Object.observe(root, RootMaker.ceilingCatWatchesForAdditions, ['add'] )
  },

  ceilingCatWatchesForAdditions(changes){
    changes.forEach((change) => {

    })

  }
}


export class EntitySnapshot {
  constructor(path, entity=null) {
    this._path = path
    this._entity = entity // @todo ggranum: this should be cloned
    Dispatcher.snapshotCreated(this)
  }

  val() {
    return this._entity
  }

  key() {
    if(!this._key){
      let idx = this._path.lastIndexOf('/')
      this._key = idx < 0 ? this._path : this._path.substring(idx + 1, this._path.length)
    }
    return this._key
  }

  ref() {
    if(!this._meta){
      this._meta = new EntityMeta(this._path)
    }
    return this._meta
  }

  exists() {
    return this._entity != null
  }

  child(key) {
    let childPath = this._path + '/' + key
    let childVal = null
    if (this.exists() && this._entity.hasOwnProperty(key)) {
      childVal = this._entity[key]
    }
    return new EntitySnapshot(childPath, childVal);
  }

  forEach(childAction) {
    Object.keys(this._entity).every((key) => {
      let snap = this.child(key)
      return childAction(snap) !== true // break if 'true' returned by callback.
    })
  }
}


export class EntityMeta {
  constructor(url) {
    this.path = url
    this.pathTokens = url ? url.split("/") : []
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

  toString(){
    return this.path
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
      Dispatcher.notify(this.path, "change", new EntitySnapshot(this.toString(), data))
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
          console.log("Push succeeded, creating snapshot")
          let snap = new EntitySnapshot(result.path, data)
          let childMeta = new EntityMeta(result.path);
          childMeta.latestSnapshot = snap
          return snap
        }).catch((e) => {
          console.log('Error creating snapshot', e)
          throw e
        })
  }


  _sync() {
    ConnectionManager.persistenceHandler.getItem(this.path).then((responseEntity) => {
      let snap = new EntitySnapshot(this.toString(), responseEntity)
      Dispatcher.notify(this.path, 'changed', snap)
      return snap
    }).catch((e) => {
      console.log(e)
      throw e
    })
  }

  once(eventType, callback = emptyFn, failureCallback = emptyFn) {
    let tempCB = (snap) => {
      this.off(eventType  , tempCB)
      callback(snap)
    }

    this.on(eventType, tempCB, (e) => {
      this.off(eventType, tempCB)
      failureCallback(e)
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
        console.log(e)
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
            console.log('added/changed: ', this.path)
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
              console.log('child changed', path)
              this.watches.child_changed.forEach((cb) => {
                cb(payload)
              })
              break;
            case 'added':
              console.log('child added', path, payload)
              this.watches.child_added.forEach((cb) => {
                cb(payload)
              })
              break;
            case 'removed':
              console.log('child removed', path, payload)
              this.watches.child_removed.forEach((cb) => {
                cb(payload)
              })
              break;
            case 'moved':
              console.log('moved')
              break;

          }
        }
      }
    }
    catch (e) {
      console.log("Notification error: ", e)
    }
  }
}

