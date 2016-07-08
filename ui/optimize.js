 
function Optimizer(optimizeF) {
	this.lastopt = 0.0;
	this.stalepoint = 10;
	this.stalecount = 0;
	this.optf = optimizeF;
	this.done = false;
	this.intid = -1;
};
Optimizer.prototype.step = function() {
	if (this.done) {
		return this.lastopt;
	}

	var energy = this.optf();
	
	if (Math.floor(energy/10) == Math.floor(this.lastopt/10)) {
		this.stalecount++;
	} else {
		this.stalecount = 0;
	}
	if (energy < 100 || this.stalecount >= this.stalepoint) {
		this.done = true;
	}
	this.lastopt = energy;
	
	return energy;
};
Optimizer.prototype.optimize = function(delay) {
	var me = this;
	this.intid = setInterval(function() { 
		me.step(); 
		if (me.done) {
			clearInterval(me.intid);
			me.intid = -1;
		}
	}, delay);
};