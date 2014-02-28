package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SuperFancyConcurrentPriorityQueue <T>{
	private PriorityBlockingQueue<T> q;
	//private Semaphore sem;
	private Lock read,write;
	
	public SuperFancyConcurrentPriorityQueue (/*int limit,*/ Comparator<T> comp) {
		q = new PriorityBlockingQueue<T>(20,comp);
		//sem = new Semaphore(limit);
		ReentrantReadWriteLock l = new ReentrantReadWriteLock();
		read = l.readLock();
		write = l.writeLock();
	}
	
	public void push( T elem ) throws InterruptedException {
		read.lock();
		//sem.acquire();
		q.add(elem);
		read.unlock();
	}
	
	public T poll() {
		read.lock();
		T elem = q.poll();
		//sem.release();
		read.unlock();
		return elem;
	}
	public T peek() {
		read.lock();
		T elem = q.peek();
		read.unlock();
		return elem;
	}
	public boolean remove( T elem ) {
		read.lock();
		boolean rtn = q.remove(elem);
		//if(rtn) sem.release();
		read.unlock();
		return rtn;
	}
	public T[] toArray(T a[]) {
		read.lock();
		T arr[] = q.toArray(a);
		read.unlock();
		return arr;
	}
	public void reorder() {
		write.lock();
		T elem = q.poll();
		if(null != elem) {
			q.add(elem);
		}	
		write.unlock();
	}
	public Iterator<T> getIterator() {
		read.lock();
		return q.iterator();
	}
	public void releaseIterator() {
		read.unlock();
		return;
	}
	public int size() {
		read.lock();
		int s = q.size();
		read.unlock();
		return s;
	}
}
