package com.art4l.dataconnector.module.dataconnector.domain.event;

// todo: this is a lot of repetition, same for the handlers
// we will likely never have any other events than "BEFORE_RECEIVE, ON_RECEIVE, AFTER_RECEIVE, BEFORE_REPLY, ON_REPLY, AFTER_REPLY" and can likely make these generic
// there should just be a way to describe a custom event that can be added just in case it's necessary
public enum GenericCommandEventType {
    BEFORE_RECEIVE,
    ON_RECEIVE,
    AFTER_RECEIVE,
    ON_REPLY,
    AFTER_REPLY;
}