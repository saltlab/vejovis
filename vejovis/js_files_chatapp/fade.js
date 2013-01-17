function fade(element) {
    if(element == null)
        return;
    var current = 153;
    function process() {
        current += 17;
        element.style.backgroundColor = "#ffff" + color_part(current);
    }
    scheduler.steps_asap(6, 250, process);
}