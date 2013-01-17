

dwr.util.setEscapeHtml(false);

// Error handling
function errorHandler(msg) {
  //alert(msg);
}
dwr.engine.setErrorHandler(errorHandler);

// Keyboard listener
var keyboardListener = "on";
document.onkeydown = function(e) {
 if(!e) e = window.event;
 if(e.keyCode==13 && keyboardListener=="on") {
  if ($("addNewTodoDiv").style.display=="inline") {
   addTodo();
  } else if ($("editTodoDiv").style.display=="inline") {
   editTodo();
  }
 }
}

// Hide the add, edit, share, ... layers.
function hideTodosLayers() {
 $("addNewTodoDiv").style.display='none';
 document.forms.addNewTodoForm.description.value = '';
 document.forms.addNewTodoForm.priority.value = '';
 document.forms.addNewTodoForm.dueDate.value = '';
 document.forms.addNewTodoForm.notes.value = '';
 $("editTodoDiv").style.display='none';
 document.forms.editTodoForm.description.value = '';
 document.forms.editTodoForm.priority.value = '';
 document.forms.editTodoForm.dueDate.value = '';
 document.forms.editTodoForm.notes.value = '';
 $("addNewListDiv").style.display='none';
 document.forms.addNewListForm.name.value = '';
 document.forms.addNewListForm.rssAllowed.checked = false;
 $("editListDiv").style.display='none';
 document.forms.editListForm.name.value = '';
 document.forms.editListForm.rssAllowed.checked = false;
 $("shareListDiv").style.display='none';
 document.forms.shareListForm.login.value = '';
}

// Show the "add a new todo" layer.
function showAddTodo() {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 createAssignedUserList('addNewTodoAssignedUser', listId);
 $("addNewTodoDiv").style.display="inline";
 document.forms.addNewTodoForm.description.focus();
 tracker('/ajax/showAddTodo');
}

// Show the "edit a todo" layer.
function showEditTodo(todoId) {
 hideTodosLayers();
 document.forms.editTodoForm.todoId.value = todoId;
 var listId = document.forms.todoForm.listId.value;
 dwr.engine.beginBatch();
 createAssignedUserList('editTodoAssignedUser', listId);
 todos.getTodoById(todoId, replyGetTodoById);
 dwr.engine.endBatch();
 $("editTodoDiv").style.display="inline";
 document.forms.editTodoForm.description.focus();
 tracker('/ajax/showEditTodo');
}

var replyGetTodoById = function(todo) {
 document.forms.editTodoForm.description.value = todo.description;
 document.forms.editTodoForm.priority.value = todo.priority;
 document.forms.editTodoForm.dueDate.value = todo.dueDate;
 if (todo.notes == null) {
  document.forms.editTodoForm.notes.value = '';
 } else {
  document.forms.editTodoForm.notes.value = todo.notes;
 }
 dwr.util.setValue('editTodoAssignedUser', todo.assignedUserLogin);
}  

function createAssignedUserList(id, listId) {
 dwr.util.removeAllOptions(id);
 dwr.util.addOptions(id, ['focarizajr']);
 dwr.util.addOptions(id, [{desc:'-- Not assigned --',id:''}],'id','desc');
 var reply = function(data) {
  dwr.util.addOptions(id, data);
 }
 todo_lists.getTodoListUsers(listId, reply);
}

//Render the main todo table.
function renderTableListId(listId) {
 hideTodosLayers();
 document.forms.todoForm.listId.value = listId;
 todos.forceRenderTodos(listId, replyRenderTable);
 tracker('/ajax/renderTableListId');
}

var tableDate;
//Render the main todo table.
function renderTable() {
 var listId = document.forms.todoForm.listId.value;
 if (tableDate == null) {
  todos.forceRenderTodos(listId, replyRenderTable);
 } else {
  todos.renderTodos(listId, tableDate, replyRenderTable);
 }
 tracker('/ajax/renderTable');
}

//Render the main todo table with the todos due in the next 4 days
function renderNextDays() {
 document.forms.todoForm.listId.value = null;
 todos.renderNextDays(replyRenderTable);
 tracker('/ajax/renderNextDays');
}

//Render the main todo table with the todos assigned to the current user
function renderAssignedToMe() {
 document.forms.todoForm.listId.value = null;
 todos.renderAssignedToMe(replyRenderTable);
 tracker('/ajax/renderAssignedToMe');
}

var replyRenderTable = function(data) {
 if (data.length > 0) {
  dwr.util.setValue('todosTable', data);
 }
 tableDate = new Date();
 document.forms.quickAddNewTodoForm.description.focus();
}

//Sort the main todo table
function sortTable(sorter) {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 todos.sortAndRenderTodos(listId, sorter, replyRenderTable);
 tracker('/ajax/sortTable');
}

