package Server;

import Server.Message;

import java.util.ArrayList;

public class RAFTMessagePriorityBlockingQueue<T> {
    private volatile ArrayList<Message> array;
    private volatile int top;

    public RAFTMessagePriorityBlockingQueue() {
        this.array = new ArrayList();
        this.top = -1;
    }

    public synchronized void add(Message mess) {
        boolean add = true;
        for (int i = 0; i < array.size(); i++) {
            Message m = array.get(i);
            int comp = mess.compareTo(m);
            if (comp < 0) {
                array.add(i, mess);
                add = false;
                break;
            }
        }
        if (add) array.add(mess);
        top++;
    }

    public synchronized Message remove() {
        return top >= 0 ? array.remove(top--) : null;
    }

    public synchronized Message peek() {//pode nao necessitar syncronized
        return top >= 0 ? array.get(top) : null;
    }

    public synchronized int size() {
        return top + 1;
    }

    public synchronized void clear() {
        array.clear();
        top = -1;
    }

    @Override
    public String toString() {
        return array.toString();
    }

}
