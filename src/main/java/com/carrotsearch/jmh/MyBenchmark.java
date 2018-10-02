/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.carrotsearch.jmh;

import org.openjdk.jmh.annotations.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 4, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 1)
public class MyBenchmark {
    private double [] vec1 = new double [1024 * 1024];
    private double [] vec2 = new double [1024 * 1024];

    public MyBenchmark() {
        int r = 0;
        for (int i = 0; i < vec1.length; i++) {
            vec1[i] = r++ / 10;
            vec2[i] = r++ % 11;
        }
    }

    private interface Fma {
        double fma(double a, double b, double c);
    }

    private final static Fma fmaClosure = (a, b, c) -> a * b + c;
    private final static Fma fmaMethodHandles;
    static {
        try {
            MethodHandle mh = MethodHandles.lookup()
                    .findStatic(Math.class, "fma", MethodType.methodType(double.class, double.class, double.class, double.class));
            fmaMethodHandles = (a, b, c) -> {
                try {
                    return (double) mh.invokeExact(a, b, c);
                } catch (Throwable e) {
                    throw new RuntimeException("Method handles failed.", e);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public double mathFma() {
        double f = 0;
        for (int i = 0; i < vec1.length; i++) {
            f = Math.fma(vec1[i], vec2[i], f);
        }
        return f;
    }

    @Benchmark
    @Fork(jvmArgs = "-XX:-UseFMA")
    public double mathNoFma() {
        return mathFma();
    }

    @Benchmark
    public double manual() {
        double f = 0;
        for (int i = 0; i < vec1.length; i++) {
            f = vec1[i] * vec2[i] + f;
        }
        return f;
    }

    @Benchmark
    public double manualViaClosure() {
        double f = 0;
        for (int i = 0; i < vec1.length; i++) {
            f = fmaClosure.fma(vec1[i], vec2[i], f);
        }
        return f;
    }

    @Benchmark
    public double fmaViaMethodHandles() {
        double f = 0;
        for (int i = 0; i < vec1.length; i++) {
            f = fmaMethodHandles.fma(vec1[i], vec2[i], f);
        }
        return f;
    }
}
