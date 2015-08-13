
let ConnectionManager = {
  baseUrl: "http://localhost:8080/",
  username: 'admin@dotcms.com',
  password: 'admin',
  persistenceHandler: {},
  locationQuery: window.location.search.substring(1),
  setBaseUrl(url){
    if(url === null){
      // set to same as current request
      let loc = document.location
      ConnectionManager.baseUrl =  loc.protocol + '//' + loc.host
    }
    else  if(url && (url.startsWith('http://' || url.startsWith('https://')))){
      ConnectionManager.baseUrl = url.endsWith('/') ? url : url + '/' ;
    } else {
      throw new Error("Invalid proxy server base url: '" + url + "'")
    }
  }
}

ConnectionManager.setBaseUrl(null);


if (ConnectionManager.locationQuery && ConnectionManager.locationQuery.length) {
  let q = ConnectionManager.locationQuery
  let token = 'baseUrl='
  let idx = q.indexOf(token)
  if (idx >= 0) {
    let end = q.indexOf('&', idx)
    end = end != -1 ? end : q.length
    ConnectionManager.setBaseUrl(q.substring(idx + token.length, end))
    console.log('Proxy server Base URL set to ', ConnectionManager.baseUrl)
  }
}
export {ConnectionManager};