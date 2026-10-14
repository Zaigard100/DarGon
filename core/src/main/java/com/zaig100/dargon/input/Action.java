package com.zaig100.dargon.input;

/** Абстрактные действия игрока. Системы и UI видят только их, а не клавиши и кнопки. */
public enum Action {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    USE,
    INVENTORY,
    DROP,
    PAUSE,
    DEBUG
}
