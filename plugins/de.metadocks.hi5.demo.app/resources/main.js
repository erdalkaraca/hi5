define([ 'jquery', 'hi5' ], function($, hi5) {
	var module = {};

	module.loadSPA = function() {
		var $uiContainer = $(".app-uiContents");
		$uiContainer.empty();

		var $menu = $("<div></div>");
		$uiContainer.append($menu);
		$menu.hi5(function(partService) {
			partService.loadUIElement("app.mainMenu");
		});
		
		var $contents = $("<div></div>");
		$uiContainer.append($contents);
		$contents.hi5(function(partService) {
			partService.loadUIElement("app.perspective.home");
		});
	}

	return module;
});