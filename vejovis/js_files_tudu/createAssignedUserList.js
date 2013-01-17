function createAssignedUserList(id, listId) {
 dwr.util.removeAllOptions(id);
 dwr.util.addOptions(id, ['focarizajr']);
 dwr.util.addOptions(id, [{desc:'-- Not assigned --',id:''}],'id','desc');
 var reply = function(data) {
  dwr.util.addOptions(id, data);
 }
 todo_lists.getTodoListUsers(listId, reply);
}