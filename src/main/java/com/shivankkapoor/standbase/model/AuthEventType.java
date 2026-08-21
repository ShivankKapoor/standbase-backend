package com.shivankkapoor.standbase.model;

public enum AuthEventType {
    LOGIN_SUCCESS,
    LOGIN_SUCCESS_TFA,
    LOGIN_FAIL,
    CREDENTIALS_ACCEPTED,
    TFA_FAIL,
    LOGOUT,
    IP_MISMATCH,
    TFA_REPLAY_FAIL,
    SESSION_EXPIRED
}
