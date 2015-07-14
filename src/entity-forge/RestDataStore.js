import XDebug from 'debug';
let log = XDebug('EntityForge.RestDataStore');

import  {Check} from './Check.js'
import  {ConnectionManager} from './ConnectionManager.js'
import rest from 'rest';
import basicAuth from 'rest/interceptor/basicAuth';


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
  return ConnectionManager.baseUrl + path.toLowerCase()
}

let remoteSet = function (path, entity, create = false) {
  return new Promise((resolve, reject)=> {
    let url = pathToUrl(path)
    log("Saving entity to: ", url)
    client({
      method: create ? "POST" : "PUT",
      username: ConnectionManager.username, password: ConnectionManager.password,
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


export let RestDataStore = {

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


  childKeys(path){
    return new Promise((resolve, reject) => {
      remoteGet(path).then((response) => {
        resolve(Object.keys(response.entity))
      }).catch((err) => {
        reject(err)
      })
    })
  },

}