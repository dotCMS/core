import  {Check} from '../../api/validation/Check'
import  {Verify} from '../../api/validation/Verify'
import {DataStore} from '../../api/persistence/DataStore'

export class LocalDataStore extends DataStore {

  constructor() {
    super()
  }

  get length() {
    return localStorage.length
  }

  key(idx) {
    return localStorage.key(idx)
  }

  clear() {
    localStorage.clear()
  }

  setItem(path, value) {
    path = this.checkPath(path)
    if (Verify.exists(value)) {
      localStorage.setItem(path, JSON.stringify(value))
    }
    else {
      this.removeItem(path)
    }
  }

  getItem(path) {
    let item = localStorage.getItem(path)
    item = item === null ? null : JSON.parse(item)
    return item
  }

  getItems(...paths) {
    return paths.map((path) => {
      return this.getItem(path)
    })
  }

  hasItem(path) {
    return localStorage.getItem(path) !== null
  }

  removeItem(path) {
    let itemJson = localStorage.getItem(path)
    let item = null
    if (itemJson !== null) {
      item = JSON.parse(item)
    }
    localStorage.removeItem(path)
    return item;
  }

  childKeys(path) {
    let pathLen = path.length
    return this.childPaths(path).map((childPath) => {
      return childPath.substring(pathLen)
    })
  }

  childPaths(path) {
    let childPaths = []
    for (let i = 0; i < localStorage.length; i++) {
      let childPath = localStorage.key(i)
      if (childPath.startsWith(path)) {
        childPaths.push(childPath)
      }
    }
    return childPaths;
  }

  childItems(path) {
    let pathLen = path.length
    return this.childPaths(path).map((childPath) => {
      return {path: childPath, key: childPath.substring(pathLen), val: this.getItem(childPath)}
    })
  }
}

