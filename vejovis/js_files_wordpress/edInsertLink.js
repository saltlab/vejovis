function edInsertLink(myField, i, defaultValue) {
	if ( 'object' == typeof(wpLink) ) {
		wpLink.open();
	} else {
		if (!defaultValue) {
			defaultValue = 'http://';
		}
		if (!edCheckOpenTags(i)) {
			var URL = prompt(quicktagsL10n.enterURL, defaultValue);
			if (URL) {
				edButtons[i].tagStart = '<a href="' + URL + '">';
				edInsertTag(myField, i);
			}
		}
		else {
			edInsertTag(myField, i);
		}
	}
}