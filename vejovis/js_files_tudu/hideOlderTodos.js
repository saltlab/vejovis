function hideOlderTodos() {
 var listId = document.forms.todoForm.listId.value;
 todos.hideOlderTodos(listId, replyRenderTable);
 tracker('/ajax/hideOlderTodos');
}