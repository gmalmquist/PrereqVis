var Mathf = {};
Mathf.lerps = function(a, b, s) { 
	return a + s*(b-a); 
};
Mathf.lerpv = function(a, b, s) { 
	return new Vec(
		this.lerps(a.x,b.x, s),
		this.lerps(a.y,b.y, s),
		this.lerps(a.z,b.z, s)
	);
};

function Vec(x,y,z) {
	if (typeof x === "undefined") {
		x = 0.0;
	}
	if (typeof y === "undefined") {
		y = 0.0;
	}
	if (typeof z === "undefined") {
		z = 0.0;
	}
	
	if (typeof x === "number" || typeof x === "string") {
		this.x = parseFloat(x, 10);
		this.y = parseFloat(y, 10);
		this.z = parseFloat(z, 10);
	} else {
		this.x = x.x;
		this.y = x.y;
		this.z = x.z;
	}
};

// methods
Vec.prototype.d = function() {
	return new Vec(this.x, this.y, this.z);
};
Vec.prototype.adds = function(x,y,z) {
	this.x += x;
	this.y += y;
	this.z += z;
	return this;
};
Vec.prototype.addv = function(v) {
	return this.adds(v.x, v.y, v.z);
};
Vec.prototype.sadds = function(s, x, y, z) {
	return this.adds(s*x, s*y, s*z);
};
Vec.prototype.saddv = function(s, v) {
	return this.adds(s*v.x, s*v.y, s*v.z);
};
Vec.prototype.sets = function(x, y, z) {
	this.x = x;
	this.y = y;
	this.z = z;
	return this;
};
Vec.prototype.setv = function(v) {
	return this.sets(v.x, v.y, v.z);
};
Vec.prototype.muls = function(x,y,z) {
	if (typeof y === "undefined") {
		y = x;
	}
	if (typeof z === "undefined") {
		z = x;
	}
	this.x *= x;
	this.y *= y;
	this.z *= z;
	return this;
}
Vec.prototype.dot = function(v) {
	return this.x*v.x + this.y*v.y + this.z*v.z;
};
Vec.prototype.mag2 = function() {
	return this.dot(this);
};
Vec.prototype.mag = function() {
	return Math.sqrt(this.mag2());
};
Vec.prototype.lerps = function(s, x,y,z) {
	this.x += s*(x-this.x);
	this.y += s*(y-this.y);
	this.z += s*(z-this.z);
	return this;
};
Vec.prototype.lerpv = function(s, v) {
	return this.lerps(s, v.x, v.y, v.z);
};
Vec.prototype.scross = function(i, j, k) {
	var x = this.x;
	var y = this.y;
	var z = this.z;
	
	this.x =  (y*k - j*z);
	this.y = -(x*k - i*z);
	this.z =  (x*j - i*y);
	
	return this;
};
Vec.prototype.vcross = function(v) {
	return this.scross(v.x, v.y, v.z);
};
Vec.prototype.toString = function() {
	return "<" + Math.floor(100*this.x)/100.0 + ", " + Math.floor(100*this.y)/100.0 + ", " + Math.floor(100*this.z)/100.0 + ">";
};

// globals
Vec.A_X = new Vec(1,0,0);
Vec.A_Y = new Vec(0,1,0);
Vec.A_Z = new Vec(0,0,1);