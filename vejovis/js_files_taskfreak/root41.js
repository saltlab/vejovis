
var xajaxRequestUri="xajax.task.php";
var xajaxDebug=false;
var xajaxStatusMessages=false;
var xajaxWaitCursor=true;
var xajaxDefinedGet=0;
var xajaxDefinedPost=1;
var xajaxLoaded=false;
function xajax_staying_alive(){return xajax.call("staying_alive", arguments, 1);}
function xajax_task_load_view(){return xajax.call("task_load_view", arguments, 1);}
function xajax_task_load_more(){return xajax.call("task_load_more", arguments, 1);}
function xajax_task_load_edit(){return xajax.call("task_load_edit", arguments, 1);}
function xajax_task_load_users(){return xajax.call("task_load_users", arguments, 1);}
function xajax_task_update_status(){return xajax.call("task_update_status", arguments, 1);}
function xajax_task_update_full(){return xajax.call("task_update_full", arguments, 1);}
function xajax_task_delete(){return xajax.call("task_delete", arguments, 1);}
function xajax_task_comment_edit(){return xajax.call("task_comment_edit", arguments, 1);}
function xajax_task_comment_delete(){return xajax.call("task_comment_delete", arguments, 1);}
function xajax_task_update_comment(){return xajax.call("task_update_comment", arguments, 1);}
	