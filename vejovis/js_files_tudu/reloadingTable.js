function reloadingTable() {
  renderTable();
  setTimeout('reloadingTable();', 2 * 60 * 1000);
 }