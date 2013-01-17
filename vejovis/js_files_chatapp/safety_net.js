function safety_net(f) {
    try
    {
        return f();
    }
    catch(ignore) {}
}