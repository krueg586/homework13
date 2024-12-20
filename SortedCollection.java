package edu.uwm.cs351;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SortedCollection<E> extends AbstractCollection<E> {

	private static class Node<E> {
		E data;
		Node<E> next;
		Node(E d, Node<E> n) { data = d; next = n; }
	}
	
	private Comparator<E> comparator;
	private Node<E> tail;
	private int size;
	private int version;
	
	
	/// private helper methods:
	
	/*
	 * These methods work with CLLs.
	 * An CLL is cyclic singly-linked lists with a dummy node.
	 * An CLL is identified by its TAIL (not the head or dummy!)
	 * An CLL is never null, even if empty. 
	 */

	/**
	 * Create a CLL from the elements in the given collection in the same order.
	 * @param dummy dummy node to use, if null, a new dummy will be created
	 * @param coll collection to use, must not be null, but may be empty
	 * @return CLL of elements in same order
	 */
	private Node<E> toCLL(Node<E> dummy, Collection<? extends E> coll) {
		if (dummy == null) dummy = new Node<E>(null,null);
		dummy.next = dummy;
		Node<E> t = dummy;
		for (E e : coll) {
			t = t.next = new Node<E>(e,dummy);
		}
		return t;
	}
	
	/**
	 * Convert a CLL to a string.  
	 * A problem is signified by a string without a proper ending paren.
	 * Use this for debugging.
	 * @param tail CLL
	 * @return string picture of a CLL
	 */
	private static <E> String CLLtoString(Node<E> tail) {
		if (tail == null) return "<NULL>";
		Node<E> dummy = tail.next;
		if (dummy == tail) return "()";
		if (dummy == null) return "<NO DUMMY>";
		if (dummy.data != null) return "<DUMMY " + dummy.data + ">";
		Node<E> head = dummy.next;
		StringBuilder sb = new StringBuilder();
		Node<E> fast = head.next;
		while (head != dummy) {
			if (head == null) return sb.toString(); // no closing paren
			if (head == fast) {
				System.out.println("...");
				return sb.toString();
			}
			if (sb.length() == 0) sb.append("(");
			else sb.append(",");
			sb.append(head.data);
			head = head.next;
			if (fast != dummy && fast != null) fast = fast.next;
			if (fast != dummy && fast != null) fast = fast.next;
		}
		sb.append(")"); // clean close
		return sb.toString();
	}
	
	/**
	 * Merge two sorted CLLs, or rather, merge the elements
	 * of the second list into the first list.
	 * This method should not create any new nodes!
	 * The lists may have duplicates (according to the comparator)
	 * but will be in non-decreasing order w.r.t. the comparator.
	 * The result should include the dummy from the first CLL
	 * and the second CLL should be left empty (dummy points to itself).
	 * @note This method does not efficiently handle when the second list
	 * contains a single element that belongs at the end of the first list.
	 * In other words, this method does not do the work of insertion sort. 
	 * @param t1 tail of first CLL
	 * @param t2 tail of second CLL
	 * @return tail of merged list (first CLL with nodes of second CLL merged in)
	 */
	private Node<E> merge(Node<E> t1, Node<E> t2) {
		if (t2.next == t2) return t1;
		Node<E> dummy = t1.next;
		Node<E> dummy2 = t2.next;
		Node<E> current = dummy;
		Node<E> li1 = dummy.next;
		Node<E> li2 = dummy2.next;
		t2.next.next = t2.next;
		t1.next.next = t1.next;
		while(li1 != dummy && li2 != dummy2) {
			int comp = comparator.compare(li1.data, li2.data);
			if (comp <= 0) {
				current.next = li1;
				li1 = li1.next;
			}
			else {
				current.next = li2;
				li2 = li2.next;
			}
			current = current.next;
		}
		if (li1 == dummy) {
			current.next = li2;
			t2.next = dummy;
			t1 = t2;
		}
		else if(li2 == dummy2) {
			current.next = li1;
			t1.next = dummy;
		}
		t2 = dummy2;
		return t1; 
	}
	
	/**
	 * Partition a CLL w.r.t. the first element in the list.
	 * The list elements are rearranged so that those which
	 * are less that the pivot are placed before it, and those
	 * that are greater are placed after them.  Equal elements
	 * will be placed in the "after" list right after the pivot.  
	 * The elements may be rearranged arbitrarily otherwise.
	 * @param tail the last element of the CLL. 
	 * There must be at least one element in this list
	 * @return new tail
	 */
	private Node<E> partition(Node<E> tail) {
		if (tail == null || tail.next.next == tail || tail.next == tail) return tail;
		Node<E> pivot = tail.next.next;
		Node<E> pivotTail = pivot;
		Node<E> dummy = tail.next;
		Node<E> current = pivot.next;
		Node<E> fin = dummy;
		tail.next = tail = null;
		Node<E> largerStart = tail;
		dummy.next = pivot.next = null;
		while(current != null) {
			Node<E> save = current.next;
			int comp = comparator.compare(current.data, pivot.data);
			if (comp < 0) {
				fin.next = current;
				current.next = null;
				fin = current;
			}
			else if (comp > 0) {
				if (tail == null) largerStart = tail = current;
				tail.next = current;
				tail = current;
				tail.next = null;
			}
			else {
				pivotTail.next = current;
				pivotTail = current;
				pivotTail.next = null;
			}
			current = save;
		}
		fin.next = pivot;
		if (tail == null) tail = pivotTail;
		else pivotTail.next = largerStart;
		tail.next = dummy;
		return tail;
	}
	
	/**
	 * Destructively sort a CLL using quicksort, and return it.
	 * The pivot chose should always be the first element.
	 * @param l CLL identified by its tail
	 */
	private Node<E> quicksort(Node<E> tail) {
		if (tail.next.next == tail || tail.next == tail) return tail;		
		Node<E> pivot = tail.next.next,
				dummy = tail.next;
		tail = partition(tail);
		Node<E> smallerEnd = dummy,
				pivotEnd = pivot;
		for (Node<E> c = dummy.next; c != dummy; c = c.next) {
			int comp = comparator.compare(c.data, pivot.data);
			if (comp < 0) smallerEnd = c;
			else if (comp == 0) pivotEnd = c;
			else break;
		}
		if (smallerEnd != dummy) {
			smallerEnd.next = dummy;
			smallerEnd = quicksort(smallerEnd);
			smallerEnd.next = pivot;
		}
		if (pivotEnd.next != dummy) {
			Node<E> saveStart = dummy.next;
			dummy.next = pivotEnd.next;
			tail = quicksort(tail);
			pivotEnd.next = dummy.next;
			dummy.next = saveStart;
		}
		return tail;
	}
	
	private boolean report(String message) {
		System.out.println("Invariant error: " + message);
		return false;
	}
	
	private boolean wellFormed() {
		if (comparator == null) return report("null comparator");
		if (tail == null) return report("no dummy");
		Node<E> dummy = tail.next;
		if (dummy == null) return report("null dummy");
		if (dummy.data != null) return report("dummy has real data");
		int count = 0;
		for (Node<E> p = dummy.next; p != tail.next; p = p.next) {
			if (p == null || p.next == null) return report("found null (not cyclic)");
			if (p.data == null) return report("found null data");
			if (++count > size) {
				return report("too many nodes (bad cycle?)");
			}
			if (p != tail && comparator.compare(p.data, p.next.data) > 0) {
				return report("found out of order: " + p.data + " and " + p.next.data);
			}
		}
		if (count != size) return report("size wrong: claimed " + size + " but has " + count + " elements.");
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public SortedCollection() {
		this((o1,o2) -> ((Comparable<E>)o1).compareTo(o2));
		assert wellFormed() : "invariant failed at end of constructor";
	}
	
	/**
	 * Create a sorted collection using the given comparator.
	 * @param comp comparator to use, must not be null
	 */
	public SortedCollection(Comparator<E> comp) {
		if (comp == null) throw new IllegalArgumentException("comparator cannot be null");
		tail = new Node<E>(null, null);
		tail.next = tail;
		size = 0;
		version = 0;
		comparator = comp;
		assert wellFormed() : "invariant failed at end of constructor";
	}
	
	/**
	 * Create a sorted collection with the natural comparator,
	 * and with all the elements from the given collection
	 * @param from collection to get elements from, must not be null
	 */
	public SortedCollection(Collection<? extends E> from) {
		this();
		addAll(from);
		assert wellFormed() : "invariant failed at end of constructor";
	}
	
	@Override // required
	public int size() {
		assert wellFormed() : "invariant false at start of size";
		return size;
	}

	@Override // efficiency
	public void clear() {
		assert wellFormed() : "invariant false at start of clear";
		tail = tail.next;
		tail.next = tail;
		size = 0;
		++version;
		assert wellFormed() : "invariant flase at end of clear";
	}

	@Override // implementation
	public boolean add(E element) {
		assert wellFormed() : "invariant false at start of add";
		if (element == null) throw new IllegalArgumentException("cannot add null");
		if (size > 0 && comparator.compare(element, tail.data) < 0) {
			Node<E> elem = new Node<E>(element, null);
			Node<E> c = tail.next.next;
			
			if (comparator.compare(elem.data, c.data) < 0) {
				elem.next = c;
				tail.next.next = elem;
			}
			else {
				for ( ; c != tail.next; c = c.next) 
					if (comparator.compare(element, c.next.data) <= 0) break;
				elem.next = c.next;
				c.next = elem;
			}
		} 
		else tail = tail.next = new Node<E>(element, tail.next);
		++size;
		++version;
		assert wellFormed() : "invariant false at end of add";
		return true;
	}
	
	@Override // efficiency
	public boolean addAll(Collection<? extends E> c) {
		assert wellFormed() : "invariant false at start of addAll";
		if (c == null) return false;
		Node<E> newTail = toCLL(null, c);
		int colSize = c.size();
		if (colSize == 0) return false;
		if (colSize == 1) add(c.iterator().next());
		else {
			newTail = quicksort(newTail);
			tail = merge(tail, newTail);		
			size += colSize;
		}
		++version;
		assert wellFormed() : "invariant false at end of addAll";
		return true;
	}

	@Override // required
	public Iterator<E> iterator() {
		return new MyIterator();
	}
		
	private class MyIterator implements Iterator<E> {
		private int myVersion = version;
		private Node<E> precursor = tail.next, cursor = precursor;
		
		private boolean wellFormed() {
			// 0. The outer invariant holds
			// 1. cursor is not null
			// 2. precursor is equal to cursor or is the node before cursor
			// 3. precursor is in the list
			// We don't check 1,2,3 unless the version matches.
			
			// 0.
			if (!SortedCollection.this.wellFormed()) return false;
			if (myVersion == version) {
				// 1.
				if (cursor == null && tail != null) return report("cursor is null");
				
				// 2.
				if (precursor != cursor && precursor.next != cursor)
					return report("precursor is bad");
				
				// 3.
				Node<E> p;
				for (p = tail.next; p != precursor && p != tail; p = p.next) {
					// nothing
				}
				if (p != precursor) return report("precursor not in list");

			}
			return true;
		}
		
		public MyIterator() {
			assert wellFormed() : "invariant fails in iterator constructor";
		}
		
		private void checkVersion() {
			if (version != myVersion) {
				throw new ConcurrentModificationException("iterator stale");
			}
		}
		
		public boolean hasNext() {
			assert wellFormed() : "invariant fails at start of hasNext()";
			checkVersion();
			return cursor != tail; 
		}

		public E next() {
			assert wellFormed() : "invariant fails at start of next()";
			checkVersion();
			if (!hasNext()) {
				throw new NoSuchElementException("no more elements");
			}
			precursor = cursor;
			cursor = cursor.next;
			assert wellFormed() : "invariant fails at end of next()";
			return cursor.data;
		}

		public void remove() {
			assert wellFormed() : "invariant fails at start of remove()";
			checkVersion();
			if (precursor == cursor) {
				throw new IllegalStateException("cannot remove until next() is called (again)");
			}
			boolean removingTail = cursor == tail;
			precursor.next = cursor.next;
			cursor = precursor;
			if (removingTail) tail = cursor;
			--size;
			myVersion = ++version;
			assert wellFormed() : "invariant fails at end of remove()";
		}
	}
	
}
