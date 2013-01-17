function update_fail() {
    fail_count += 1;
    var node = document.getElementById('fail');
    if(node != null) {
        node.innerHTML = fail_count.toString();
        fade(document.getElementById('success'));
    }
}