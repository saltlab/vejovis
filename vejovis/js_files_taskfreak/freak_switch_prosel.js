function freak_switch_prosel() {
	freak_switch('project');
	try {
		document.forms[0].project.focus();
	} catch (e) { }
}