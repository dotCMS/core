import {Injectable} from 'angular2/core';

@Injectable()
export class UserModel {
  username: string
  password: string
  locale: string

  constructor(){
    if(top.location.port && Number.parseInt(top.location.port) >= 9000) {
      this.username = 'admin@dotcms.com'
      this.password = 'admin'
    }
    this.locale = 'en-US' // default to 'en-US'
    try{
      let url = window.location.search.substring(1)
      this.locale = this.checkQueryForUrl(url)
    } catch(e){
      console.log("Could not set locale from URL.")
    }
  }

  checkQueryForUrl(locationQuery:string):string{
    let locale = this.locale;
    if (locationQuery && locationQuery.length) {
      let q = locationQuery
      let token = 'locale='
      let idx = q.indexOf(token)
      if (idx >= 0) {
        let end = q.indexOf('&', idx)
        end = end != -1 ? end : q.indexOf('#', idx)
        end = end != -1 ? end : q.length
        locale = q.substring(idx + token.length, end)
        console.log('Locale set to to ', locale)
      }
    }
    return locale
  }

}