import RedditApi from './reddit-api';
import ExtractGifs from './extract-gifs';
import DisplayGifs from './display-gifs';
import css from  './main.css!';

export class LoopGifs extends HTMLElement {

  constructor() {
    super()
  }

  createdCallback() {
    this.innerHTML = `<div class="title">LoopGifs WebComponent</div>`;
    RedditApi.load()
      .then(ExtractGifs)
      .then((url) => DisplayGifs(url, this))
  }
}

document.registerElement('loop-gifs', LoopGifs)
