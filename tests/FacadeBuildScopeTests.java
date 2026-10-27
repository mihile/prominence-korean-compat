package local.prominence.work;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class FacadeBuildScopeTests {
    public static void main(String[] args) throws Exception {
        Object a = new String("same"), b = new String("same");
        AtomicInteger calls = new AtomicInteger();
        if (FacadeBuildScope.first(a, calls::incrementAndGet, -1) != 1) throw new AssertionError();
        try (var scope = FacadeBuildScope.begin()) {
            if (FacadeBuildScope.first(a, calls::incrementAndGet, -1) != 2) throw new AssertionError();
            for (int i=0; i<100_000; i++) if (FacadeBuildScope.first(a, calls::incrementAndGet, -1) != -1) throw new AssertionError();
            if (FacadeBuildScope.first(b, calls::incrementAndGet, -1) != 3) throw new AssertionError("Equality must be identity");
            try (var nested = FacadeBuildScope.begin()) {
                if (FacadeBuildScope.first(a, calls::incrementAndGet, -1) != 4) throw new AssertionError("Nested scopes isolated");
            }
            if (FacadeBuildScope.first(a, calls::incrementAndGet, -1) != -1) throw new AssertionError("Parent not restored");
            Object error = new Object();
            try { FacadeBuildScope.first(error, () -> { throw new IllegalArgumentException(); }, -1); throw new AssertionError(); }
            catch (IllegalArgumentException expected) { }
            if (FacadeBuildScope.first(error, calls::incrementAndGet, -1) != 5) throw new AssertionError("Failure cached");
            var executor=Executors.newSingleThreadExecutor();
            try {
                if (executor.submit(() -> { try(var other=FacadeBuildScope.begin()) { return FacadeBuildScope.first(a,calls::incrementAndGet,-1); } }).get() != 6) throw new AssertionError("Thread scopes mixed");
            } finally { executor.shutdown(); }
            if (scope.repeated() != 100_001) throw new AssertionError();
        }
        if (FacadeBuildScope.first(a,calls::incrementAndGet,-1) != 7) throw new AssertionError("Scope leaked");
        try (var scope=FacadeBuildScope.begin()) {
            if(FacadeBuildScope.first(a,calls::incrementAndGet,-1)!=8) throw new AssertionError("Rebuild reused stale result");
        }
        System.out.println("PASS: 100000 duplicate checks, identity/nesting/thread/error/rebuild/finally lifetime");
    }
}
