function renderMenu() {
 todos.getCurrentTodoLists(menuDate, replyCurrentTodoLists);
 menuDate = new Date();
 Effect.Appear("todoListsMenuBody");
 tracker('/ajax/renderMenu');
}