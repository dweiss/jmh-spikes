package com.carrotsearch.jmh;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 1)
public class Word2VecBenchmark {
  private final int dimensions = 256;
  private final int words = 1 << 17;
  private float[][] inputVector = new float[words][dimensions];
  private float[][] outputVector = new float[words][dimensions];
  private float[] eh = new float[dimensions];
  private final SplittableRandom random = new SplittableRandom();

  public Word2VecBenchmark() {
    for (int i = 0; i < words; i++) {
      for (int j = 0; j < dimensions; j++) {
        inputVector[i][j] = (float) random.nextDouble();
        outputVector[i][j] = (float) random.nextDouble();
      }
    }
    for (int i = 0; i < eh.length; i++) {
      eh[i] = (float) random.nextDouble();
    }
  }

  @Benchmark
  public float fmaComputeF() {
    final int w1 = random.nextInt(words);
    final int w2 = random.nextInt(words);

    float f = 0;
    for (int i = 0; i < dimensions; i++) {
      f = Math.fma(inputVector[w1][i], outputVector[w2][i], f);
    }
    return f;
  }

  @Benchmark
  public float manualComputeF() {
    final int w1 = random.nextInt(words);
    final int w2 = random.nextInt(words);

    float f = 0;
    for (int i = 0; i < dimensions; i++) {
      f += inputVector[w1][i] * outputVector[w2][i];
    }
    return f;
  }

  @Benchmark
  public float unrolled4xFmaComputeF() {
    // 2- and 8-way expansions were slower.
    final int w1 = random.nextInt(words);
    final int w2 = random.nextInt(words);

    float f0 = 0, f1 = 0, f2 = 0, f3 = 0;
    for (int i = 0; i < dimensions; i += 4) {
      f0 = Math.fma(inputVector[w1][i], outputVector[w2][i], f0);
      f1 = Math.fma(inputVector[w1][i + 1], outputVector[w2][i + 1], f1);
      f2 = Math.fma(inputVector[w1][i + 2], outputVector[w2][i + 2], f2);
      f3 = Math.fma(inputVector[w1][i + 3], outputVector[w2][i + 3], f3);
    }
    return f0 + f1 + f2 + f3;
  }

  @Benchmark
  public float unrolled4xManualComputeF() {
    final int w1 = random.nextInt(words);
    final int w2 = random.nextInt(words);

    float f0 = 0, f1 = 0, f2 = 0, f3 = 0;
    for (int i = 0; i < dimensions; i += 4) {
      f0 += inputVector[w1][i] * outputVector[w2][i];
      f1 += inputVector[w1][i + 1] * outputVector[w2][i + 1];
      f2 += inputVector[w1][i + 2] * outputVector[w2][i + 2];
      f3 += inputVector[w1][i + 3] * outputVector[w2][i + 3];
    }
    return f0 + f1 + f2 + f3;
  }

  @Benchmark
  public void fmaUpdateEH() {
    final int w2 = random.nextInt(words);
    final float g = (float) random.nextDouble();

    for (int i = 0; i < dimensions; i++) {
      eh[i] = Math.fma(g, outputVector[w2][i], eh[i]);
    }
  }

  @Benchmark
  public void manualUpdateEH() {
    final int w2 = random.nextInt(words);
    final float g = (float) random.nextDouble();

    for (int i = 0; i < dimensions; i++) {
      eh[i] += g * outputVector[w2][i];
    }
  }

  @Benchmark
  public void updateInputVector() {
    final int w1 = random.nextInt(words);

    for (int i = 0; i < dimensions; i++) {
      inputVector[w1][i] += eh[i];
    }
  }
}
