import {Inject} from 'angular2/angular2';

import {UserModel} from "api/auth/UserModel";


export class ApiRoot {
  // Points to {baseUrl}/api/v1
  root:EntityMeta;
  defaultSite:EntityMeta;
  baseUrl: string = "http://localhost:8080/";
  username: string
  password: string
  defaultSiteId: string = '48190c8c-42c4-46af-8d1a-0cd5db894797'
  userModel: UserModel;
  resourceRef: EntityMeta
  constructor(@Inject(UserModel) userModel:UserModel){
    this.userModel = userModel
    this.username = userModel.username
    this.password = userModel.password
    let url = this.checkQueryForUrl(document.location.search.substring(1))
    this.setBaseUrl(url) // if null, just uses the base of the current URL
    this.resourceRef = this.root.child('system/i18n')
  }

  testStuff(){
    let countryDataListRef:EntityMeta = this.resourceRef.child('en/system/locale/country')
    countryDataListRef.on('value', (snapshot)=>{
      let jsonData = snapshot.val()
    })
  }

  checkQueryForUrl(locationQuery:string):string{
    let queryBaseUrl = null;
    if (locationQuery && locationQuery.length) {
      let q = locationQuery
      let token = 'baseUrl='
      let idx = q.indexOf(token)
      if (idx >= 0) {
        let end = q.indexOf('&', idx)
        end = end != -1 ? end : q.length
        queryBaseUrl = q.substring(idx + token.length, end)
        console.log('Proxy server Base URL set to ', queryBaseUrl)
      }
    }
    return queryBaseUrl
  }

  setBaseUrl(url=null){
    if(url === null){
      // set to same as current request
      let loc = document.location
      this.baseUrl =  loc.protocol + '//' + loc.host + '/'
    }
    else  if(url && (url.startsWith('http://' || url.startsWith('https://')))){
      this.baseUrl = url.endsWith('/') ? url : url + '/' ;
    } else {
      throw new Error("Invalid proxy server base url: '" + url + "'")
    }
    this.root = new EntityMeta(this.baseUrl + 'api/v1')
    this.defaultSite = this.root.child('sites/' + this.defaultSiteId)
  }


  getRoot():EntityMeta {
    return this.root
  }
}