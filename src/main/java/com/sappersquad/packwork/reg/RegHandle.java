package com.sappersquad.packwork.reg;

import java.util.function.Supplier;

/**
 * A registered entry, kept behind the same {@code .get()} the NeoForge branches'
 * {@code DeferredHolder}/{@code DeferredItem} call sites use - so the forty-odd
 * consumers across menu/trinkets/sorting/tests read identically on both loaders.
 * On Fabric registration is EAGER (plain {@code Registry.register} during mod init),
 * so this is just a value in a Supplier's clothing.
 */
public final class RegHandle<T> implements Supplier<T> {

    private final T value;

    public RegHandle(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }
}
