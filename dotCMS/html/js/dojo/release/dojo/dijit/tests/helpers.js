// Helper methods for automated testing

function isVisible(node){
		if(node.domNode){ node = node.domNode; }
		return (dojo.style(node, "display") != "none") &&
				(dojo.style(node, "visibility") != "hidden") &&
				(dojo.position(node).y >= 0);
}

function isHidden(node){
		if(node.domNode){ node = node.domNode; }
		return (dojo.style(node, "display") == "none") ||
				(dojo.style(node, "visibility") == "hidden") ||
				(dojo.position(node).y < 0);
}

function innerText(node){
	return node.textContent || node.innerText || "";
}
