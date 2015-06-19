let LocalDataStore = {
  get length() {
    return localStorage.length
  },
  key(idx) {
    return localStorage.key(idx)
  },
  clear() {
    localStorage.clear()
  },
  setItem(key, value) {
    key = Check.exists(key, "Cannot save with an empty key")
    value = Check.exists(value, "Cannot save empty values. Did you mean to remove?")
    localStorage.setItem(key, JSON.stringify(value))
  },
  getItem(key) {
    let item = localStorage.getItem(key)
    item = item === null ? null : JSON.parse(item)
    return item
  },
  getItems(...keys){
    return keys.map((key) => {
      return LocalDataStore.getItem(key)
    })
  },
  hasItem(key){
    return localStorage.getItem(key) !== null
  },
  removeItem(key) {
    let itemJson = localStorage.getItem(key)
    let item = null
    if(itemJson !== null){
      item = JSON.parse(item)
    }
    localStorage.removeItem(key)
    return item;
  },
  childKeys(path){
    let pathLen = path.length
    let childKeys = LocalDataStore.childPaths(path).map((childPath) => {
      return childPath.substring(pathLen)
    })
    return childKeys;
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
    return LocalDataStore.childPaths(path).map((childPath) => {
      return { path:childPath,  key: childPath.substring(pathLen), val: LocalDataStore.getItem(childPath) }
    })
  }

}

export {LocalDataStore }