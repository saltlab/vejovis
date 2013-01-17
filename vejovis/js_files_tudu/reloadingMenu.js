function reloadingMenu() {
  renderMenu();
  setTimeout('reloadingMenu();', 2.4 * 60 * 1000);
 }