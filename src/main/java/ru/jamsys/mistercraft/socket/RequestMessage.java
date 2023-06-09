package ru.jamsys.mistercraft.socket;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.message.MessageImpl;

public class RequestMessage extends MessageImpl {
    @Getter
    @Setter
    SessionWrap sessionWrap;
}
