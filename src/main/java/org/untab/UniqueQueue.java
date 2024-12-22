package org.untab;

import java.util.*;

// Most operations are O(1) but its not thread safe
public class UniqueQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final Set<T> set = new HashSet<>();

    // Add an element to the queue (if not already present)
    public boolean add(T element) {
        if (set.add(element)) { // Add to the set, returns false if already exists
            queue.add(element); // Add to the queue
            return true;
        }
        return false;
    }

    // Remove and return the first element (FIFO)
    public T poll() {
        T element = queue.poll();
        if (element != null) {
            set.remove(element); // Remove from the set
        }
        return element;
    }

    // Peek at the first element without removing
    public T peek() {
        return queue.peek();
    }

    // Check if the queue contains an element
    public boolean contains(T element) {
        return set.contains(element);
    }

    // Check if the queue is empty
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // Get the size of the queue
    public int size() {
        return queue.size();
    }

    // Clear the entire queue and set
    public void clear() {
        queue.clear();
        set.clear();
    }
}
