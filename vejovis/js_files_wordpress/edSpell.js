function edSpell(myField) {
	var word = '', sel, startPos, endPos;
	if (document.selection) {
		myField.focus();
	    sel = document.selection.createRange();
		if (sel.text.length > 0) {
			word = sel.text;
		}
	}
	else if (myField.selectionStart || myField.selectionStart == '0') {
		startPos = myField.selectionStart;
		endPos = myField.selectionEnd;
		if (startPos != endPos) {
			word = myField.value.substring(startPos, endPos);
		}
	}
	if (word == '') {
		word = prompt(quicktagsL10n.wordLookup, '');
	}
	if (word !== null && /^\w[\w ]*$/.test(word)) {
		window.open('http://www.answers.com/' + escape(word));
	}
}