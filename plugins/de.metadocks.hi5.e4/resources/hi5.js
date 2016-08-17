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

	grammar["GenericStack"] = function($e, stackElementsType) {
		if (stackElementsType == null) {
			stackElementsType = $e.children().eq(0).attr("etype");
		}

		var $ul = $("<ul class='w3-navbar w3-white'></ul>");
		$e.children("." + stackElementsType).css("display", "none").each(
				function() {
					var $p = $(this);
					var $a = $("<a href='#'>" + $p.attr("label") + "</a>");
					var $li = $("<li></li>");
					$a.click(function() {
						var $pers = $e.children("." + stackElementsType);
						$pers.css("display", "none");
						$pers.eq($li.index()).css("display", "block");
						$ul.children().attr("active", "false");
						$li.attr("active", "true");
					});
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
		$e.addClass("w3-panel");
		$e.addClass("w3-light-grey");
		$e.addClass("w3-border");
		var url = toUrl($e, $e.attr("state_index"));
		$e.load(url);
	};

	grammar["TrimBar"] = function($e) {
		var dir = $e.attr("direction");
		handleUIElements($e);
	};

	grammar["ToolBar"] = function($e) {
		handleUIElements($e);
	};

	grammar["HandledToolItem"] = function($e) {
		$e.html("<button class='w3-btn w3-white'><img src='"
				+ toUrl($e, $e.attr("iconURI")) + "'></img><br>"
				+ $e.attr("label") + "</button>");
		$e.css("float", "left");
	};

	function applyRule($e) {
		var etype = $e.attr("etype");
		var rule = grammar[etype];

		if (rule == null) {
			console.log("No rule found for: " + etype);
		} else {
			rule($e);
		}
	}

	function handleUIElements($e, filter) {
		$(".UIElement " + (filter == null ? "" : filter), $e).each(function() {
			applyRule($(this));
		});
	}

	function toUrl($e, relativePath) {
		return $e.attr("contributoruri") + "/" + relativePath;
	}

	function getParamNames(fn) {
		var fstr = fn.toString();
		return fstr.match(/\(.*?\)/)[0].replace(/[()]/gi, '').replace(/\s/gi,
				'').split(',');
	}

	function getData($element, key) {
		var context = $element.data("hi5_context");

		if (context == null) {
			var $parent = $element.parent();

			// document.body is defined as highest context
			if ($parent == null || $parent.length == 0
					|| $parent[0] == document.body) {
				return hi5[key];
			}

			return getData($parent, key);
		}

		return context[key];
	}

	var PartService = function($container) {
		this.$container = $container;
	};
	PartService.prototype.loadUIElement = function(id) {
		this.$container.load("hi5/ws/element/" + id, function() {
			$uiElement = $("[elementid='" + id + "']", this);
			applyRule($uiElement);
		});
	};

	$.fn.hi5 = function(fn) {
		var names = getParamNames(fn);
		this.each(function() {
			var $this = $(this);
			$this.data("hi5_context", {
				partService : new PartService($this)
			});
			var args = new Array();

			for (var i = 0; i < names.length; i++) {
				var name = names[i];
				var arg = getData($this, name);
				args.push(arg);
			}

			fn.apply(this, args);
		})
	};

	return hi5;
});