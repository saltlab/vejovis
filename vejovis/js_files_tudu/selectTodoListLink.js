function selectTodoListLink(data) {
 return "<a href=\"javascript:renderTableListId('"
  + data.listId + "')\">" + data.description + "</a>";
}