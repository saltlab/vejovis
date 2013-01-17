function update_ok() {
    ok_count += 1;
    var node = document.getElementById('ok');
    if(node != null) {
        node.innerHTML = ok_count.toString();
        fade(document.getElementById('success'));
    }
}