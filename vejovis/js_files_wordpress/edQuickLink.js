function edQuickLink(i, thisSelect) {
	if (i > -1) {
		var newWin = '', tempStr;
		if (edLinks[i].newWin == 1) {
			newWin = ' target="_blank"';
		}
		tempStr = '<a href="' + edLinks[i].URL + '"' + newWin + '>'
		            + edLinks[i].display
		            + '</a>';
		thisSelect.selectedIndex = 0;
		edInsertContent(edCanvas, tempStr);
	}
	else {
		thisSelect.selectedIndex = 0;
	}
}