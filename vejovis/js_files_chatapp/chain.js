function chain() {
    for(var index = 0; index < arguments.length; ++index) {
        try {
            var result = arguments[index]();
            return result;
        } catch(ignore) {}
    }
}