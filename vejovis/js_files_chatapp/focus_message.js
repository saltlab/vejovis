function focus_message() {
    var node = document.getElementById('myvoice');
    if(node != null)
        node.focus();
    return node;
}