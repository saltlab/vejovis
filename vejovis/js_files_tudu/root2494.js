
//Render the menu
var menuDate;
function renderMenu() {
 todos.getCurrentTodoLists(menuDate, replyCurrentTodoLists);
 menuDate = new Date();
 Effect.Appear("todoListsMenuBody");
 tracker('/ajax/renderMenu');
}
        
var replyCurrentTodoLists = function(data) {
 if (data != null) {
   dwr.util.removeAllRows("todoListsMenuBody");
   dwr.util.addRows("todoListsMenuBody", data, [ selectTodoListLink ]);
 }
}

function selectTodoListLink(data) {
 return "<a href=\"javascript:renderTableListId('"
  + data.listId + "')\">" + data.description + "</a>";
}

// Show the "add a new todo list" layer.
function showAddTodoList() {
 hideTodosLayers();
 $("addNewListDiv").style.display="inline";
 document.forms.addNewListForm.name.focus();
}
        
//Cancel the Add Todo List action
function cancelAddTodoList() {
 $("addNewListDiv").style.display='none';
}

//Add a todo list
function addTodoList(name) {
 var name = document.forms.addNewListForm.name.value;
 var rssAllowed = 0;
 if (document.forms.addNewListForm.rssAllowed.checked) {
  rssAllowed = 1;
 }
 $("addNewListDiv").style.display='none';
 dwr.engine.beginBatch();
 todo_lists.addTodoList(name, rssAllowed);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 dwr.engine.endBatch();
 tracker('/ajax/addTodoList');
}

// Show the "edit a todo list" layer.
function showEditTodoList() {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 if (listId != null && listId != "null" &&  listId != "") {
  $("editListDiv").style.display="inline";
  todo_lists.getTodoList(listId, replyEditTodoList);
  document.forms.editListForm.name.focus();
  tracker('/ajax/showEditTodoList');
 }
}

var replyEditTodoList = function(list) {
 document.forms.editListForm.name.value = list.name;
 var rssAllowed = list.rssAllowed;
 if (rssAllowed == 1) {
  document.forms.editListForm.rssAllowed.checked = true;
 } else {
  document.forms.editListForm.rssAllowed.checked = false;
 }
}

// Edit the current todo list.
function editTodoList() {
 var listId = document.forms.todoForm.listId.value;
 var name = document.forms.editListForm.name.value;
 var rssAllowed = 0;
 if (document.forms.editListForm.rssAllowed.checked) {
  rssAllowed = 1;
 }
 $("editListDiv").style.display='none';
 dwr.engine.beginBatch();
 todo_lists.editTodoList(listId, name, rssAllowed);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 renderTable();
 dwr.engine.endBatch();
 tracker('/ajax/editTodoList');
}

//Cancel the edit the current list action
function cancelEditTodoList() {
 $("editListDiv").style.display='none';
}

// Show the "share a todo list" layer.
function showShareTodoList() {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 if (listId != null && listId != "null" &&  listId != "") {
  $("shareListDiv").style.display="inline";
  todo_lists.getTodoListUsers(listId, replyShareTodoListUsers);
  document.forms.shareListForm.login.focus();
  tracker('/ajax/showShareTodoList');
 }
}

var replyShareTodoListUsers = function(data) {
 dwr.util.removeAllRows("usersTableBody");
 dwr.util.addRows("usersTableBody", data, [ direct, deleteTodoListUserLink ]);
}

function direct(data) {
 return data;
}

function deleteTodoListUserLink(data) {
 return "<a href=\"javascript:deleteTodoListUser('" + data + "')\">Remove</a>";
}

// Add a TodoList user.
function addTodoListUser() {
 var listId = document.forms.todoForm.listId.value;
 var login = document.forms.shareListForm.login.value;
 dwr.engine.beginBatch();
 todo_lists.addTodoListUser(listId, login, replyAddTodoListUser);
 todo_lists.getTodoListUsers(listId, replyShareTodoListUsers);
 dwr.engine.endBatch();
 tracker('/ajax/addTodoListUser');
}

var replyAddTodoListUser = function(data) {
 if (data != null && typeof data == 'object') {
  alert(dwr.util.toDescriptiveString(data, 2));
 } else {
  if (data == "ObjectRetrievalFailureException") {
   alert("Requested user does not exist.");
  }
 }
}

// Delete a TodoList user.
function deleteTodoListUser(login) {
 var listId = document.forms.todoForm.listId.value;
 dwr.engine.beginBatch();
 todo_lists.deleteTodoListUser(listId, login);
 todo_lists.getTodoListUsers(listId, replyShareTodoListUsers);
 dwr.engine.endBatch();
 tracker('/ajax/deleteTodoListUser');
}

// Delete the current todo list.
function deleteTodoList(listId) {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 if (listId != null && listId != "null" &&  listId != "") {
  var sure = confirm("Are you sure you want to delete this Todo List?");
  if (sure) {
   dwr.engine.beginBatch();
   todo_lists.deleteTodoList(listId);
   todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
   document.forms.todoForm.listId.value = null;
   dwr.util.setValue('todosTable', 
    "<div class='message'>Todo List successfully deleted.</div>");
   dwr.engine.endBatch();
   tracker('/ajax/deleteTodoList');
  }
 }
}
