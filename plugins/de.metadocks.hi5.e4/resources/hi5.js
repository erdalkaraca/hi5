define(
    [ 'jquery', 'pubsubjs' ],
    function(jquery) {
      var hi5 = {
        version : "0.7.0"
      };
      var grammar = {};

      grammar["Application"] = function($e) {
        handleUIElements($e);
      };

      grammar["TrimmedWindow"] = function($e) {
        handleUIElements($e);
        handleUIElements($e, ".Menu");
        handleUIElements($e, ".TrimBar");
      };

      grammar["Menu"] = function($e) {
        var $menuTrigger = $("<span class='w3-opennav w3-xlarge w3-right w3-margin-right'>&#9776;</span>");
        $menuTrigger.click(function() {
          // show the menu panel configured below
          $e.show();
        });
        // add to container of the menu
        $e.parent().prepend($menuTrigger);

        // the menu panel which will be shown if the menu trigger is pressed
        $e.addClass("w3-sidenav w3-white w3-card-2 w3-animate-right");
        $e.hide();
        $e.css("right", "0");

        var $closeItem = $("<a href='javascript:void(0)' class='w3-closenav w3-light-grey'>"
            + "Close<span class='w3-right w3-large w3-margin-right'>&times;</span></a>");
        $e.prepend($closeItem);
        $closeItem.click(function() {
          $e.hide();
        });

        handleUIElements($e, ".MenuItem");
      };

      grammar["MenuItem"] = function($e) {
        // TODO handle sub menus
        handleUIElements($e, ".MenuItem");
      };

      grammar["DirectMenuItem"] = function($e) {
        processDirectItem($e, true, true);
      };
      grammar["DirectToolItem"] = function($e) {
        processDirectItem($e, false, false);
      }

      grammar["GenericStack"] = function($e, stackElementsType) {
        if (stackElementsType == null) {
          stackElementsType = $e.children().eq(0).attr("etype");
        }

        var $ul = $("<ul class='w3-navbar w3-white'></ul>");
        $e.children("." + stackElementsType).hide().each(function() {
          var $p = $(this);
          var $a = $("<a href='#'>" + $p.attr("label") + "</a>");

          var iconSpanHtml = getIconSpanHtml($p, $p.attr("iconURI"));
          if (iconSpanHtml) {
            $a.prepend(iconSpanHtml);
          }

          var $li = $("<li></li>");
          var clickHandler = function() {
            var $pers = $e.children("." + stackElementsType);
            $pers.hide();
            $pers.eq($li.index()).show();
            $ul.children().attr("active", "false");
            $li.attr("active", "true");
            var url = $p.attr("hi5-contents-url");
            if (url) {
            	$p.load(url, function() {
            		$p.removeAttr("hi5-contents-url");
            		PubSub.publish('hi5.stackelement.activated', $e.attr("elementid"));
            	});
			} else {
				PubSub.publish('hi5.stackelement.activated', $e.attr("elementid"));
			}
          };
          $a.click(clickHandler);
          // install click handler also for stack element
          // to allow to programmatically show it
          // $p.click(clickHandler);
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
          handleUIElements($e, ".Menu");
        });
        handleUIElements($e, ".ToolBar");
        var contribution = $e.attr("contributionuri");
        if (!contribution) {
          contribution = $e.attr("state_index");
        }
        var url = toUrl($e, contribution);
        if ($e.index() == 1) {
        	$e.load(url);
		} else {
			$e.attr("hi5-contents-url", url);
		}
      };

      grammar["TrimBar"] = function($e) {
        var dir = $e.attr("direction");
        handleUIElements($e);
      };

      grammar["ToolBar"] = function($e) {
        $e.addClass("w3-right");
        handleUIElements($e);
      };

      grammar["HandledToolItem"] = function($e) {
        $e.html("<button class='w3-btn w3-white'><img src='"
            + toUrl($e, $e.attr("iconURI")) + "'></img><br>" + $e.attr("label")
            + "</button>");
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

      function processDirectItem($e, hideParentOnClick, showLabel) {
        var $a = $("<div class='w3-btn w3-white w3-round w3-margin-left w3-margin-righ w3-hover'></div>");

        var label = $e.attr("label");
        if (showLabel) {
          if (label !== "") {
            $a.text(label);
          }
        } else {
          $a.attr("title", label);
        }

        var iconSpanHtml = getIconSpanHtml($e, $e.attr("iconURI"));
        if (iconSpanHtml) {
          if (!showLabel) {
            iconSpanHtml.removeClass("w3-margin-right");
          }
          $a.prepend(iconSpanHtml);
        }

        $e.prepend($a);
        var requireModule = toUrl($e, $e.attr("contributionuri"));
        $a.click(function() {
          // hide the menu panel
          if (hideParentOnClick) {
            $e.parent().hide();
          }
          require([ requireModule ], function(handler) {
            if (typeof handler.execute === 'function') {
              if (typeof handler.canExecute === 'function'
                  && !handler.canExecute()) {
                return;
              }
              handler.execute();
            }
          });
        })
      }

      function getIconSpanHtml($e, iconURI) {
        if (iconURI) {
          if (iconURI.match('^fa-')) {
            return $("<span class='w3-margin-right fa " + iconURI + "'></span>");
          } else {
            return toUrl($e, iconURI);
          }
        }
        return false;
      }

      function handleUIElements($e, filter) {
        $e.children(".UIElement " + (filter == null ? "" : filter)).each(
            function() {
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

      var PartService = function($context) {
        this.$context = $context;
      };
      PartService.prototype.loadUIElement = function(id, fn) {
        this.$context.load("hi5/ws/model/element/" + id, function() {
          $uiElement = $("[elementid='" + id + "']", this);
          applyRule($uiElement);
          if (typeof fn === 'function') {
            fn();
          }
        });
      };
      PartService.prototype.showPerspective = function(id, callback) {
        $(document).find(".Perspective[elementid='" + id + "']").each(
            function() {
              var $this = $(this);
              // each stack element knows how to show itself when clicked on,
              // see GenericStack rule
              $this.trigger('click');
              callback.apply($this);
            });
      };
      PartService.prototype.select = function(selector) {
        // first find the enclosing Part div, then start searching from that
        // context downwards
        return this.$context.closest(".Part").find(selector);
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