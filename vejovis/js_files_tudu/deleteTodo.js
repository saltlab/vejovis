function deleteTodo(todoId) {
 var sure = confirm("Are you sure you want to delete this Todo?");
 if (sure) {
  hideTodosLayers();
  dwr.engine.beginBatch();
  todos.deleteTodo(todoId, replyRenderTable);
  todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
  dwr.engine.endBatch();
  tracker('/ajax/deleteTodo');
 }
}