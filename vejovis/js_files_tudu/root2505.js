
 // The todos table should be refreshed automatically every 2 minutes.
 function reloadingTable() {
  renderTable();
  setTimeout('reloadingTable();', 2 * 60 * 1000);
 }
 reloadingTable();
