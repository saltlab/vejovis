function load_content(nid, url, fun) {
    var node = document.getElementById(nid);
    if(node == null)
        return false;

    function callback(xhttp) {
        node.innerHTML = xhttp.responseText;
        if(fun != null)
            fun(xhttp);
    }

    load_data(url, callback);
}