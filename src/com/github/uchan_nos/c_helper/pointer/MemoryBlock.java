package com.github.uchan_nos.c_helper.pointer;

/**
 * ヒープ領域にある1つのメモリブロックを表す.
 */
public class MemoryBlock {
    private int id;
    private boolean allocated;
    private int refCount;

    /**
     * 新たなメモリブロックを生成する.
     */
    public MemoryBlock(int id, boolean allocated, int refCount) {
        this.id = id;
        this.allocated = allocated;
        this.refCount = refCount;
    }

    /**
     * 指定されたメモリブロックのコピーを生成する.
     */
    public MemoryBlock(MemoryBlock o) {
        this.id = o.id;
        this.allocated = o.allocated;
        this.refCount = o.refCount;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MemoryBlock)) {
            return false;
        }
        MemoryBlock b = (MemoryBlock) o;
        return this.id == b.id
            && this.allocated == b.allocated
            && this.refCount == b.refCount;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + this.id;
        result = 31 * result + (this.allocated ? 0 : 1);
        result = 31 * result + this.refCount;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MB(id=");
        sb.append(id);
        sb.append(',');
        sb.append(allocated ? "allocated" : "released");
        sb.append(",rc=");
        sb.append(refCount);
        sb.append(')');
        return sb.toString();
    }

    /**
     * このメモリブロックの参照カウントを1増加させる.
     */
    public void ref() {
        ++refCount;
    }

    /**
     * このメモリブロックの参照カウントを1減少させる.
     */
    public void unref() {
        --refCount;
    }

    /**
     * このメモリブロックに割り当て済みマークを付ける.
     * MemoryManagerから呼び出されることを想定している.
     */
    public void allocate() {
        allocated = true;
    }

    /**
     * このメモリブロックの割り当て済みマークを消す.
     * MemoryManagerから呼び出されることを想定している.
     */
    public void release() {
        allocated = false;
    }

    /**
     * このメモリブロックの識別子を返す.
     */
    public int id() {
        return id;
    }

    /**
     * このメモリブロックの割り当て済みマークを返す.
     */
    public boolean allocated() {
        return allocated;
    }

    /**
     * このメモリブロックの参照カウント値を返す.
     */
    public int refCount() {
        return refCount;
    }
}
