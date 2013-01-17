function quickEditTodo(todoId, description) {
 todos.quickEditTodo(todoId, description, replyRenderTable);
 tracker('/ajax/quickEditTodo');
}