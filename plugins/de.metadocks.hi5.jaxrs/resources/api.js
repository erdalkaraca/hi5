define({
  load : function(name, req, onload, config) {
    var cmps = name.split(":");
    var newName = (cmps[0] === "" ? "" : cmps[0] + "/") + "ws/" + cmps[1]
        + "/api.js";
    req([ newName ], function(value) {
      onload(value);
    });
  }
});