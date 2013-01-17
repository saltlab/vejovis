
 // The menu table should be refreshed automatically every 2.4 minutes.
 function reloadingMenu() {
  renderMenu();
  setTimeout('reloadingMenu();', 2.4 * 60 * 1000);
 }
 reloadingMenu();
