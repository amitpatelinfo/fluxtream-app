define(["core/FlxState"], function(FlxState) {
	
	function Application(name, author, icon) {
		this.name = name;
		this.author = author;
		this.icon = icon;
	}
	
	Application.prototype.destroy = function() {
		console.log("WARNING: DESTROY IS NOT IMPLEMENTED!!!")
	}
	
	/**
	 * Retrieve and dom-insert the html fragment that's associated
	 * with this app, destroy (unwire) the
	 * previous app and call renderState() on the new one
	 */
	Application.prototype.render = function(state) {
		console.log("showing up application " + this.name);
		$("#"+this.name+"MenuButton").button('toggle');
		if (state==="last")
			state = FlxState.getState(this.name);
		that = this;
		if ($(".application").attr("id")!=this.name) {
			require([ "text!applications/"+ this.name + "/template.html"], function(html) {
				$(".application").attr("id", that.name);
				$(".application").empty();
				$(".application").append(html);
				if (typeof(App.activeApp)!="undefined")
					App.activeApp.destroy();
				App.activeApp = App.apps[that.name];
				App.activeApp.renderState(state);
			});
		} else {
			this.renderState(state);
		}
	}
	
	Application.prototype.renderState = function(state) {
		console.log("WARNING: RENDERSTATE IS NOT IMPLEMENTED!!!")
	}

	return Application;
	
});