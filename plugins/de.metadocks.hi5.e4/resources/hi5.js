define(
		[ 'jquery' ],
		function(jquery) {
			var grammar = {};

			grammar["Application"] = function($e) {
				handleChildren($e);
			};

			grammar["TrimmedWindow"] = function($e) {
				handleChildren($e);
				handleChildren($e, "TrimBar");
			};

			grammar["PerspectiveStack"] = function($e) {
				$e.addClass("ui-layout-center");
				$e.css("display", "block");

				var $ul = $("<nav class='w3-sidenav w3-white w3-card-2 w3-animate-right' style='display:none;right:0;' id='perspectiveStackMenu'></nav>");
				$ul
						.append("<a href='javascript:void(0)' class='w3-closenav w3-large'>Close &times;</a>");
				$e.children(".Perspective").each(function() {
					var $p = $(this);
					var $a = $("<a href='#'>" + $p.attr("label") + "</a>");
					$ul.append($a);
				});
				$e.prepend($ul);
				$e
						.prepend("<header class='w3-container w3-teal'><span class='w3-opennav w3-xlarge w3-right'>&#9776;</span></header>");
				handleChildren($e);
				$e.children(".Perspective").css("display", "block");
			};

			function showTab() {

			}

			grammar["Perspective"] = function($e) {
				$e.css("display", "block");
				handleChildren($e);
			};

			grammar["PartStack"] = function($e) {
				handleChildren($e);
			};

			grammar["Part"] = function($e) {
				$e.addClass("w3-panel");
				$e.addClass("w3-light-grey");
				$e.addClass("w3-border");
				$e.css("display", "block");
				handleChildren($e);

				var url = toUrl($e, $e.attr("state_index"));
				$e.load(url);
			};

			grammar["TrimBar"] = function($e) {
				var dir = $e.attr("direction");
				handleChildren($e);
			};

			grammar["ToolBar"] = function($e) {
				handleChildren($e);
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

			function handleChildren($e, filter) {
				$e.children(filter).each(function() {
					applyRule($(this));
				});
			}

			function toUrl($e, relativePath) {
				return $e.attr("contributoruri") + "/" + relativePath;
			}

			var hi5 = {};
			hi5.version = "v0.7.0";
			hi5.grammar = grammar;
			hi5.process = applyRule;
			return hi5;
		});