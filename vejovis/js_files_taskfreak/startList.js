function startList() {
	if (document.all&&document.getElementById) {
		elems = document.getElementsByTagName("ul");
		for(var i = 0; i < elems.length; i++)
		{
			elem = elems[i];
			id = elem.id;
			cls = elem.className;
			if (id == 'nav' || cls == 'level1' || cls =='level2') {
				for (m=0; m<elem.childNodes.length; m++) {
					node = elem.childNodes[m];
					if (node.nodeName=="LI") {
						node.onmouseover=function() {
							this.className+=" over";
						}
						node.onmouseout=function() {
							this.className=this.className.replace(" over", "");
						}
					}
				}
			}
		}
	}
}