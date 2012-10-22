package com.github.uchan_nos.c_helper.pointer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MemoryManager {
    private final List<MemoryBlock> memoryBlocks;

    public MemoryManager() {
        this.memoryBlocks = new ArrayList<MemoryBlock>();
    }

    public MemoryManager(MemoryManager o) {
        this.memoryBlocks = new ArrayList<MemoryBlock>(o.memoryBlocks.size());
        for (MemoryBlock b : o.memoryBlocks) {
            this.memoryBlocks.add(new MemoryBlock(b));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MemoryManager)) {
            return false;
        }
        MemoryManager m = (MemoryManager) o;
        return this.memoryBlocks.equals(m.memoryBlocks);
    }

    @Override
    public int hashCode() {
        return this.memoryBlocks.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        Iterator<MemoryBlock> it = memoryBlocks.iterator();
        if (it.hasNext()) {
            sb.append(it.next().toString());

            while (it.hasNext()) {
                sb.append(',');
                sb.append(it.next().toString());
            }
        } else {
            sb.append("empty-mem-mgr");
        }

        return sb.toString();
    }

    /**
     * メモリブロックを割り当てて返す.
     * 既存のメモリブロック郡の中で解放済みかつ参照されていないブロックがあれば，
     * そのブロックを割り当て済みにして返す.
     * 参照カウントは変更されない.
     */
    public MemoryBlock allocate() {
        for (MemoryBlock b : memoryBlocks) {
            if (b.allocated() == false && b.refCount() == 0) {
                b.allocate();
                return b;
            }
        }
        MemoryBlock newBlock = new MemoryBlock(memoryBlocks.size(), true, 0);
        memoryBlocks.add(newBlock);
        return newBlock;
    }

    /**
     * 指定されたメモリブロックを解放済みにする.
     * 参照カウントは変更されない.
     */
    public void release(MemoryBlock b) {
        b.release();
    }

    /**
     * 指定された識別子を持つメモリブロックを返す.
     * そのようなメモリブロックが見つからなければ null を返す.
     */
    public MemoryBlock find(int id) {
        for (MemoryBlock b : memoryBlocks) {
            if (b.id() == id) {
                return b;
            }
        }
        return null;
    }

    /**
     * メモリブロックの一覧を返す.
     */
    public List<MemoryBlock> memoryBlocks() {
        return memoryBlocks;
    }
}
