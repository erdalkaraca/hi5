require([ 'jquery', 'jquery-ui' ], function() {
	require([ 'jquery-layout' ], function() {
		var grammar = {};

		grammar["Application"] = function($e) {
			$e.addClass("ui-layout-center");
			handleChildren($e);
		};

		grammar["TrimmedWindow"] = function($e) {
			$e.addClass("ui-layout-center");
			handleChildren($e);
			handleChildren($e, "TrimBar");
		};

		grammar["PerspectiveStack"] = function($e) {
			$e.addClass("ui-layout-center");
			handleChildren($e);
		};

		grammar["Perspective"] = function($e) {
			$e.addClass("ui-layout-center");
			handleChildren($e);
		};

		grammar["PartStack"] = function($e) {
			$e.addClass("ui-layout-center");
			handleChildren($e);
		};

		grammar["Part"] = function($e) {
			$e.addClass("ui-layout-center");
			handleChildren($e);
		};

		grammar["TrimBar"] = function($e) {
			var dir = $e.attr("direction");
			$e.addClass("ui-layout-north");
			handleChildren($e);
		};

		grammar["ToolBar"] = function($e) {
			$e.addClass("ui-layout-north");
			handleChildren($e);
		};

		grammar["HandledToolItem"] = function($e) {
			$e.html("<button><img src='" + toUrl($e, $e.attr("iconURI"))
					+ "'></img><br>" + $e.attr("label") + "</button>");
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

		function handleChildren($e, filter) {
			$e.children(filter).each(function() {
				applyRule($(this));
			});
		}

		function toUrl($e, relativePath) {
			return $e.attr("contributoruri") + "/" + relativePath;
		}

		// start with the root application node
		applyRule($(".Application"));

		$('.Part').each(function() {
			var $e = $(this);
			var url = toUrl($e, $e.attr("state_index"));
			$e.load(url);
		});

		$(".ui-layout-center").each(function() {
			$(this).parent().layout({
				applyDefaultStyles : false
			});
		});
	});
});