//Add a todo
function addTodo() {
 var listId = document.forms.todoForm.listId.value;
 var description = document.forms.addNewTodoForm.description.value;
 var priority = document.forms.addNewTodoForm.priority.value;
 var dueDate = document.forms.addNewTodoForm.dueDate.value;
 var notes = document.forms.addNewTodoForm.notes.value;
 var assignedUser = document.forms.addNewTodoForm.assignedUser.value;
 if (validateForm(priority, dueDate, notes) != "ok") {
   return;
 }
 $("addNewTodoDiv").style.display='none';
 commonAddTodo(listId, description, priority, dueDate, notes, assignedUser);
 tracker('/ajax/addTodo');
}

//Quick add a todo
function quickAddTodo() {
 var listId = document.forms.todoForm.listId.value;
 var description = document.forms.quickAddNewTodoForm.description.value;
 if (description != "") {
  commonAddTodo(listId, description, 0, "", null, "focarizajr");
  tracker('/ajax/quickAddTodo');
 }
}

//Common function for adding a todo
function commonAddTodo(listId, description, priority, dueDate, notes, assignedUser) {
 dwr.engine.beginBatch();
 todos.addTodo(listId, description, priority, dueDate, notes, assignedUser, replyRenderTable);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 dwr.engine.endBatch();
}

//Cancel the Add Todo action
function cancelAddTodo() {
 $("addNewTodoDiv").style.display='none';
}

//Reopen a todo
function reopenTodo(todoId) {
 dwr.engine.beginBatch();
 todos.reopenTodo(todoId, replyRenderTable);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 dwr.engine.endBatch();
 tracker('/ajax/reopenTodo');
}

//Complete a todo
function completeTodo(todoId) {
 dwr.engine.beginBatch();
 todos.completeTodo(todoId, replyRenderTable);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 dwr.engine.endBatch();
 tracker('/ajax/completeTodo');
}

//Edit a todo
function editTodo() {
 var todoId = document.forms.editTodoForm.todoId.value;
 var description = document.forms.editTodoForm.description.value;
 var priority = document.forms.editTodoForm.priority.value;
 var dueDate = document.forms.editTodoForm.dueDate.value;
 var notes = document.forms.editTodoForm.notes.value;
 var assignedUser = document.forms.editTodoForm.assignedUser.value;
 if (validateForm(priority, dueDate, notes) != "ok") {
   return;
 }
 $("editTodoDiv").style.display='none';
 todos.editTodo(todoId, description, priority, dueDate, notes, assignedUser, replyRenderTable);
 tracker('/ajax/editTodo');
}

//Show the "quick edit" function for editing a todo
function showQuickEditTodo(todoId) {
 $("show-" + todoId).style.display='none';
 $("edit-" + todoId).style.display='inline';
 $("edit-in-" + todoId).focus();
}

//Quick edit a todo
function quickEditTodo(todoId, description) {
 todos.quickEditTodo(todoId, description, replyRenderTable);
 tracker('/ajax/quickEditTodo');
}

//Cancel the Edit Todo action
function cancelEditTodo() {
 $("editTodoDiv").style.display='none';
}

// Delete a todo.
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

// Delete all completed todos.
function deleteAllCompletedTodos(todoId) {
 var sure = confirm("Are you sure you want to delete all the completed Todos?");
 if (sure) {
  var listId = document.forms.todoForm.listId.value;
  hideTodosLayers();
  dwr.engine.beginBatch();
  todos.deleteAllCompletedTodos(listId, replyRenderTable);
  todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
  dwr.engine.endBatch();
  tracker('/ajax/deleteAllCompletedTodos');
 }
}

// Show the older todos.
function showOlderTodos() {
 var listId = document.forms.todoForm.listId.value;
 todos.showOlderTodos(listId, replyRenderTable);
 tracker('/ajax/showOlderTodos');
}

// Hide the older todos.
function hideOlderTodos() {
 var listId = document.forms.todoForm.listId.value;
 todos.hideOlderTodos(listId, replyRenderTable);
 tracker('/ajax/hideOlderTodos');
}

//Validate the priority and the date
function validateForm(priority, dueDate, notes) {
 if (priority != "" && !priority.match(/^\d+$/)) {
   alert("Validation error : the priority is not a number.");
   return "nok";
 }
 if (dueDate != "" && !dueDate.match(/^\d{1,2}\/\d{1,2}\/\d{1,4}$/)) {
   alert("Validation error : the due date is not a date.");
   return "nok";
 }
 if (notes.length > 10000) {
   alert("Validation error : the notes cannot be more than 10,000 characters long.");
   return "nok";
 }
 return "ok";
}
