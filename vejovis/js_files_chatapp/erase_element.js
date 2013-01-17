function erase_element(name) {
    var node = document.getElementById(name);
    if(node != null) {
        node.parentNode.removeChild(node);
    }
}