package brooklyn.management.internal;

import java.util.List
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import brooklyn.entity.Entity
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.event.SensorEvent
import brooklyn.event.EventListener
import brooklyn.event.basic.EventFilters
import brooklyn.event.basic.BasicSensorEvent
import brooklyn.management.ExecutionManager;
import brooklyn.management.SubscriptionManager
import brooklyn.util.task.BasicExecutionManager;

import com.google.common.base.Objects
import com.google.common.base.Predicate

/**
 * A {@link SubscriptionManager} that stores subscription details locally.
 */
public class LocalSubscriptionManager implements SubscriptionManager {
    private static class Subscription {
        //FIXME subs this should be populated, typically with the subscribing entity, but with tag-type TBD in cases of remote subscriptions (webapp, tests)  
        public Collection subscriberTags = []
        
        public Predicate<Entity> entities;
        public Predicate<BasicSensorEvent<?>> filter;
        public EventListener<?> listener;

        public Subscription(Predicate<Entity> entities, Predicate<BasicSensorEvent<?>> filter, EventListener<?> listener) {
            this.entities = entities;
            this.filter = filter;
            this.listener = listener;
        }
        
        @Override
        public boolean equals(Object other) {
            // FIXME proper equals
            return Objects.equal(this, other);
        }
 
        @Override
        public int hashCode() {
            return Objects.hashCode(entities, filter, listener);
        }
    }
    
    AtomicLong subscriptionId = new AtomicLong(0L);
    ConcurrentMap<Long, Subscription> allSubscriptions = new ConcurrentHashMap<Long, Subscription>();
    
    final ExecutionManager em;
    
    public LocalSubscriptionManager(ExecutionManager em) { this.em = em }
    
    public <T> void publish(SensorEvent<T> event) {
        // FIXME SUBS - execution - see below
        
        // now run in new thread in case listeners are blocked;
        
        // FIXME should be given execution manager when instantiated
        
        // FIXME for any subscriber, should define SingleThreadedExecution for any subscriber ID tag
        // in order to ensure callbacks are invoked in the order they are submitted
        // (recommend exactly one per subscription to prevent deadlock)
        // this is done with:
        // em.setTaskPreprocessorForTag(subscriberId, SingleThreadedExecution.class);
        
        //note, generating the notifications must be done in the calling thread to preserve order
        //e.g. emit(A); emit(B); should cause onEvent(A); onEvent(B) in that order
        allSubscriptions.each { key, Subscription s -> 
            if (s.filter.apply(event) && (!s.entities || s.entities.apply(event))) 
                em.submit(tags: s.subscriberTags, { s.listener.onEvent(event) })
        }
    }

    //FIXME should take tags for subscribers
    public <T> long subscribe(String producerId, String sensorName, EventListener<T> listener) {
        Subscription sub = new Subscription(EventFilters.entityId(producerId), EventFilters.sensorName(sensorName), listener);
        long id = subscriptionId.incrementAndGet();
        allSubscriptions.put(id, sub);
        return id;
    }

    public void unsubscribe(long subscriptionId) {
        allSubscriptions.remove(subscriptionId);
    }
}
