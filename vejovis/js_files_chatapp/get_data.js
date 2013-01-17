function get_data(nid, failure) {
    var node = document.getElementById(nid);
    var value = node ? node.value : failure;
    return trim(value);
}