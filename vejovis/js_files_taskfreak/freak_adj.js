function freak_adj(idx) {
	vn = 'status'+idx;
	if (!ff.elements[vn].checked) {
		idx--;
	}
	for (i=1; i<=idx; i++) {
		vn = 'status'+i;
		ff.elements[vn].checked = true;
	}
	for (i=idx+1; i<6; i++) {
		vn = 'status'+i;
		ff.elements[vn].checked = false;
	}
}