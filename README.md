## The Scenario
Suppose that we have an `EventGenerator` class that notifies registered `EventListener` objects when an `Event` occurs.

Typicallly the classes would look like the following code:

A hypothetical event class:

    public class Event {
    
        public final String message;
    
        public Event(String message) {
            this.message = message;
        }
    }

A hypothetical event listener interface:

    public interface EventListener {
    
        // called by the EventGenerator when an event occurs
        void anEvent(Event event);
    
        // called by the EventGenerator when other event occurs
        void otherEvent(Event event);
    
        // called by the EventGenerator when one more event occurs
        void oneMoreEvent(Event event);
    }

A hypothetical class that generates events and notifies its registered listeners:

    import java.util.LinkedList;
    import java.util.List;
    
    public class EventGenerator {
    
        // a collection os listeners
        private List<EventListener> listeners = new LinkedList<>();
    
        // method to add an listener
        public void addEventListener(EventListener listenerToAdd) {
            listeners.add(listenerToAdd);
        }
    
        // method to remove an listener
        public void removeEventListener(EventListener listenerToRemove) {
            listeners.remove(listenerToRemove);
        }
    
        // method that does something and notifies the registered listeners
        public void doSomething() {
            // perform some logic and notify an event
            Event event = new Event("An event occurred");
            for (EventListener listener : listeners) {
                listener.anEvent(event);
            }
    
            // perform more some logic and notify other event
            event = new Event("Other important thing occurred");
            for (EventListener listener : listeners) {
                listener.otherEvent(event);
            }
    
            // continue with some logic and notify one more event
            event = new Event("One more event occurred");
            for (EventListener listener : listeners) {
                listener.oneMoreEvent(event);
            }
        }
    }

And finally an example implementation that registers some listeners to the EventGenerator and handles them:

    public class Example {
    
        public static void main(String[] args) {
            Example program = new Example();
            program.execute();
        }
    
        public void execute() {
            EventGenerator object = new EventGenerator();
            object.addEventListener(firstListener);
            object.addEventListener(secondListener);
            object.addEventListener(thirdListener);
            object.doSomething();
        }
    
        // the first EventListener implementation
        private EventListener firstListener = new EventListener() {
    
            @Override
            public void anEvent(Event event) {
                System.out.println("First listener -> anEvent: " + event.message);
            }
    
            @Override
            public void otherEvent(Event event) {
                System.out.println("First listener -> otherEvent: " + event.message);
            }
    
            @Override
            public void oneMoreEvent(Event event) {
                System.out.println("First listener -> oneMoreEvent: " + event.message);
            }
        };
    
        // the second EventListener implementation
        private EventListener secondListener = new EventListener() {
    
            @Override
            public void anEvent(Event event) {
                System.out.println("Second listener -> anEvent: " + event.message);
            }
    
            @Override
            public void otherEvent(Event event) {
                System.out.println("Second listener -> otherEvent: " + event.message);
            }
    
            @Override
            public void oneMoreEvent(Event event) {
                System.out.println("Second listener -> oneMoreEvent: " + event.message);
            }
        };
    
        // the third EventListener implementation
        private EventListener thirdListener = new EventListener() {
    
            @Override
            public void anEvent(Event event) {
                System.out.println("Third listener -> anEvent: " + event.message);
            }
    
            @Override
            public void otherEvent(Event event) {
                System.out.println("Third listener -> otherEvent: " + event.message);
            }
    
            @Override
            public void oneMoreEvent(Event event) {
                System.out.println("Third listener -> oneMoreEvent: " + event.message);
            }
        };
    }

If we run the `Example` class the following output will be printed:

    First listener -> anEvent: An event occurred
    Second listener -> anEvent: An event occurred
    Third listener -> anEvent: An event occurred
    First listener -> otherEvent: Other important thing occurred
    Second listener -> otherEvent: Other important thing occurred
    Third listener -> otherEvent: Other important thing occurred
    First listener -> oneMoreEvent: One more event occurred
    Second listener -> oneMoreEvent: One more event occurred
    Third listener -> oneMoreEvent: One more event occurred

## The Problem
If we go to the `EventGenerator` class and took a look on the `doSomething` method we will see many loops that iterate through the list of `EventListener` objects.

This can work weel on simple implementations but if we have structures that generate more types of events we will need to create more loops in the code.

Another problem is that if the application is multithreaded and some thread removes or adds a `EventListener` to the `EventGenerator` while he is notifying other listeners then a `java.util.ConcurrentModificationException` will be thrown.

## The Solution
To solve that kind of problem we need to remove that loops inside the `EventGenerator` and use only one instance of `EventListener`.

