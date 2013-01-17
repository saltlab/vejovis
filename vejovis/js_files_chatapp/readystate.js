function readystate(xhttp, fun) {
    if(xhttp.readyState == 4) {
        if(xhttp.status == 200) {
            fun(xhttp);
            if(window.scheduler != null)
                window.scheduler.asap(update_ok);
        } else {
            if(window.scheduler != null)
                window.scheduler.asap(update_fail);
        }
    }
}