package local.prominence.work;

import java.util.List;
import net.minecraft.class_1799;

public final class FtbQuestsFallbackStacksTests {
    private static int checks;
    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
        checks++;
    }

    public static void main(String[] args) {
        FtbQuestsFallbackStacks.clear();
        List<class_1799> first = FtbQuestsFallbackStacks.getRegistryStacks();
        check(first != null, "Fallback registry stacks must not be null");
        List<class_1799> second = FtbQuestsFallbackStacks.getRegistryStacks();
        check(first == second, "Fallback stacks must be cached and idempotent");
        FtbQuestsFallbackStacks.clear();
        List<class_1799> third = FtbQuestsFallbackStacks.getRegistryStacks();
        check(third != null, "Reacquired fallback stacks must not be null after clear");
        System.out.println("PASS: ftb quests fallback stacks checks=" + checks);
    }
}