*But Staroski, if I need to register more than one `EventListener`, how should I proceed without using a `List`?*

The answer is: **Polymorphism**

We need an `EventListener` implementation that works like a list of objects without being sensitive to modifications on multithreaded environments.

This can be achieved using the same strategy that Java's **Swing** and **AWT** frameworks use on their event model with the `AWTEventMulticaster` class.

Let's create our `EventMulticaster` class:

    final class EventMulticaster implements EventListener {
    
        public static EventListener add(EventListener existingEventListener, EventListener eventListenerToAdd) {
            return (EventListener) addInternal(existingEventListener, eventListenerToAdd);
        }
    
        public static EventListener remove(EventListener existingEventListener, EventListener eventListenerToRemove) {
            return (EventListener) removeInternal(existingEventListener, eventListenerToRemove);
        }
    
        private static Object addInternal(Object existingObject, Object objectToAdd) {
            if (existingObject == null) {
                return objectToAdd;
            }
            if (objectToAdd == null) {
                return existingObject;
            }
            return new EventMulticaster(existingObject, objectToAdd);
        }
    
        private static Object removeInternal(Object existingObject, Object objectToRemove) {
            if (existingObject == objectToRemove || existingObject == null) {
                return null;
            }
            if (existingObject instanceof EventMulticaster) {
                EventMulticaster tuple = (EventMulticaster) existingObject;
                if (objectToRemove == tuple.a) {
                    return tuple.b;
                }
                if (objectToRemove == tuple.b) {
                    return tuple.a;
                }
                Object a = removeInternal(tuple.a, objectToRemove);
                Object b = removeInternal(tuple.b, objectToRemove);
                if (a == tuple.a && b == tuple.b) {
                    return tuple;
                }
                return addInternal(a, b);
            }
            return existingObject;
        }
    
        private final Object a;
        private final Object b;
    
        private EventMulticaster(Object a, Object b) {
            this.a = a;
            this.b = b;
        }
    
        @Override
        public void anEvent(Event event) {
            ((EventListener) a).anEvent(event);
            ((EventListener) b).anEvent(event);
        }
    
        @Override
        public void otherEvent(Event event) {
            ((EventListener) a).otherEvent(event);
            ((EventListener) b).otherEvent(event);
        }
    
        @Override
        public void oneMoreEvent(Event event) {
            ((EventListener) a).oneMoreEvent(event);
            ((EventListener) b).oneMoreEvent(event);
        }
    }

Now let's modify the `EventGenerator` to not use `List` not use loops and have only one `EventListener` instance:

    public class EventGenerator {
    
        // this instance can represent more than one listener
        private EventListener listener;
    
        // method to add an listener
        public void addEventListener(EventListener listenerToAdd) {
            // the EventMulticaster knows when and how to enchain more listeners
            listener = EventMulticaster.add(listener, listenerToAdd);
        }
    
        // method to remove an listener
        public void removeEventListener(EventListener listenerToRemove) {
            // the EventMulticaster knows when and how to enchain more listeners
            listener = EventMulticaster.remove(listener, listenerToRemove);
        }
    
        // method that does something and notifies the registered listeners
        public void doSomething() {
            // perform some logic and notify an event
            Event event = new Event("An event occurred");
            listener.anEvent(event); // no more loops
    
            // perform more some logic and notify other event
            event = new Event("Other important thing occurred");
            listener.otherEvent(event); // no more loops
    
            // continue with some logic and notify one more event
            event = new Event("One more event occurred");
            listener.oneMoreEvent(event); // no more loops
        }
    }

As we can see the `EventGenerator` code looks much more simple and if we run the `Example` class the printed output will still be:

    First listener -> anEvent: An event occurred
    Second listener -> anEvent: An event occurred
    Third listener -> anEvent: An event occurred
    First listener -> otherEvent: Other important thing occurred
    Second listener -> otherEvent: Other important thing occurred
    Third listener -> otherEvent: Other important thing occurred
    First listener -> oneMoreEvent: One more event occurred
    Second listener -> oneMoreEvent: One more event occurred
    Third listener -> oneMoreEvent: One more event occurred

## What is this repository for?
The **multicaster** generates a class like the `EventMulticaster` shown in this hypothetical example.

The easiest way to create your own multicaster is running the GUI class: `br.com.staroski.multicaster.MulticasterGeneratorUI`.
It opens a frame where you can enter the name of your generated class and the name of the interfaces your multicaster should implement.
Yes it supports multiple interfaces.

Or can programmatically do it with the class: `br.com.staroski.multicaster.MulticasterGenerator`.
