function Tree(treeId, nodes, icons, className) {
    this.nodes = nodes;
    this.treeId = treeId;
    this.icons = icons;
	this.className = className;
	this.openNodes = new Array();

    this.addNode = Tree_addNode;
    this.create = Tree_create;
    this.hasChildNode = Tree_hasChildNode;
    this.isNodeOpen = Tree_isNodeOpen;
    this.setOpenNodes = Tree_setOpenNodes;
    this.toggle = Tree_toggle;
}

function Tree_create(openNodes) {
	if (this.nodes.length > 0) {
		if (openNodes != 0 || openNodes != null) {
			this.setOpenNodes(openNodes);
		}

		var nodeValues = this.nodes[0].split("|");

		document.write("<a class=\"" + this.className + "\" href=\"" + nodeValues[6] + "\" style=\"text-decoration: none;\">");
		document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" src=\"" + this.icons[nodeValues[5]] + "\" vspace=\"0\" width=\"19\">");
		document.write("<font class=\"" + this.className + "\" size=\"1\">&nbsp;" + nodeValues[4] + "</font>");
		document.write("</a><br>");

		var recursedNodes = new Array();
		//this.addNode(0, recursedNodes);
		this.addNode(1, recursedNodes);
	}
}

function Tree_addNode(parentNode, recursedNodes) {
	for (var i = parentNode; i < this.nodes.length; i++) {
		var nodeValues = this.nodes[i].split("|");

		if (nodeValues[1] == parentNode) {
			var ls = false;
			if (nodeValues[2] == "1") {
				ls = true;
			}

			var hcn = this.hasChildNode(nodeValues[0]);
			var ino = this.isNodeOpen(nodeValues[0]);

			for (var j = 0; j < recursedNodes.length; j++) {
				if (recursedNodes[j] != 1) {
					document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" src=\"" + this.icons[1] + "\" vspace=\"0\" width=\"19\">");
				}
				else {
					document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" src=\"" + this.icons[2] + "\" vspace=\"0\" width=\"19\">");
				}
			}

			// Line and empty icons

			if (ls) {
				recursedNodes.push(0);
			}
			else {
				recursedNodes.push(1);
			}

			// Write out join icons

			if (hcn) {
				if (ls) {
					document.write("<a class=\"" + this.className + "\" href=\"javascript: " + this.treeId + ".toggle('" + this.treeId + "', " + nodeValues[0] + ", 1);\" style=\"text-decoration: none;\">");
					document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" id=\"" + this.treeId + "join" + nodeValues[0] + "\" src=\"");

					if (ino) {
						document.write(this.icons[6]);	// minus.gif
					}
					else {
						document.write(this.icons[8]);	// plus.gif
					}

					document.write("\" vspace=\"0\" width=\"19\"></a>");
				}
				else {
					document.write("<a class=\"" + this.className + "\" href=\"javascript: " + this.treeId + ".toggle('" + this.treeId + "', " + nodeValues[0] + ", 0);\" style=\"text-decoration: none;\">");
					document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" id=\"" + this.treeId + "join" + nodeValues[0] + "\" src=\"");

					if (ino) {
						document.write(this.icons[5]);	// minus.gif
					}
					else {
						document.write(this.icons[7]);	// plus.gif
					}

					document.write("\" vspace=\"0\" width=\"19\"></a>");
				}
			}
			else {
				if (ls) {
					document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" src=\"" + this.icons[3] + "\" vspace=\"0\" width=\"19\">");
				}
				else {
					document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" src=\"" + this.icons[4] + "\" vspace=\"0\" width=\"19\">");
				}
			}

			// Link

			document.write("<a class=\"" + this.className + "\" href=\"" + nodeValues[6] + "\" style=\"text-decoration: none;\">");

			if (hcn) {
				document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" id=\"" + this.treeId + "icon" + nodeValues[0] + "\" src=\"")

				if (ino) {
					document.write(this.icons[10]); // folder_open.gif
				}
				else {
					document.write(this.icons[9]);	// folder.gif
				}

				document.write("\" vspace=\"0\" width=\"19\">");
			}
			else {
				document.write("<img align=\"absmiddle\" border=\"0\" height=\"20\" hspace=\"0\" id=\"" + this.treeId + "icon" + nodeValues[0] + "\" src=\"" + this.icons[nodeValues[5]] + "\" vspace=\"0\" width=\"19\">");
			}

			document.write("<font class=\"" + this.className + "\" size=\"1\">&nbsp;");
			document.write(nodeValues[4]);
			document.write("</font></a><br>");

			// Recurse if node has children

			if (hcn) {
				document.write("<div id=\"" + this.treeId + "div" + nodeValues[0] + "\"");

				if (!ino) {
					document.write(" style=\"display: none;\"");
				}

				document.write(">");

				this.addNode(nodeValues[0], recursedNodes);

				document.write("</div>");
			}

			// Pop last line or empty icon

			recursedNodes.pop();
		}
	}
}

function Tree_hasChildNode(parentNode) {
	if (parentNode >= this.nodes.length) {
		return false;
	}

	var nodeValues = this.nodes[parentNode].split("|");

	if (nodeValues[1] == parentNode) {
		return true;
	}

	return false;
}

function Tree_isNodeOpen(node) {
	for (i = 0; i < this.openNodes.length; i++) {
		if (this.openNodes[i] == node) {
			return true;
		}
	}

	return false;
}

function Tree_setOpenNodes(openNodes) {
	for (i = 0; i < this.nodes.length; i++) {
		var nodeValues = this.nodes[i].split("|");

		if (nodeValues[0] == openNodes) {
			this.openNodes.push(nodeValues[0]);
			this.setOpenNodes(nodeValues[1]);
		}
	}
}

function Tree_toggle(treeId, node, bottom) {
	var divEl = document.getElementById(treeId + "div" + node);
	var joinEl	= document.getElementById(treeId + "join" + node);
	var iconEl = document.getElementById(treeId + "icon" + node);

	if (divEl.style.display == "none") {
		if (bottom == 1) {
			joinEl.src = this.icons[6];	// minus_bottom.gif
		}
		else {
			joinEl.src = this.icons[5]; // minus.gif
		}

		iconEl.src = this.icons[10];	// folder_open.gif
		divEl.style.display = "";
	}
	else {
		if (bottom == 1) {
			joinEl.src = this.icons[8];	// plus.gif
		}
		else {
			joinEl.src = this.icons[7]; // plus.gif
		}

		iconEl.src = this.icons[9];		// folder.gif
		divEl.style.display = "none";
	}

	self.focus();
}

if (!Array.prototype.push) {
	function array_push() {
		for(var i = 0; i < arguments.length; i++) {
			this[this.length] = arguments[i];
		}

		return this.length;
	}

	Array.prototype.push = array_push;
}

if (!Array.prototype.pop) {
	function array_pop(){
		lastElement = this[this.length - 1];
		this.length = Math.max(this.length - 1, 0);

		return lastElement;
	}

	Array.prototype.pop = array_pop;
}