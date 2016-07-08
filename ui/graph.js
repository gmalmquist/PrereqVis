function find(graph, uid) {
	if (typeof graph.lookup === "undefined") {
		graph.lookup = {};
		for (var i = 0; i < graph.vertices.length; i++) {
			graph.lookup[graph.vertices[i].uid] = graph.vertices[i];
		}
	}
	
	return graph.lookup[uid];
}

function bounds(graph) {
	var min = new Vec();
	var max = new Vec();
	
	for (var i = 0; i < graph.vertices.length; i++) {
		var r = graph.vertices[i].r*1.0;
		var x = graph.vertices[i].x*1.0;
		var y = graph.vertices[i].y*1.0;
		
		if (i == 0) {
			min.x = x-r;
			max.x = x+r;
			
			min.y = y-r;
			max.y = y+r;
		}
		
		if (x-r < min.x) { min.x = x-r; }
		if (x+r > max.x) { max.x = x+r; }
		if (y-r < min.y) { min.y = y-r; }
		if (y+r > max.y) { max.y = y+r; }
	}
	
	return [min, max];
}

function adj(graph, v) {
	var us = [];
	for (var i = 0; i < graph.edges.length; i++) {
		if (graph.edges[i].tail == v.uid) {
			us.push(graph.edges[i].head);
		}
	}
	return us;
}

function Graph(vertices, edges) {
	this.vertices = vertices;
	this.edges = edges;
};
Graph.prototype.find = function(uid) {
	return find(this, uid);
};
Graph.prototype.bounds = function() {
	return bounds(this);
};
Graph.prototype.shift = function(x,y,z) {
	var v = new Vec(x,y,z);
	
	for (var i = 0; i < this.vertices.length; i++) {
		this.vertices[i].x += v.x;
		this.vertices[i].y += v.y;
		this.vertices[i].z += v.z;
	}
};
Graph.prototype.outgoing = function(v) {
	return adj(this, v);
};
Graph.prototype.incoming = function(v) {
	var us = [];
	for (var i = 0; i < graph.edges.length; i++) {
		if (graph.edges[i].head == v.uid) {
			us.push(graph.edges[i].tail);
		}
	}
	return us;
};
Graph.prototype.adjacent = function(v) {
	var nsO = this.outgoing(v);
	var nsI = this.incoming(v);
	for (var i = 0; i < nsI.length; i++) {
		nsO.push(nsI[i]);
	}
	return nsO;
};
Graph.prototype.optimize = function() {
	var energy = 0.0;

	var G = GraphUI.graph;
	if (typeof G._optcons === "undefined") {
		G._optcons = {};
		for (var i = 0; i < G.vertices.length; i++) {
			var A = G.vertices[i];
			
			var L = A;
			var R = A;
			for (var j = 0; j < G.vertices.length; j++) {
				if (i == j) { continue; }
				
				var B = G.vertices[j];
				
				if (Math.floor(A.y) == Math.floor(B.y)) {
					if ((L.uid === A.uid || B.x > L.x) && B.x < A.x) {
						L = B;
					}
					
					if ((R.uid === A.uid || B.x < R.x) && B.x > A.x) {
						R = B;
					}
				}
			}
			
			G._optcons[A.uid] = {'L':L, 'R':R};
		}
	}
	
	// constraints
	var C = G._optcons;
	
	var toUpdate = [];
	var updateTo = [];
	
	for (var i = 0; i < G.vertices.length; i++) {
		var v = G.vertices[i];
		if (v.type !== "link") {
			continue;
		}
		
		var avg = new Vec();
		
		var ns = G.adjacent(v);
		var count = 0;
		for (var j = 0; j < ns.length; j++) {
			var n = G.find(ns[j]);
			if (n.type == "link") {
				avg.adds(n.x, n.y, n.z);
				count++;
			}
		}
		
		if (count == 0) {
			continue;
		}
		
		avg.muls(1.0/count);
		
		var pos = new Vec(v.x, v.y, 0.0);
		
		energy += Math.abs(pos.d().saddv(-1, avg).dot(Vec.A_X));
		
		pos.lerpv( 0.5, avg);
		pos.lerpv(-0.5, avg);
		
		var L = C[v.uid].L;
		var R = C[v.uid].R;
		
		if (L !== v && pos.x < L.x + L.r*2) {
			pos.x = L.x + L.r*2;
		}
		if (R !== v && pos.x > R.x - R.r*2) {
			pos.x = R.x - R.r*2;
		}
		
		toUpdate.push(v);
		updateTo.push(pos);
	}
	
	for (var i = 0; i < toUpdate.length; i++) {
		toUpdate[i].x = updateTo[i].x;
		//toUpdate[i].y = updateTo[i].y;
	}
	
	return energy;
};

