function deleteAllCompletedTodos(todoId) {
 var sure = confirm("Are you sure you want to delete all the completed Todos?");
 if (sure) {
  var listId = document.forms.todoForm.listId.value;
  hideTodosLayers();
  dwr.engine.beginBatch();
  todos.deleteAllCompletedTodos(listId, replyRenderTable);
  todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
  dwr.engine.endBatch();
  tracker('/ajax/deleteAllCompletedTodos');
 }
}