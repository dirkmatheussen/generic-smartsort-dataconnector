package com.art4l.dataconnector.module.dataconnector.domain.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class EventEmitter {

    private Predicate<CommandEvent> filter;

    private List<EventEmitter> children;
    private List<Subscriber> subscribers;

    public EventEmitter(){
        this.children = new ArrayList<>();
        this.subscribers = new ArrayList<>();
    }

    public EventEmitter(Predicate<CommandEvent> filter){
        this();
        this.filter = filter;
    }

    public EventEmitter filter(Predicate<CommandEvent> filter){
        EventEmitter child = new EventEmitter(filter);
        this.children.add(child);
        return child;
    }

    public void emitEvent(CommandEvent event) throws Exception {
        // Get subscribers
        List<Subscriber> filteredSubscribers = getFilteredSubscribers(event);

        // Sort by priority (highest first)
        filteredSubscribers.sort(Comparator.comparing(Subscriber::getPriority));
        Collections.reverse(filteredSubscribers);

        // Call one by one
        for(Subscriber subscriber: filteredSubscribers){
            subscriber.handleEvent(event);
        }
    }

    public EventEmitter subscribe(Subscriber subscriber){
        this.subscribers.add(subscriber);
        return this;
    }

    private List<Subscriber> getFilteredSubscribers(CommandEvent event){
        List<Subscriber> filteredSubscribers = new ArrayList<>();
        if(filter == null || filter.test(event)){
            filteredSubscribers.addAll(this.subscribers);
            for(EventEmitter child: children){
                filteredSubscribers.addAll(child.getFilteredSubscribers(event));
            }
        }
        return filteredSubscribers;
    }
}