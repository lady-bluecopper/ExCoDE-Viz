package eu.unitn.disi.db.dence.utils;

import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 *
 * @author bluecopper
 * @param <T>
 */
public class HashObjSetCollector<T> implements Collector<T, HashObjSet<T>, HashObjSet<T>> {

    public Supplier<HashObjSet<T>> supplier() {
        return () -> HashObjSets.newMutableSet();
    }

    public BiConsumer<HashObjSet<T>, T> accumulator() {
        return (builder, t) -> builder.add(t);
    }

    public BinaryOperator<HashObjSet<T>> combiner() {
        return (l, r) -> {
            l.addAll(r);
            return l;
        };
    }

    public Function<HashObjSet<T>, HashObjSet<T>> finisher() {
        return t -> t;
    }

    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    
}
