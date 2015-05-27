import {Component,
  onDestroy,
  onChange,
  onAllChangesDone} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';
import {Parent} from 'angular2/src/core/annotations_impl/visibility';
import {ListWrapper} from 'angular2/src/facade/collection';
import {isPresent,
  isString,
  NumberWrapper,
  stringify} from 'angular2/src/facade/lang';
export class MdGridList {
  constructor() {
    this.tiles = [];
  }
  onAllChangesDone() {}
  onChange(_) {
    if (!isPresent(this.spaceTracker)) {
      if (isString(this.cols)) {
        this.cols = NumberWrapper.parseIntAutoRadix(this.cols);
      }
      this.spaceTracker = ListWrapper.createFixedSize(this.cols);
      ListWrapper.fill(this.spaceTracker, 0);
    }
  }
  addTile(tile) {
    ListWrapper.push(this.tiles, tile);
  }
  removeTile(tile) {
    ListWrapper.remove(this.tiles, tile);
  }
  performLayout() {}
  getBaseTileSize(sizePercent, gutterFraction) {
    return `${sizePercent}% - ( ${this.gutterSize} * ${gutterFraction} )`;
  }
  getTilePosition(baseSize, offset) {
    return `calc( (${baseSize} + ${this.gutterSize}) * ${offset} )`;
  }
  getTileSize(baseSize, span) {
    return `calc( (${baseSize} * ${span}) + (${span - 1} * ${this.gutterSize}) )`;
  }
  getTileStyle(tile, rowIndex, colIndex) {
    var percentWidthPerTile = this.cols / 100;
    var gutterWidthFractionPerTile = (this.cols - 1) / this.cols;
    var baseTileWidth = getBaseTileSize(percentWidthPerTile, gutterWidthFractionPerTile);
    var tileStyle = new TileStyle();
    tileStyle.left = getTilePosition(baseTileWidth, colIndex);
    tileStyle.width = getTileSize(baseTileWidth, tile.colspan);
    switch (this.rowHeightMode) {
      case 'fixed':
        tileStyle.top = getTilePosition(stringify(this.fixedRowHeight), rowIndex);
        tileStyle.height = getTileSize(stringify(this.fixedRowHeight), tile.rowspan);
        break;
      case 'ratio':
        var percentHeightPerTile = percentWidthPerTile / this.rowHeightRatio;
        let baseTileHeight = getBaseTileSize(percentHeightPerTile, gutterWidthFractionPerTile);
        tileStyle.marginTop = getTilePosition(baseTileHeight, rowIndex);
        tileStyle.paddingTop = getTileSize(baseTileHeight, tile.rowspan);
        break;
      case 'fit':
        break;
    }
    return tileStyle;
  }
}
Object.defineProperty(MdGridList, "annotations", {get: function() {
    return [new Component({
      selector: 'md-grid-list',
      properties: {
        'cols': 'cols',
        'gutterSize': 'gutter-size'
      },
      lifecycle: [onChange]
    }), new View({templateUrl: 'angular2_material/src/components/grid_list/grid_list.html'})];
  }});
Object.defineProperty(MdGridList.prototype.addTile, "parameters", {get: function() {
    return [[MdGridTile]];
  }});
Object.defineProperty(MdGridList.prototype.removeTile, "parameters", {get: function() {
    return [[MdGridTile]];
  }});
Object.defineProperty(MdGridList.prototype.getBaseTileSize, "parameters", {get: function() {
    return [[assert.type.number], [assert.type.number]];
  }});
Object.defineProperty(MdGridList.prototype.getTilePosition, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.number]];
  }});
Object.defineProperty(MdGridList.prototype.getTileSize, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.number]];
  }});
Object.defineProperty(MdGridList.prototype.getTileStyle, "parameters", {get: function() {
    return [[MdGridTile], [assert.type.number], [assert.type.number]];
  }});
export class MdGridTile {
  constructor(gridList) {
    this.gridList = gridList;
    this.role = 'listitem';
    this.rowspan = 1;
    this.colspan = 1;
    this.styleHeight = `${gridList.tiles.length * 100}px`;
  }
  onChange(_) {
    if (!this.isRegisteredWithGridList) {
      this.gridList.addTile(this);
      this.isRegisteredWithGridList = true;
    } else {
      this.gridList.performLayout();
    }
  }
  onDestroy() {
    this.gridList.removeTile(this);
  }
}
Object.defineProperty(MdGridTile, "annotations", {get: function() {
    return [new Component({
      selector: 'md-grid-tile',
      properties: {
        'rowspan': 'rowspan',
        'colspan': 'colspan'
      },
      hostProperties: {
        'styleHeight': 'style.height',
        'styleWidth': 'style.width',
        'styleTop': 'style.top',
        'styleLeft': 'style.left',
        'styleMarginTop': 'style.marginTop',
        'stylePaddingTop': 'style.paddingTop',
        'role': 'role'
      },
      lifecycle: [onDestroy, onChange]
    }), new View({template: `<figure><content></content></figure>`})];
  }});
Object.defineProperty(MdGridTile, "parameters", {get: function() {
    return [[MdGridList, new Parent()]];
  }});
class TileStyle {}
//# sourceMappingURL=grid_list.js.map

//# sourceMappingURL=./grid_list.map