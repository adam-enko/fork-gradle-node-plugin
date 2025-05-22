package com.github.gradle.node.util

import org.gradle.api.provider.Provider

internal fun <A, B> zip(
    aProvider: Provider<A>,
    bProvider: Provider<B>,
): Provider<Pair<A, B>> {
    return aProvider.zip(bProvider) { a, b -> Pair(a, b) }
}

internal fun <A, B, C> zip(
    aProvider: Provider<A>,
    bProvider: Provider<B>,
    cProvider: Provider<C>,
):
        Provider<Triple<A, B, C>> {
    return zip(aProvider, bProvider).flatMap { pair -> cProvider.map { c -> Triple(pair.first, pair.second, c!!) } }
}

internal fun <A, B, C, D> zip(
    aProvider: Provider<A>,
    bProvider: Provider<B>,
    cProvider: Provider<C>,
    dProvider: Provider<D>,
): Provider<Tuple4<A, B, C, D>> {
    return zip(zip(aProvider, bProvider), zip(cProvider, dProvider))
        .map { pairs -> Tuple4(pairs.first.first, pairs.first.second, pairs.second.first, pairs.second.second) }
}

internal data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

internal fun <A, B, C, D, E> zip(
    aProvider: Provider<A>,
    bProvider: Provider<B>,
    cProvider: Provider<C>,
    dProvider: Provider<D>,
    eProvider: Provider<E>,
): Provider<Tuple5<A, B, C, D, E>> {
    return zip(zip(aProvider, bProvider), zip(cProvider, dProvider, eProvider))
        .map { pairs ->
            Tuple5(
                pairs.first.first, pairs.first.second, pairs.second.first, pairs.second.second,
                pairs.second.third
            )
        }
}

internal data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)


internal fun <P01, P02, R> zip(
    provider01: Provider<P01>,
    provider02: Provider<P02>,
    combiner: (P01, P02) -> R,
): Provider<R> {
    return provider01
        .zip(provider02) { p1, p2 -> combiner(p1, p2) }
}

internal fun <P01, P02, P03, R> zip(
    provider01: Provider<P01>,
    provider02: Provider<P02>,
    provider03: Provider<P03>,
    combiner: (P01, P02, P03) -> R,
): Provider<R> {
    zip(
        provider01,
        provider02
    ) { p1, p2 -> listOf(p1, p2) }
    return provider01
        .zip(provider02) { p1, p2 -> listOf(p1, p2) }
        .zip(provider03) { (p1, p2), p3 ->
            @Suppress("UNCHECKED_CAST")
            combiner(
                p1 as P01,
                p2 as P02,
                p3,
            )
        }
}

internal fun <P01, P02, P03, P04, R> zip(
    provider01: Provider<P01>,
    provider02: Provider<P02>,
    provider03: Provider<P03>,
    provider04: Provider<P04>,
    combiner: (P01, P02, P03, P04) -> R,
): Provider<R> {
    return zip(
        provider01,
        provider02,
        provider03,
    ) { p1, p2, p3 -> listOf(p1, p2, p3) }
        .zip(provider04) { (p1, p2, p3), p4 ->
            @Suppress("UNCHECKED_CAST")
            combiner(
                p1 as P01,
                p2 as P02,
                p3 as P03,
                p4,
            )
        }
}


internal fun <P01, P02, P03, P04, P05, R> zip(
    provider01: Provider<P01>,
    provider02: Provider<P02>,
    provider03: Provider<P03>,
    provider04: Provider<P04>,
    provider05: Provider<P05>,
    combiner: (P01, P02, P03, P04, P05) -> R,
): Provider<R> {
    return zip(
        provider01,
        provider02,
        provider03,
        provider04,
    ) { p1, p2, p3, p4 -> listOf(p1, p2, p3, p4) }
        .zip(provider05) { (p1, p2, p3, p4), p5 ->
            @Suppress("UNCHECKED_CAST")
            combiner(
                p1 as P01,
                p2 as P02,
                p3 as P03,
                p4 as P04,
                p5
            )
        }
}
