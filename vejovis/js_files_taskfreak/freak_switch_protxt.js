function freak_switch_protxt() {
	freak_switch('project');
	freak_mp(0);
	try {
		document.forms[0].project2.focus();
	} catch (e) { }
}