function freak_switch(field) {
	var g = gE(field+'_sel');
	if (isD(g))	{
		hD(g);
		ff.elements[field].selectedIndex = 0;
		sD(gE(field+'_txt'));
	} else {
		ff.elements[field+'2'].value = '';
		hD(gE(field+'_txt'));
		sD(g);
	}
}