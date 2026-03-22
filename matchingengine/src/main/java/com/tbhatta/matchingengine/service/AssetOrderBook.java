package com.tbhatta.matchingengine.service;

import com.sun.source.tree.Tree;
import com.tbhatta.matchingengine.model.OrderItemModel;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AssetOrderBook {
    private final TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTree;
    private final TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askTree;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public AssetOrderBook(TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> bidTree,
                          TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> askTree) {
        this.bidTree = bidTree;
        this.askTree = askTree;
    }

    // Lock acquisition/release helper methods
    public void acquireReadLock() {
        lock.readLock().lock();
    }

    public void acquireWriteLock() {
        lock.writeLock().lock();
    }

    public void releaseReadLock() {
        lock.readLock().unlock();
    }

    public void releaseWriteLock() {
        lock.writeLock().unlock();
    }

    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getBidTree() {
        return bidTree;
    }

    public TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> getAskTree() {
        return askTree;
    }

    public long getTotalVolume(String side) {
        lock.readLock().lock();
        try {
            TreeMap<BigDecimal, PriorityQueue<OrderItemModel>> tree =
                    side.equalsIgnoreCase("ask") ? askTree : bidTree;
            return tree.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(PriorityQueue::stream)
                    .filter(Objects::nonNull)
                    .mapToLong(o -> o.getVolume().longValue())
                    .sum();
        } finally {
            lock.readLock().unlock();
        }
    }



}
