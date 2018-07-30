# Building with fiber support using Project Loom.

At the moment to test fiber support using [Project
Loom](http://openjdk.java.net/projects/loom/) you'll need to do a few
things.

## Building Loom, TruffleRuby, and the support jar

1. Build a JDK from the `fibers` branch in the project loom
   repository, it may be helpful to set an environment variable to
   point to this JDK, e.g. `LOOM_HOME`.
2. Build truffleruby as normal.
3. Take a copy of
   `src/main/java/org/truffleruby/core/fiber/ContinuationHelper.java`,
   edit it to remove the `new Error()` lines and uncomment the continuations code,
   and finally build a jar using your loom JDK. So something like
   ```
   mkdir loom-test/org/truffleruby/core/fiber/ContinuationHelper.java
   cp src/main/java/org/truffleruby/core/fiber/ContinuationHelper.java loom-test
   cd loom-test
   $LOOM_HOME/bin/javac org/truffleruby/core/fiber/ContinuationHelper.java
   $LOOM_HOME/bin/javr --create --file helper.jar org/truffleruby/core/fiber/*.class   
   ```
   
## Running TruffleRuby

To run TruffleRuby you will need the support jar on your class path
before the TruffleRuby jar itself. Unfortunately our current command
line scripts append class path entries after the TruffleRuby jar. You
can generate the Java command to run TruffleRuby with Graal by doing

```
tool/jt.rb ruby --graal -J-cmd
```

Edit that command to include the helper jar at the start of the class
path and to use your Loom JDK and you should be able to bring up a
Ruby REPL.

## Disclaimer

The current Loom prototype will probably crash the whole JVM if a
continuation refers to a compiled method that has been marked
non-reentrant and that method is evicted from the code cache. You may
be able to avoid this by setting `-XX:InitialCodeCacheSize` and
`-XX:ReservedCodeCacheSize` to suitably large values, but it is
probably unwise to attempt to run any large Ruby programs at this
point.
