function completeTodo(todoId) {
 dwr.engine.beginBatch();
 todos.completeTodo(todoId, replyRenderTable);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 dwr.engine.endBatch();
 tracker('/ajax/completeTodo');
}