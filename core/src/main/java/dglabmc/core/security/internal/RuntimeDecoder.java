package dglabmc.core.security.internal;

public final class RuntimeDecoder {
    private RuntimeDecoder() {
    }

    public static int[] decodeTape(int[] blob, int seed) {
        int[] decoded = new int[blob.length];
        for (int index = 0; index < blob.length; index++) {
            decoded[index] = blob[index] ^ mask(seed, index);
        }
        return decoded;
    }

    public static int decodeScalar(int blob, int seed) {
        return blob ^ seed;
    }

    private static int mask(int seed, int index) {
        int mixed = seed + (index * 17);
        mixed ^= (index << 1);
        mixed ^= (index << 1);
        return mixed & 0xFF;
    }
}

