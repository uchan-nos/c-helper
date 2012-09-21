package com.github.uchan_nos.c_helper.pointer;

import com.github.uchan_nos.c_helper.util.Util;

/**
 * ヒープ領域に確保されたメモリブロックの先頭アドレスを表す.
 */
public class HeapAddress extends Address {
    private final MemoryBlock memoryBlock;

    /**
     * 指定されたメモリブロックの先頭アドレスを生成.
     */
    public HeapAddress(MemoryBlock memoryBlock) {
        this.memoryBlock = memoryBlock;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof HeapAddress)) {
            return false;
        }
        HeapAddress a = (HeapAddress) o;
        return Util.equalsOrBothNull(this.memoryBlock, a.memoryBlock);
    }

    @Override
    public final int hashCode() {
        return memoryBlock == null ? 0 : memoryBlock.hashCode();
    }

    @Override
    public String toString() {
        return "HeapAddress(" + memoryBlock.id() + ")";
    }

    /**
     * このアドレスが表すメモリブロックを返す.
     */
    public MemoryBlock memoryBlock() {
        return memoryBlock;
    }
}
