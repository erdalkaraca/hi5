define([], () => {
        var hi5 = {};
        var grammar = {};

        grammar["Application"] = function ($e) {
            handleUIElements($e);
        };

        grammar["TrimmedWindow"] = function ($e) {
            handleUIElements($e);
        };

        grammar["Menu"] = function ($e) {
            var $menuTrigger = $("<span class='w3-opennav w3-xlarge w3-right w3-white w3-opacity w3-round'>&#9776;</span>");
            $menuTrigger.click(function () {
                // show the menu panel configured below
                $e.show();
            });
            // add to container of the menu
            $e.parent().prepend($menuTrigger);

            // the menu panel which will be shown if the menu trigger is
            // pressed
            $e.addClass("w3-sidenav w3-white w3-card-2 w3-animate-right w3-round");
            $e.hide();
            $e.css("right", "0");
            $e.css("top", "0");

            var $closeItem = $("<span class='w3-button w3-block w3-left-align w3-closenav w3-light-grey w3-hover-red w3-large'>&times;</span>");
            $e.prepend($closeItem);
            $closeItem.click(function () {
                $e.hide();
            });

            handleUIElements($e, ".MenuItem");
        };

        grammar["MenuItem"] = function ($e) {
            // TODO handle sub menus
            handleUIElements($e, ".MenuItem");
        };

        grammar["DirectMenuItem"] = function ($e) {
            processDirectItem($e, true, true);
        };
        grammar["DirectToolItem"] = function ($e) {
            processDirectItem($e, false, false);
        }

        function loadPartContents($partContainer, url) {
            $partContainer.html("<span class='fa fa-spinner w3-spin w3-jumbo w3-center'></span>");
            $partContainer.load(url, function () {
                $partContainer.removeAttr("hi5-contents-url");
                $("*[ui-init]", $partContainer).each(function () {
                    var $container = $(this);
                    var initModuleName = $container.attr("ui-init");
                    if (initModuleName) {
                        require([initModuleName], function (uiMod) {
                            uiMod.createUI($container);
                        });
                    }
                });

                PubSub.publish('hi5.stackelement.activated', $partContainer
                    .attr("elementid"));
            });
        }

        grammar["GenericStack"] = function ($e, stackElementsType) {
            if (stackElementsType == null) {
                stackElementsType = $e.children().eq(0).attr("etype");
            }

            var $ul = $("<div class='w3-bar w3-green'></div>");
            $e.children("." + stackElementsType).hide().each(
                function () {
                    var $p = $(this);
                    var $li = $("<div class='w3-bar-item w3-hover-blue'>" + $p.attr("label") + "</div>");
                    var iconSpanHtml = getIconSpanHtml($p, $p
                        .attr("iconuri"));
                    if (iconSpanHtml) {
                        $li.prepend(iconSpanHtml);
                    }
                    var clickHandler = function () {
                        var $pers = $e
                            .children("." + stackElementsType);
                        $pers.hide();
                        $pers.eq($li.index()).show();
                        $ul.children().attr("active", "false");
                        $li.attr("active", "true");
                        var url = $p.attr("hi5-contents-url");
                        if (url) {
                            loadPartContents($p, url);
                        } else {
                            PubSub.publish(
                                'hi5.stackelement.activated', $e
                                .attr("elementid"));
                        }
                    };
                    $li.click(clickHandler);
                    // install click handler also for stack element
                    // to allow to programmatically show it
                    // $p.click(clickHandler);
                    $ul.append($li);
                });
            $e.prepend($ul);
            handleUIElements($e);
            // select first stack element
            $ul.find("w3-bar-item").eq(0).trigger('click');
        };

        grammar["PerspectiveStack"] = function ($e) {
            grammar["GenericStack"]($e, "Perspective");
        };

        grammar["Perspective"] = function ($e) {
            handleUIElements($e);
        };

        grammar["PartStack"] = function ($e) {
            grammar["GenericStack"]($e, "Part");
        };

        grammar["Part"] = function ($e) {
            $("[tags~='ViewMenu']", $e).each(function () {
                handleUIElements($e, ".Menu");
            });
            handleUIElements($e, ".ToolBar");
            var url = toUrl($e, $e.attr("resourcename"));
            if ($e.index() == 1 || !$e.parent().hasClass("PartStack")) {
                loadPartContents($e, url);
            } else {
                $e.attr("hi5-contents-url", url);
            }
        };

        grammar["TrimBar"] = function ($e) {
            var dir = $e.attr("direction");
            handleUIElements($e);
        };

        grammar["TrimContribution"] = function ($e) {
            handleUIElements($e, ".ToolControl");
        };
        grammar["ToolControl"] = function ($e) {
            var url = toUrl($e, $e.attr("resourcename"));
            if ($e.index() == 1 || !$e.parent().hasClass("PartStack")) {
                loadPartContents($e, url);
            } else {
                $e.attr("hi5-contents-url", url);
            }
        };

        grammar["ToolBar"] = function ($e) {
            $e.addClass("w3-right");
            handleUIElements($e);
        };

        grammar["HandledToolItem"] = function ($e) {
            $e.html("<button class='w3-btn w3-white'><img src='" +
                toUrl($e, $e.attr("iconuri")) + "'></img><br>" +
                $e.attr("label") + "</button>");
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
            var $a = $("<div class='w3-button w3-block w3-left-align w3-white w3-round w3-hover-blue'></div>");

            var label = $e.attr("label");
            if (showLabel) {
                if (label !== "") {
                    $a.text(label);
                }
            } else {
                $a.attr("title", label);
            }

            var iconSpanHtml = getIconSpanHtml($e, $e.attr("iconuri"));
            if (iconSpanHtml) {
                if (!showLabel) {
                    iconSpanHtml.removeClass("w3-margin-right");
                }
                $a.prepend(iconSpanHtml);
            }

            $e.prepend($a);
            var requireModule = toUrl($e, $e.attr("resourcename"));
            $a.click(function () {
                // hide the menu panel
                if (hideParentOnClick) {
                    $e.parent().hide();
                }
                require([requireModule], function (handler) {
                    if (typeof handler.execute === 'function') {
                        if (typeof handler.canExecute === 'function' &&
                            !handler.canExecute()) {
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
                    return $("<span class='w3-margin-right fa " + iconURI +
                        "'></span>");
                } else {
                    return toUrl($e, iconURI);
                }
            }
            return false;
        }

        function handleUIElements($e, filter) {
            $e.children(".UIElement " + (filter == null ? "" : filter))
                .each(function () {
                    applyRule($(this));
                });
        }

        function toUrl($e, relativePath) {
            return $e.attr("bundlepath") + "/" + relativePath;
        }

        function getParamNames(fn) {
            var fstr = fn.toString();
            return fstr.match(/\(.*?\)/)[0].replace(/[()]/gi, '').replace(
                /\s/gi, '').split(',');
        }

        function getState(modelElement, key) {
            return modelElement.persistedState[key];
        }

        function toHTML($uiContainer, modelElement) {
            let etype = getState(modelElement, "etype");
            let div = document.createElement("div");
            div.setAttribute("etype", etype);
            div.setAttribute("elementid", modelElement.elementId);
            div.setAttribute("iconuri", modelElement.iconURI);
            div.setAttribute("label", modelElement.label);
            div.setAttribute("bundlepath", getState(modelElement, "bundlePath"));
            div.setAttribute("resourcename", modelElement.contributionURI);

            // elementId to DOM ID replacing all . with _
            div.id = modelElement.elementId.replace(/\./g, "_");

            // all superTypes as html classes
            let superTypes = getState(modelElement, "superTypes");
            if (superTypes != undefined) {
                div.className = superTypes;
            }

            let $div = $(div);
            $uiContainer.append($div);

			if (modelElement.trimBars) {
				modelElement.trimBars.filter(trim => trim.side === "Top" || trim.side == undefined)
					.forEach(trim => {
                    	toHTML($div, trim);
					});
            }
            if (modelElement.children) {
				modelElement.children.forEach(childElement => {
                    toHTML($div, childElement);
				});
            }
			if (modelElement.mainMenu) {
				toHTML($div, modelElement.mainMenu);
            }
			if (modelElement.trimBars) {
				modelElement.trimBars.filter(trim => trim.side === "Bottom")
					.forEach(trim => {
                    	toHTML($div, trim);
					});
            }
            return $div;
        }

        var PartService = function ($context) {
            this.$context = $context;
        };
        PartService.prototype.renderModel = function (model) {
            let $root = toHTML(this.$context, model);
            applyRule($root);
        };
        PartService.prototype.showPerspective = function (id, callback) {
            $(document).find(".Perspective[elementid='" + id + "']").each(
                function () {
                    var $this = $(this);
                    // each stack element knows how to show itself when
                    // clicked on,
                    // see GenericStack rule
                    $this.trigger('click');
                    callback.apply($this);
                });
        };
        PartService.prototype.select = function (selector) {
            // first find the enclosing Part div, then start searching from
            // that
            // context downwards
            return this.$context.closest(".Part").find(selector);
        };

        $.fn.hi5 = function (fn) {
            var names = getParamNames(fn);
            this.each(function () {
                var $this = $(this);
                var context = {
                    partService: new PartService($this)
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