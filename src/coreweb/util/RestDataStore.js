import XDebug from 'debug';
let log = XDebug('CoreWeb.RestDataStore');


import rest from 'rest';
import basicAuth from 'rest/interceptor/basicAuth';

import  {Core} from '../Core.js'
import  {Check} from '../Check.js'
import  {ServerManager} from '../ServerManager.js'

let client = rest.wrap(basicAuth);

let transformValidResponse = function (response) {
  let result = {
    status: response.status || {},
    headers: response.headers || {},
    request: response.request || {},
    entity: {}
  }
  if (response.entity) {
    result.entity = JSON.parse(response.entity)
  }
  let path = response.request.path
  result.path = path.substring(path.indexOf('/', 8)) // http://foo.com/thisIsThePathWeMean
  result.key = path.substring(path.lastIndexOf('/') + 1);
  return result
}

let transformStatusErrorResponse = function (response) {
  return {
    status: response.status || {},
    headers: response.headers || {},
    entity: response.entity || "",
    request: response.request || {}
  }
}

let pathToUrl = function (path) {
  if (path.startsWith('/')) {
    path = path.substring(1)
  }
  if (path.endsWith('/')) {
    path = path.substring(0, path.length - 1)
  }
  return ServerManager.baseUrl + path.toLowerCase()
}

let remoteSet = function (path, entity, create = false) {
  return new Promise((resolve, reject)=> {
    let url = pathToUrl(path)
    log("Saving entity to: ", url)
    client({
      method: create ? "POST" : "PUT",
      username: 'admin@dotcms.com', password: 'admin',
      path: url,
      entity: JSON.stringify(entity),
      headers: {'Content-Type': 'application/json'}
    }).then((response) => {
      if (response.status.code > 199 && response.status.code < 300) {
        resolve(transformValidResponse(response))
      } else {
        reject(transformStatusErrorResponse(response))
      }
    }).catch((e)=> {
      log("Save operation resulted in an error: ", e)
      reject(e)
    })
  })
}

let remoteGet = function (path) {
  return new Promise((resolve, reject) => {
    let url = pathToUrl(path)
    log("Getting entity from: ", url)
    client({
      username: 'admin@dotcms.com', password: 'admin',
      path: url,
      headers: {'Content-Type': 'application/json'}
    }).then((response) => {
      resolve(transformValidResponse(response))
    }).catch((err)=> {
      reject(err)
    })
  })
}


let remoteDelete = function (path) {
  return new Promise((resolve, reject)=> {
    let url = pathToUrl(path)
    log("Deleting entity at: ", url)
    client({
      method: "DELETE",
      username: 'admin@dotcms.com', password: 'admin',
      path: url,
      headers: {'Content-Type': 'application/json'},
    }).then((response) => {
      if (response.status.code > 199 && response.status.code < 300) {
        resolve(transformValidResponse(response))
      } else {
        reject(transformStatusErrorResponse(response))
      }
    }).catch((e)=> {
      log("Delete operation resulted in an error: ", e)
      reject(e)
    })
  })
}

let emptyFn = function () {
}


let RestDataStore = {
  get length() {
    return localStorage.length
  },
  key(idx) {
    return localStorage.key(idx)
  },
  clear() {
    localStorage.clear()
  },

  setItem(path, entity, isNew) {
    return new Promise((resolve, reject) => {
      path = Check.exists(path, "Cannot save with an empty key")
      entity = Check.exists(entity, "Cannot save empty values. Did you mean to remove?")
      remoteSet(path, entity, isNew).then((response) => {
        if (response.status.code === 200) {
          if (isNew === true) {
            path = path + '/' + response.entity.id
          } else {
            path = path.substring(0, path.lastIndexOf('/') + 1) + response.entity.id
          }
          localStorage.setItem(path, JSON.stringify(entity))
          response.path = path
          resolve(response)
        } else {
          reject(response)
        }
      })
    });
  },
  getItem(path) {
    return new Promise((resolve, reject) => {
      remoteGet(path).then((response) => {
        resolve(response.entity)
      }).catch((err)=> {
        reject(err)
      })
    })
  },
  removeItem(path) {
    return remoteDelete(path);
  },
  getItems(...keys){
    return keys.map((key) => {
      return RestDataStore.getItem(key)
    })
  },
  hasItem(key){
    return localStorage.getItem(key) !== null
  },

  childKeys(path){
    return new Promise((resolve, reject) => {
      remoteGet(path).then((response) => {
        resolve(Object.keys(response.entity))
      }).catch((err) => {
        reject(err)
      })
    })
  },
  childPaths(path) {
    let childPaths = []
    for (let i = 0; i < localStorage.length; i++) {
      let childPath = localStorage.key(i)
      if (childPath.startsWith(path)) {
        childPaths.push(childPath)
      }
    }
    return childPaths;
  },
  childItems(path){
    let pathLen = path.length
    return RestDataStore.childPaths(path).map((childPath) => {
      return {path: childPath, key: childPath.substring(pathLen), val: RestDataStore.getItem(childPath)}
    })
  }

}

export {RestDataStore}


let Foo = {
  getPersistenceHandler(){
    return RestDataStore
  }
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
      persistPromise = Foo.getPersistenceHandler().removeItem(this.path)
    } else {
      persistPromise = Foo.getPersistenceHandler().setItem(this.path, data)
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
    return Foo.getPersistenceHandler().setItem(this.path, data, true)
        .then((result) => {
          return new EntitySnapshot(new EntityMeta(result.path ), data)
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
    return Foo.getPersistenceHandler().getItem(this.path).then((responseEntity) => {
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

