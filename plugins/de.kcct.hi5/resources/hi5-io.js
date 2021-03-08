define(['jquery'], function($) {
	let module = {};
	module.ax = function(method, dataType, contentType, url, o) {
		const opts = {method, dataType, contentType, url, ...o};
		opts.dataType = opts.dataType || undefined;
		opts.contentType = opts.contentType || undefined;
		if (opts.data && typeof opts.data == 'object') {
			opts.data = JSON.stringify(o.data);
		}
		// check whether REST path params are provided and replace inline in URL
		if (opts.pparams) {
			Object.keys(opts.pparams).forEach(key => {
				opts.url = opts.url.split('{' + key + '}').join(opts.pparams[key]);
			});
		}
		// opts.params deprecated, caller should use opts.qparams
		if (opts.params || opts.qparams) {
			const params = {...opts.params, ...opts.qparams};
			opts.url = opts.url + '?' + $.param(params);
		}
		return $.ajax(opts);
	};
	return module;
});