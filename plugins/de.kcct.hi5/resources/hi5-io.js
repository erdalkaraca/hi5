define(['jquery'], function($) {
	let module = {};
	module.ax = function(u, s, o) {
		o = o || {};
		if (o.data && typeof o.data == 'object') {
			o.data = JSON.stringify(o.data);
		}
		s.url = u + s.url;
		if (o.params) {
			s.url = s.url + '?' + $.param(o.params);
		}
		$.ajax($.extend(s, o))
	};
	return module;
});