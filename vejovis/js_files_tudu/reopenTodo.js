function reopenTodo(todoId) {
 dwr.engine.beginBatch();
 todos.reopenTodo(todoId, replyRenderTable);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 dwr.engine.endBatch();
 tracker('/ajax/reopenTodo');
}