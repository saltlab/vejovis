function focus_message_channel() {
    return (current_channel == default_channel) ? focus_channel() : focus_message();
}