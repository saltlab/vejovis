function renderTable() {
 var listId = document.forms.todoForm.listId.value;
 if (tableDate == null) {
  todos.forceRenderTodos(listId, replyRenderTable);
 } else {
  todos.renderTodos(listId, tableDate, replyRenderTable);
 }
 tracker('/ajax/renderTable');
}