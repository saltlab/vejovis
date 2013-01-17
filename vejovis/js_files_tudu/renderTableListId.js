function renderTableListId(listId) {
 hideTodosLayers();
 document.forms.todoForm.listId.value = listId;
 todos.forceRenderTodos(listId, replyRenderTable);
 tracker('/ajax/renderTableListId');
}