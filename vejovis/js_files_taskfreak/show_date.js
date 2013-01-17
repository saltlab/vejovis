function show_date() {
	var clk_date = new Date();
	if (clk_date.getSeconds() == 0) { // every minute
		clk_intvl = clk_date.getTime() - clk_start;
		if (frk_reload && (clk_intvl > (frk_reload * 60000)) && (!document.forms[0].elements['id'].value)) {
			try {
				freak_rld();
			} catch (e) {}
		}
	}
	str = clk_date.toLocaleString();
	//wE(gE('userdate'),str);
	setTimeout("show_date()",1000);
}