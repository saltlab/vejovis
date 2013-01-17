function freak_edit_select(field,value) {
	sel=ff.elements[field];
	for (i=0;i<sel.options.length;i++) {
		if (sel.options[i].value == value) {
			sel.selectedIndex = i;
			break;
		}
	}
}