function listenClick(e) {
	// this function makes clicking area bigger on drop down menu
	var targ;
	if (!e) var e = window.event;
	if (e.target) targ = e.target;
	else if (e.srcElement) targ = e.srcElement;
	if (targ.nodeType == 3) // defeat Safari bug
		targ = targ.parentNode;
	if (targ.tagName == 'TD' && !targ.onclick) {
		var str = targ.parentNode;
		if (!isNaN(str.id) && (str.id > 0)) {
			freak_view(str.id);
		}
	}
}