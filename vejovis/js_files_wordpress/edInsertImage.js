function edInsertImage(myField) {
	var myValue = prompt(quicktagsL10n.enterImageURL, 'http://');
	if (myValue) {
		myValue = '<img src="'
				+ myValue
				+ '" alt="' + prompt(quicktagsL10n.enterImageDescription, '')
				+ '" />';
		edInsertContent(myField, myValue);
	}
}