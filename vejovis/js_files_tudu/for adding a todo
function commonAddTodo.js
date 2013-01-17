function for adding a todo
function commonAddTodo(listId, description, priority, dueDate, notes, assignedUser) {
 dwr.engine.beginBatch();
 todos.addTodo(listId, description, priority, dueDate, notes, assignedUser, replyRenderTable);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 dwr.engine.endBatch();
}