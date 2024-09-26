package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    boolean profiled = Arrays.stream(klass.getMethods())
            .anyMatch(m -> m.isAnnotationPresent(Profiled.class));
    if (!profiled) {
      throw new IllegalArgumentException(
              "Wrapped interface does not provide methods with @Profiled annotation.");
    }

    Object proxy = Proxy.newProxyInstance(
            klass.getClassLoader(),
            new Class[] { klass },
            new ProfilingMethodInterceptor(clock, state, delegate));
    return (T) proxy;
  }

  @Override
  public void writeData(Path path) {
    Objects.requireNonNull(path);
    try (Writer writer = new BufferedWriter(new FileWriter(path.toFile()))) {
      writeData(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
