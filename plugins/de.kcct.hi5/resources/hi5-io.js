define(['jquery'], function($) {
	let module = {};
	module.ax = function(method, dataType, contentType, url, o) {
		const opts = {method, dataType, contentType, url, ...o};
		opts.dataType = opts.dataType || undefined;
		opts.contentType = opts.contentType || undefined;
		if (opts.data && typeof opts.data == 'object') {
			opts.data = JSON.stringify(o.data);
		}
		if (opts.params) {
			opts.url = opts.url + '?' + $.param(opts.params);
		}
		return $.ajax(opts);
	};
	return module;
});