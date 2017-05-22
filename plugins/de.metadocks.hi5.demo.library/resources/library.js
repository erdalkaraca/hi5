define([ 'rivets', 'api!library:library-service' ], function(rivets, lib) {
	var module = {};

	module.loadLibrary = function($container) {
		lib.getBooks({
			success : function(data) {
				var controller = {
					books : data
				};
				rivets.bind($container, controller);
			}
		});
	}

	return module;
});