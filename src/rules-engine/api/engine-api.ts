import jsonp from 'jsonp'

class EngineApi {
  private baseUrl;
  private dataUrl;
  private redditUrl;
  constructor() {
    this.baseUrl = "http://localhost:8080";
    this.dataUrl = this.baseUrl + "/api/sites/${site_id}/rules";
  }

  load() {
    return new Promise((resolve, reject) => {
      jsonp(this.redditUrl, {param: 'jsonp'}, (err, data) => {
        err ? reject(err) : resolve(data.data.children)
      })
    })
  }
}

export default new EngineApi();