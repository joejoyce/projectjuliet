package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SuperFancyConcurrentPriorityQueue <T extends Comparable<T>>{
	private LinkedBlockingDeque<T> q;
	private PriorityBlockingQueue<T>bq;
	//private Semaphore sem;
	private Lock read,write;
	
	public SuperFancyConcurrentPriorityQueue (/*int limit,*/ Comparator<T> comp, Comparator <T> toArrComp) {
		q = new LinkedBlockingDeque<T>(20);//,comp);
		bq = new PriorityBlockingQueue<T>(16,toArrComp);
		//sem = new Semaphore(limit);
		ReentrantReadWriteLock l = new ReentrantReadWriteLock();
		read = l.readLock();
		write = l.writeLock();
	}
	
	public void push( T elem ) throws InterruptedException {
		//read.lock();
		//sem.acquire();
		q.put(elem);
		bq.add(elem);
		//read.unlock();
	}
	
	/*public T poll() {
		read.lock();
		T elem = q.poll();
		//sem.release();
		read.unlock();
		return elem;
	}*/
	public T peek() {
		write.lock();
		T a = q.poll(), b = q.poll(), rtn = null;
		if(null != a && null != b) {
			try {
				if(a.compareTo(b) <= 0) {
					rtn = a;
					q.put(b);
					q.putFirst(a);
				} else {
					rtn = b;
					q.put(a);
					q.putFirst(b);
				}
					
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if( null == b){
			if(null != a) {
				try {
					rtn = a;
					q.put(a);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		write.unlock();
		return rtn;
	}
	public boolean remove( T elem ) {
		read.lock();
		boolean rtn = q.remove(elem);
		bq.remove(elem);
		//if(rtn) sem.release();
		read.unlock();
		return rtn;
	}
	public T[] toArray(T a[]) {
		//read.lock();
		T arr[] = bq.toArray(a);
		//read.unlock();
		return arr;
	}
	public Iterator<T> iterator() {
		//read.lock();
		return bq.iterator();
	}
	/*public void releaseIterator() {
		read.unlock();
		return;
	}*/
	public int size() {
	//	read.lock();
		int s = bq.size();
	//	read.unlock();
		return s;
	}
}
