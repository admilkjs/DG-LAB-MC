package dglabmc.core.security.internal;

public final class ShardRepository {
    private static final int PASSWORD_SEED = 113;
    private static final int SIGNAL_SEED = 151;
    private static final int PROGRAM_SEED = 197;
    private static final int PREFIX_SEED = 59;
    private static final int VERSION_SEED = 77;
    private static final int PATTERN_SEED = 91;
    private static final int SEPARATOR_SEED = 123;
    private static final int JOIN_SEED = 131;

    private ShardRepository() {
    }

    public static int[] passwordBlob() {
        return merge(PasswordShardA.data(), PasswordShardB.data(), PasswordShardC.data());
    }

    public static int[] signalBlob() {
        return merge(SignalShardA.data(), SignalShardB.data(), SignalShardC.data());
    }

    public static int[] subProgramBlob() {
        return merge(ProgramShardA.data(), ProgramShardB.data());
    }

    public static int passwordSeed() {
        return PASSWORD_SEED;
    }

    public static int signalSeed() {
        return SIGNAL_SEED;
    }

    public static int programSeed() {
        return PROGRAM_SEED;
    }

    public static int[] prefixBlob() {
        return new int[] {127, 11, 17, 47, 61, 180};
    }

    public static int prefixSeed() {
        return PREFIX_SEED;
    }

    public static int[] versionBlob() {
        return new int[] {59, 111};
    }

    public static int versionSeed() {
        return VERSION_SEED;
    }

    public static int[] patternBlob() {
        return new int[] {31, 43, 49, 207, 221, 236, 157, 246, 203, 175, 68, 59, 125, 89, 100, 32, 91, 81, 180, 193, 130, 157, 250, 203};
    }

    public static int patternSeed() {
        return PATTERN_SEED;
    }

    public static int[] separatorBlob() {
        return new int[] {7};
    }

    public static int separatorSeed() {
        return SEPARATOR_SEED;
    }

    public static int[] joinBlob() {
        return new int[] {185};
    }

    public static int joinSeed() {
        return JOIN_SEED;
    }

    private static int[] merge(int[] first, int[] second, int[] third) {
        int[] merged = new int[first.length + second.length + third.length];
        int cursor = 0;
        cursor = copy(first, merged, cursor);
        cursor = copy(second, merged, cursor);
        copy(third, merged, cursor);
        return merged;
    }

    private static int[] merge(int[] first, int[] second) {
        int[] merged = new int[first.length + second.length];
        int cursor = 0;
        cursor = copy(first, merged, cursor);
        copy(second, merged, cursor);
        return merged;
    }

    private static int copy(int[] source, int[] target, int cursor) {
        System.arraycopy(source, 0, target, cursor, source.length);
        return cursor + source.length;
    }

    private static final class PasswordShardA {
        private static int[] data() {
            return new int[] {89, 175, 183, 132, 144, 228, 179};
        }
    }

    private static final class PasswordShardB {
        private static int[] data() {
            return new int[] {197, 215, 47, 51, 7, 89, 107};
        }
    }

    private static final class PasswordShardC {
        private static int[] data() {
            return new int[] {121, 90, 163, 246, 156, 204};
        }
    }

    private static final class SignalShardA {
        private static int[] data() {
            return new int[] {191, 133, 157, 234, 254, 206, 153, 35};
        }
    }

    private static final class SignalShardB {
        private static int[] data() {
            return new int[] {49, 21, 105, 121, 7, 78, 165};
        }
    }

    private static final class SignalShardC {
        private static int[] data() {
            return new int[] {184, 128, 144, 236, 190, 212, 132};
        }
    }

    private static final class ProgramShardA {
        private static int[] data() {
            return new int[] {208, 192, 240, 224};
        }
    }

    private static final class ProgramShardB {
        private static int[] data() {
            return new int[] {16, 0, 48, 32};
        }
    }
}

