import XDebug from 'debug';
let log = XDebug('CoreWeb.RestDataStore');


import rest from 'rest';
import basicAuth from 'rest/interceptor/basicAuth';

import  {Core, Check} from '../../coreweb/index.js'

let client = rest.wrap(basicAuth);

let transformValidResponse = function (response) {
  return {
    status: response.status || {},
    headers: response.headers || {},
    entity: response.entity ? JSON.parse(response.entity) : null,
    request: response.request || {}
  }
}

let remoteSet = function (path, entity) {
  return new Promise((resolve, reject)=> {
    path = path + "?t=" + new Date().getTime() // break cache hack.

    client({
      username: 'admin@dotcms.com', password: 'admin',
      path: path,
      entity: JSON.stringify(entity),
      headers: {'Content-Type': 'application/json'}
    }).then((response) => {
      resolve(transformValidResponse(response))
    }).catch((e)=> {
      log("Save operation resulted in an error: ", e)
      reject(e)
    })
  })
}

let remoteGet = function (path) {

  return new Promise((resolve, reject) => {
    if(path.endsWith('/')){
      path = path.substring(0, path.length - 1)
    }
    path = path + "?t=" + new Date().getTime() // break cache hack.

    client({
      username: 'admin@dotcms.com', password: 'admin',
      path: path,
      headers: {'Content-Type': 'application/json'}
    }).then((response) => {
      resolve(transformValidResponse(response))
    }).catch((err)=> {
      reject(err)
    })
  })
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

  setItem(path, entity, isNewHack) {
    return new Promise((resolve, reject) => {
      path = Check.exists(path, "Cannot save with an empty key")
      entity = Check.exists(entity, "Cannot save empty values. Did you mean to remove?")
      remoteSet(path, entity).then((response) => {
        if (response.status.code === 200) {
          if (isNewHack === true) {
            path = path + '/' + response.entity.id
          } else {
            path = path.substring(0, path.lastIndexOf('/') + 1) + response.entity.id
          }
          localStorage.setItem(path, JSON.stringify(entity))
          resolve(entity)
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
      }).catch((err)=>{
        debugger;
        reject(err)
      })
    })
  },
  getItems(...keys){
    return keys.map((key) => {
      return RestDataStore.getItem(key)
    })
  },
  hasItem(key){
    return localStorage.getItem(key) !== null
  },
  removeItem(key) {
    let itemJson = localStorage.getItem(key)
    let item = null
    if (itemJson !== null) {
      item = JSON.parse(item)
    }
    localStorage.removeItem(key)
    return item;
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
    debugger;
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
    debugger;
    let pathLen = path.length
    return RestDataStore.childPaths(path).map((childPath) => {
      return {path: childPath, key: childPath.substring(pathLen), val: RestDataStore.getItem(childPath)}
    })
  }

}

export {RestDataStore}