package com.github.uchan_nos.c_helper.pointer;

/**
 * ヒープ領域に確保されたメモリブロックの先頭アドレスを表す.
 */
public class HeapAddress extends Address {
    private final int memoryBlockId;

    /**
     * 指定されたメモリブロックの先頭アドレスを生成.
     */
    public HeapAddress(int memoryBlockId) {
        this.memoryBlockId = memoryBlockId;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof HeapAddress)) {
            return false;
        }
        HeapAddress a = (HeapAddress) o;
        return this.memoryBlockId == a.memoryBlockId;
    }

    @Override
    public final int hashCode() {
        return memoryBlockId;
    }

    @Override
    public String toString() {
        return "HeapAddress(" + memoryBlockId + ")";
    }

    /**
     * このアドレスが表すメモリブロックを返す.
     */
    public int memoryBlockId() {
        return memoryBlockId;
    }
}
