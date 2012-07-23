if (typeof dwr == 'undefined' || dwr.engine == undefined) throw new Error('You must include DWR engine before including this file');

(function() {
  if (dwr.engine._getObject("ContainerAjaxDrawedTemplate") == undefined) {
    var p;
    
    p = {};
    p._path = '/dwr';

    /**
     * @param {class java.lang.String} p0 a param
     * @param {function|Object} callback callback function or options object
     */
    p.getContainerStructure = function(p0, callback) {
      return dwr.engine._execute(p._path, 'ContainerAjaxDrawedTemplate', 'getContainerStructure', arguments);
    };

    /**
     * @param {class java.lang.String} p0 a param
     * @param {function|Object} callback callback function or options object
     */
    p.checkDependencies = function(p0, callback) {
      return dwr.engine._execute(p._path, 'ContainerAjaxDrawedTemplate', 'checkDependencies', arguments);
    };

    /**
     * @param {interface java.util.Map} p0 a param
     * @param {interface java.util.Map} p1 a param
     * @param {int} p2 a param
     * @param {int} p3 a param
     * @param {interface java.util.List} p4 a param
     * @param {boolean} p5 a param
     * @param {function|Object} callback callback function or options object
     */
    p.fetchContainersDesignTemplate = function(p0, p1, p2, p3, p4, p5, callback) {
      return dwr.engine._execute(p._path, 'ContainerAjaxDrawedTemplate', 'fetchContainersDesignTemplate', arguments);
    };

    dwr.engine._setObject("ContainerAjaxDrawedTemplate", p);
  }
})();