function sortTable(sorter) {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 todos.sortAndRenderTodos(listId, sorter, replyRenderTable);
 tracker('/ajax/sortTable');
}