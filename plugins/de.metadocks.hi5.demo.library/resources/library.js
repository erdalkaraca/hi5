define([ 'rivets', 'api!library:library-service' ], function(rivets, lib) {
	var module = {};

	module.loadLibrary = function($container) {
		lib.getLibrary({
			success : function(data) {

			}
		});
	}

	return module;
});