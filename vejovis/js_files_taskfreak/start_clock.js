function start_clock() {
	clk_date = new Date();
	clk_start = clk_date.getTime();
	show_date();
	setTimeout("show_date()",1000);
}