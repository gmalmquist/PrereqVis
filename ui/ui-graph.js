var GraphUI = {};

GraphUI.Init = function(graph) {
	function drawVertex(g, graph, v) {
		if (v.type != "node") {
			return;
		}
		
		var TAU = 2 * 3.14159265;
		var N = 16;
		var r = Math.floor(v.r);
		var cx = Math.floor(v.x);
		var cy = Math.floor(v.y);
		var i;
		
		g.beginPath();
		
		for (i = 0; i <= N; i++) {
			var x = r * Math.cos(TAU * i/N) + cx;
			var y = r * Math.sin(TAU * i/N) + cy;
			if (i == 0) {
				g.moveTo(x,y);
			} else {
				g.lineTo(x,y);
			}
		}
		
		var color  = v.selected ? "#0000AA" : "#000000";
		var weight = v.selected ? 2 : 1;
		
		if (!v.selected) {
			for (i = 0; i < graph.edges.length; i++) {
				var e = graph.edges[i];
				if (e.tail === v.uid || e.head === v.uid) {
					if (isEdgeSelected(graph, e.tail, e.head)) {
						color = "#5555AA";
						weight = 2;
						break;
					}
				}
			}
		}
		
		g.strokeStyle = color;
		g.fillStyle = "#FFFFFF";
		g.lineWidth = weight;
		g.fill();
		g.stroke();
		
		g.fillStyle = color;
		g.lineWidth = 1;
		g.font = v.selected ? "bold 10px sans-serif" : "10px sans-serif";
		g.textAlign = "center";
		var parts = v.name.split(" ");
		var th = 10;
		for (i = 0; i < parts.length; i++) {
			g.fillText(parts[i], cx, cy + r/2 - th*parts.length/2 + th*i);
		}
	}

	function drawEdge(g, graph, from, to) {
		g.strokeStyle = "#000000";
		
		var v0 = graph.find(from);
		var v1 = graph.find(to);
		
		var A = new Vec(v0.x, v0.y);
		var D = new Vec(v1.x, v1.y);
		
		var dist = D.d().saddv(-1, A).mag();
		var fdist = dist / graph.size.x;
		
		var perf0 = 1.0;
		var perf1 = 1.0;
		
		perf0 *= fdist*2.0;
		perf1 *= fdist*2.0;

		if (v0.type == "link" && v1.type == "link") {
			perf1 = 0.5;
			perf0 = 0.5;
			
			if (Math.abs(D.d().saddv(-1, A).dot(Vec.A_X)) < 20) {
				perf0 = 0;
				perf1 = 0;
			}
		}
		
		var B = new Vec(
			Mathf.lerps(v0.x,v1.x,0.00 - (perf0*0.1)), 
			Mathf.lerps(v0.y,v1.y,perf0 + Math.random()*0.3));
		var C = new Vec(
			Mathf.lerps(v0.x,v1.x,1.00 + (perf1*0.1)), 
			Mathf.lerps(v0.y,v1.y,1-perf1 - Math.random()*0.3));

		var selected = isEdgeSelected(graph, from, to);
		
		g.strokeStyle = selected ? "#0000AA" : "#000000";
		g.lineWidth   = selected ? 4 : 1;
		g.beginPath();
		g.moveTo(A.x, A.y);
		g.bezierCurveTo(B.x,B.y, C.x,C.y, D.x,D.y);
		g.stroke();
	}

	function selectNode(uid) {
		var node = graph.find(uid);
		if (node.selected) {
			return;
		}
		
		var txt = node.name;
		if (node.longname != "null") {
			txt += " - " + node.longname;
		}
		output(txt);
		
		node.selected = true;
		if (typeof graph.selected !== "undefined") {
			graph.selected.selected = false;
		}
		graph.selected = node;
		drawGraph();
	}

	function isLinky(v) {
		return v.type === "link" || v.name === "ANY" || v.name === "ALL";
	};

	function isEdgeSelected(graph, from, to, dir) {
		if (typeof dir === "undefined") {
			dir = "both";
		}

		var A = graph.find(from);
		var B = graph.find(to);
		var i;
		
		if (A.selected || B.selected) {
			return true;
		}
		
		var forward  = dir === "both" || dir === "forward";
		var backward = dir === "both" || dir === "backward";
		
		if (forward && isLinky(B)) {
			for (var i = 0; i < graph.edges.length; i++) {
				var e = graph.edges[i];
				if (e.tail == B.uid && isEdgeSelected(graph, e.tail, e.head, "forward")) {
					return true;
				}
			}
		}
		
		if (backward && isLinky(A)) {
			for (var i = 0; i < graph.edges.length; i++) {
				var e = graph.edges[i];
				if (e.head == A.uid && isEdgeSelected(graph, e.tail, e.head, "backward")) {
					return true;
				}
			}
		}
	}

	for (var i = 0; i < graph.vertices.length; i++) {
		graph.vertices[i].x = parseFloat(graph.vertices[i].x, 10);
		graph.vertices[i].y = parseFloat(graph.vertices[i].y, 10);
		graph.vertices[i].z = parseFloat(graph.vertices[i].z, 10);
		graph.vertices[i].r = parseFloat(graph.vertices[i].r, 10) * 2;
		graph.vertices[i].selected = false;
	}

	var map = document.getElementById("nodemap");
	var can = document.getElementById("graphCanvas");
	var g = can.getContext("2d");
	g.setTransform(1,0, 0,1, 0,0);
	var width = 1500;
	var height = 1500;
	g.clearRect(-1, -1, width+2, height+2);

	graph = new Graph(graph.vertices, graph.edges);

	graph.shift(-graph.bounds()[0].x+5, -graph.bounds()[0].y+5);
	graph._bounds = graph.bounds();
	for (var i = 0; i < graph.vertices.length; i++) {
		//graph.vertices[i].y = -graph.vertices[i].y + graph._bounds[1].y + graph.vertices[i].r;
	}
	graph._bounds = graph.bounds();
	graph.size = graph._bounds[1].d().saddv(-1, graph._bounds[0]);
	graph.scale = Math.min(width/(50.0+graph.size.x), height/(50.0+graph.size.y));
	if (graph.scale > 1.0) {
		graph.scale = 1.0;
	}
	g.scale(graph.scale, graph.scale);

	function m2v(x,y,z) {
		var v = new Vec(x,y,z);
		v.x *= graph.scale;
		v.y *= graph.scale;
		return v;
	}
	function v2m(x,y,z) {
		var v = new Vec(x,y,z);
		v.x /= graph.scale;
		v.y /= graph.scale;
		return v;
	}

	map.innerHTML = "";
	
	for (var i = 0; i < graph.vertices.length; i++) {
		var v = graph.vertices[i];
		if (v.type === "node") {
			var pos = m2v(v.x, v.y, 0);
			
			var action = "GraphUI.selectNode('" + v.uid + "')";
			var fullname = v.longname;
			if (typeof fullname === "undefined" || fullname == "null") {
				fullname = v.uid;
			}
			
			var href = " href=\"" + v.url + "\" "
							+ " target=\"blank\" ";
			if (v.url === "") {
				href = "";
			}
			
			map.innerHTML += "\n<area shape=\"circle\" coords=\"" 
							+ Math.floor(pos.x) + "," 
							+ Math.floor(pos.y) + "," 
							+ Math.floor(graph.scale * v.r) + "\""
							+ " title=\"" + fullname + "\" "
							+ " onmouseover=\"" + action + "\" " 
							+ " onmouseclick=\"" + action + "\" "
							+ href
							+ " />";
		}
	}

	function drawGraph() {
		var i;
		var edges = [];
		var vertices = [];
		
		g.clearRect(-1, -1, width/graph.scale +2, height/graph.scale +2);
		for (i = 0; i < graph.edges.length; i++) {
			var e = graph.edges[i];
			e.index = i;
			if (isEdgeSelected(graph, e.tail, e.head)) {
				edges.push(e);
			} else {
				edges.splice(0,0,e);
			}
		}
		for (i = 0; i < graph.vertices.length; i++) {
			vertices.push(graph.vertices[i]);
		}
		
		for (i = 0; i < edges.length; i++) {
			Math.seedrandom('Prerequisite Graph Random Seed-' + edges[i].index);
			drawEdge(g, graph, edges[i].tail, edges[i].head);
		}
		for (i = 0; i < vertices.length; i++) {
			drawVertex(g, graph, vertices[i]);
		}
	}
	this.drawGraph = drawGraph;
	drawGraph();
	
	
	this.drawVertex = drawVertex;
	this.drawEdge = drawEdge;
	this.graph = graph;
	this.selectNode = selectNode;
	this.isLinky = isLinky;
	this.isEdgeSelected = isEdgeSelected;
	this.drawGraph = drawGraph;
};