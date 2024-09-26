package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState state;
  private final Object target;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, ProfilingState state, Object target) {
    this.clock = Objects.requireNonNull(clock);
    this.state = Objects.requireNonNull(state);
    this.target = target;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    if (!method.isAnnotationPresent(Profiled.class)) {
      try {
        return method.invoke(target, args);
      } catch (InvocationTargetException e ) {
        throw e.getTargetException();
      } catch (IllegalAccessException e ) {
        throw e;
      }
    }

    Object returnObject = null;
    Instant start = clock.instant(), end;
    Throwable throwable = null;
    try {
       returnObject = method.invoke(target, args);
    } catch (InvocationTargetException e) {
      throwable = e.getTargetException();
    } catch (IllegalAccessException e ) {
      throwable = e;
    } finally {
      end = clock.instant();
      state.record(target.getClass(), method, Duration.between(start, end));
      if (throwable != null) throw throwable;
    }
    return returnObject;
  }
}
