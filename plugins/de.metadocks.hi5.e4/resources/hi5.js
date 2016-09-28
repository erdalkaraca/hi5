define([ 'jquery' ], function(jquery) {
	var hi5 = {
		version : "0.7.0"
	};
	var grammar = {};

	grammar["Application"] = function($e) {
		handleUIElements($e);
	};

	grammar["TrimmedWindow"] = function($e) {
		handleUIElements($e);
		handleUIElements($e, ".TrimBar");
	};

	grammar["Menu"] = function($e) {
		$e.addClass("w3-sidenav w3-white w3-card-2 w3-animate-right");
		$e.hide();
		$e.css("right", "0");

		var $closeItem = $("<a href='javascript:void(0)' class='w3-closenav w3-light-grey'>"
				+ "Close<span class='w3-right w3-large w3-margin-right'>&times;</span></a>");
		$e.prepend($closeItem);
		$closeItem.click(function() {
			$e.css("display", "none");
		});

		handleUIElements($e, ".MenuItem");
	};

	grammar["MenuItem"] = function($e) {
		// TODO handle sub menus
		handleUIElements($e, ".MenuItem");
	};

	grammar["DirectMenuItem"] = function($e) {
		var $a = $("<a  href='#'></a>");
		$a.text($e.attr("label"));
		$e.prepend($a);
		var requireModule = toUrl($e, $e.attr("contributionuri"));
		require([ requireModule ], function(handler) {
			$a.click(function() {
				handler();
			});
		})
	};

	grammar["GenericStack"] = function($e, stackElementsType) {
		if (stackElementsType == null) {
			stackElementsType = $e.children().eq(0).attr("etype");
		}

		var $ul = $("<ul class='w3-navbar w3-white'></ul>");
		$e.children("." + stackElementsType).css("display", "none").each(function() {
			var $p = $(this);
			var $a = $("<a href='#'>" + $p.attr("label") + "</a>");
			var $li = $("<li></li>");
			var clickHandler = function() {
				var $pers = $e.children("." + stackElementsType);
				$pers.css("display", "none");
				$pers.eq($li.index()).css("display", "block");
				$ul.children().attr("active", "false");
				$li.attr("active", "true");
			};
			$a.click(clickHandler);
			// install click handler also for stack element
			// to allow to programmatically show it
			$p.click(clickHandler);
			$li.append($a);
			$ul.append($li);
		});
		$e.prepend($ul);
		handleUIElements($e);
		// select first stack element
		$ul.find("a").eq(0).trigger('click');
	};

	grammar["PerspectiveStack"] = function($e) {
		grammar["GenericStack"]($e, "Perspective");
	};

	grammar["Perspective"] = function($e) {
		handleUIElements($e);
	};

	grammar["PartStack"] = function($e) {
		grammar["GenericStack"]($e, "Part");
	};

	grammar["Part"] = function($e) {
		$("[tags~='ViewMenu']", $e).each(function() {
			var $viewMenu = $(this);
			var $menuTrigger = $("<span class='w3-opennav w3-xlarge w3-right'>&#9776;</span>");
			$menuTrigger.click(function() {
				$viewMenu.css("display", "block");
			});
			$e.prepend($menuTrigger);
			handleUIElements($e, ".Menu");
		});

		$e.addClass("w3-panel");
		$e.addClass("w3-border");
		var contribution = $e.attr("contributionuri");

		if (!contribution) {
			contribution = $e.attr("state_index");
		}

		var url = toUrl($e, contribution);
		var $contents = $("<div class='w3-container'></div>");
		$e.append($contents);
		$contents.load(url);
	};

	grammar["TrimBar"] = function($e) {
		var dir = $e.attr("direction");
		handleUIElements($e);
	};

	grammar["ToolBar"] = function($e) {
		handleUIElements($e);
	};

	grammar["HandledToolItem"] = function($e) {
		$e.html("<button class='w3-btn w3-white'><img src='" + toUrl($e, $e.attr("iconURI")) + "'></img><br>"
				+ $e.attr("label") + "</button>");
		$e.css("float", "left");
	};

	function applyRule($e) {
		var etype = $e.attr("etype");
		var rule = grammar[etype];

		if (rule == null) {
			console.error("No rule found for: " + etype);
		} else {
			rule($e);
		}
	}

	function handleUIElements($e, filter) {
		$e.children(".UIElement " + (filter == null ? "" : filter)).each(function() {
			applyRule($(this));
		});
	}

	function toUrl($e, relativePath) {
		return $e.attr("contributoruri") + "/" + relativePath;
	}

	function getParamNames(fn) {
		var fstr = fn.toString();
		return fstr.match(/\(.*?\)/)[0].replace(/[()]/gi, '').replace(/\s/gi, '').split(',');
	}

	var PartService = function($context) {
		this.$context = $context;
	};
	PartService.prototype.loadUIElement = function(id) {
		this.$context.load("hi5/ws/model/element/" + id, function() {
			$uiElement = $("[elementid='" + id + "']", this);
			applyRule($uiElement);
		});
	};
	PartService.prototype.showPerspective = function(id, callback) {
		$(document).find(".Perspective[elementid='" + id + "']").each(function() {
			var $this = $(this);
			// each stack element knows how to show itself when clicked on,
			// see GenericStack rule
			$this.trigger('click');
			callback.apply($this);
		});
	};

	$.fn.hi5 = function(fn) {
		var names = getParamNames(fn);
		this.each(function() {
			var $this = $(this);
			var context = {
				partService : new PartService($this)
			};

			var args = new Array();

			for (var i = 0; i < names.length; i++) {
				var name = names[i];
				var arg = context[name];
				args.push(arg);
			}

			fn.apply(this, args);
		})
	};

	return hi5;
});