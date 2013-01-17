function quickAddTodo() {
 var listId = document.forms.todoForm.listId.value;
 var description = document.forms.quickAddNewTodoForm.description.value;
 if (description != "") {
  commonAddTodo(listId, description, 0, "", null, "focarizajr");
  tracker('/ajax/quickAddTodo');
 }
}