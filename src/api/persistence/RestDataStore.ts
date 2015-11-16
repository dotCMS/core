import {Inject} from 'angular2/angular2';
import {Check} from '../../api/validation/Check'
import {DataStore} from "../../api/persistence/DataStore";
import 'whatwg-fetch';

var fetch = window ? window['fetch'] : top['fetch']

export class RestDataStore extends DataStore {
  authHeader:string

  constructor() {
    super()
    console.log("Creating datastore")
  }

  setItem(path:string, entity:any, isNew:boolean = false) {
    path = this.checkPath(path)
    entity = Check.exists(entity, "Cannot save empty values. Did you mean to remove?")
    return this.remoteSet(path, entity, isNew).then((response) => {
      if( response.isError){
        response.entity = entity // restore the original, entity
      }
      else if (isNew === true) {
        path = path + '/' + response.entity.id
      } else {
        path = path.substring(0, path.lastIndexOf('/') + 1) + response.entity.id
      }
      response['path'] = path
      return response
    })
  }

  getItem(path) {
    return new Promise((resolve, reject) => {
      this.remoteGet(path).then((response) => {
        resolve(response.entity)
      }).catch((err)=> {
        reject(err)
      })
    })
  }

  removeItem(path) {
    return this.remoteDelete(path);
  }

  remoteGet(path) {
    let url = this.pathToUrl(path)
    console.log("Getting entity from: ", url)
    return fetch(url, {
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'Authorization': this.authHeader
      }
    }).catch(this.checkStatus).then(this.checkStatus).then(this.transformResponse)
  }


  remoteDelete(path) {
    let url = this.pathToUrl(path)
    console.log("Deleting entity at: ", url)
    return fetch(url, {
      method: "delete",
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'Authorization': this.authHeader
      }
    }).then(this.checkStatus).then(this.transformResponse).catch((e)=> {
      console.log("Delete operation resulted in an error: ", e)
    })
  }


  transformResponse(response) {
    let result:any = {
      path: null,
      key: null,
      status: response.status || {},
      headers: response.headers || {},
      entity: {},
      isError: false
    }
    let path = response.url
    result.path = path.substring(path.indexOf('/', 8)) // http://foo.com/thisIsThePathWeMean
    result.key = path.substring(path.lastIndexOf('/') + 1);
    let contentType = response.headers.get('Content-Type')
    if(response.hasError === true){
      result.isError = true
      result.error = response.error
    }
    else if (contentType && contentType.toLowerCase().includes('json')) {
      return response.json().then((json) => {
        if (json) {
          result.entity = json
        }
        return result
      }).catch((e) => {
        console.log('Error parsing response:', e)
        throw e
      });
    }
    return new Promise((resolve) => resolve(result))

  }

  checkStatus(response) {
    let error;
    if (response instanceof TypeError) {
      let message = response.message;
      if (response.message == "Failed to fetch") {
        message = response.message + ' (is the host available?)'
      }
      error = new Error(message)
      error.response = {
        status: -1
      }
    }
    else if (!(response.status >= 200 && response.status < 300)) {
      error = new Error(response.statusText)
      error.response = response
    }
    if (error) {
      console.log("Status error: ", error)
      response.error = error
      response.hasError = true
    }
    return response
  }


  pathToUrl(path) {
    if (!path.startsWith('http')) {
      throw new Error("Path must be fully qualified URL.")
    }
    if (path.endsWith('/')) {
      path = path.substring(0, path.length - 1)
    }
    return path
  }

  setAuth(username:string, password:string) {
    if (username && password) {
      this.authHeader = 'Basic ' + btoa(username + ':' + password)
    }
  }

  remoteSet(path, entity, create = false) {
    let url = this.pathToUrl(path)
    console.log("Saving entity to: ", url)
    let headers:any = {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    };
    if (this.authHeader) {
      headers.Authorization = this.authHeader
    }

    return fetch(url, {
      method: create ? "post" : "put",
      credentials: 'same-origin',
      headers: headers,
      body: JSON.stringify(entity)
    }).then(this.checkStatus)
        .then(this.transformResponse)
        .then((result)=> {
          return result
        })
        .catch((e)=> {
          console.log("Save operation resulted in an error: ", e)
          throw e
        })
  }
}