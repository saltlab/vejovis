function freak_del_remove(id) {
	var e = gE('taskSheet');
	rs = e.rows;
	for(i=0;i<rs.length;i++) {
		if (rs[i].id == id) {
			e.deleteRow(i);
			break;
		}
	}
}