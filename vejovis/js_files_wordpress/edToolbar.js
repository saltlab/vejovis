function edToolbar() {
	document.write('<div id="ed_toolbar">');
	for (var i = 0; i < edButtons.length; i++) {
		edShowButton(edButtons[i], i);
	}
	document.write('<input type="button" id="ed_spell" class="ed_button" onclick="edSpell(edCanvas);" title="' + quicktagsL10n.dictionaryLookup + '" value="' + quicktagsL10n.lookup + '" />');
	document.write('<input type="button" id="ed_close" class="ed_button" onclick="edCloseAllTags();" title="' + quicktagsL10n.closeAllOpenTags + '" value="' + quicktagsL10n.closeTags + '" />');
	document.write('<input type="button" id="ed_fullscreen" class="ed_button" onclick="fullscreen.on();" title="' + quicktagsL10n.toggleFullscreen + '" value="' + quicktagsL10n.fullscreen + '" />');
//	edShowLinks(); // disabled by default
	document.write('</div>');
}