package dglabmc.security;

import dglabmc.security.internal.ProgramComposer;
import dglabmc.security.internal.RuntimeDecoder;
import dglabmc.security.internal.ShardRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;

final class SecurityVm {
    private static final int MASK = RuntimeDecoder.decodeScalar(19, 0x5A);

    private static final int O_SET = 1;
    private static final int O_COPY = 2;
    private static final int O_BLOCK = 3;
    private static final int O_STEP = 4;
    private static final int O_NOP = 5;
    private static final int O_JUNK = 6;
    private static final int O_END = 7;
    private static final int O_FAKE = 8;
    private static final int O_HALT = 0;

    private static final int I_PUSH = 11;
    private static final int I_DUP = 12;
    private static final int I_SUB = 13;
    private static final int I_APPEND = 14;
    private static final int I_POP = 15;
    private static final int I_XOR0 = 16;
    private static final int I_NOP = 17;
    private static final int I_DONE = 19;

    private static final int S_INPUT = 21;
    private static final int S_MASK = 22;
    private static final int S_XOR = 23;
    private static final int S_ROL = 24;
    private static final int S_ROR = 25;
    private static final int S_ADD0 = 26;
    private static final int S_FAKE = 27;
    private static final int S_DONE = 28;

    private static final int V_ARG = 41;
    private static final int V_TRIM = 42;
    private static final int V_NEW = 43;
    private static final int V_ADD = 44;
    private static final int V_CONST = 45;
    private static final int V_SEED = 46;
    private static final int V_TOSTR = 47;
    private static final int V_SHA = 48;
    private static final int V_SUBSTR = 49;
    private static final int V_UPPER = 50;
    private static final int V_SPLIT = 51;
    private static final int V_REGEX = 52;
    private static final int V_RET = 53;
    private static final int V_DROP = 54;
    private static final int V_DUP = 55;
    private static final int V_FAKE = 56;
    private static final int V_EQ = 57;
    private static final int V_BOOL = 58;
    private static final int V_JOIN = 59;

    private static final int[] P0 = new int[] {
        V_NEW,
        V_SEED, 0,
        V_ADD,
        V_CONST, 4,
        V_ADD,
        V_ARG, 0,
        V_TRIM,
        V_ADD,
        V_TOSTR,
        V_SHA,
        V_SUBSTR, 0, 10,
        V_UPPER,
        V_RET
    };

    private static final int[] P1 = new int[] {
        V_NEW,
        V_CONST, 1,
        V_ADD,
        V_CONST, 3,
        V_ADD,
        V_ARG, 0,
        V_ADD,
        V_CONST, 3,
        V_ADD,
        V_ARG, 1,
        V_ADD,
        V_CONST, 3,
        V_ADD,
        V_ARG, 2,
        V_ADD,
        V_TOSTR,
        V_RET
    };

    private static final int[] P2 = new int[] {
        V_NEW,
        V_SEED, 1,
        V_ADD,
        V_CONST, 4,
        V_ADD,
        V_ARG, 0,
        V_TRIM,
        V_ADD,
        V_TOSTR,
        V_RET
    };

    private static final int[] P3 = new int[] {
        V_CONST, 0,
        V_RET
    };

    private static final int[] P4 = new int[] {
        V_CONST, 2,
        V_REGEX,
        V_RET
    };

    private static final int[] P5 = new int[] {
        V_ARG, 0,
        V_CONST, 3,
        V_SPLIT,
        V_RET
    };

    private static final int[] P6 = new int[] {
        V_CONST, 1,
        V_RET
    };

    private static volatile Pattern cache;

    private SecurityVm() {
    }

    static Object v(int program, Object... args) {
        int[] code = select(program);
        ObjStack stack = new ObjStack(16);
        StringBuilder builder = null;
        int cursor = 0;
        while (cursor < code.length) {
            int op = code[cursor++];
            if (op == V_ARG) {
                stack.push(readArg(args, code[cursor++]));
                continue;
            }
            if (op == V_TRIM) {
                Object top = stack.pop();
                stack.push(top == null ? "" : String.valueOf(top).trim());
                continue;
            }
            if (op == V_NEW) {
                builder = new StringBuilder();
                continue;
            }
            if (op == V_ADD) {
                if (builder == null) {
                    builder = new StringBuilder();
                }
                builder.append(String.valueOf(stack.pop()));
                continue;
            }
            if (op == V_CONST) {
                stack.push(c(code[cursor++]));
                continue;
            }
            if (op == V_SEED) {
                stack.push(seed(code[cursor++]));
                continue;
            }
            if (op == V_TOSTR) {
                stack.push(builder == null ? "" : builder.toString());
                builder = null;
                continue;
            }
            if (op == V_SHA) {
                stack.push(sha256Hex(String.valueOf(stack.pop())));
                continue;
            }
            if (op == V_SUBSTR) {
                int start = code[cursor++];
                int end = code[cursor++];
                stack.push(String.valueOf(stack.pop()).substring(start, end));
                continue;
            }
            if (op == V_UPPER) {
                stack.push(String.valueOf(stack.pop()).toUpperCase(Locale.ROOT));
                continue;
            }
            if (op == V_SPLIT) {
                String separator = String.valueOf(stack.pop());
                String payload = String.valueOf(stack.pop());
                stack.push(payload.split(Pattern.quote(separator)));
                continue;
            }
            if (op == V_REGEX) {
                Pattern local = cache;
                if (local == null) {
                    local = Pattern.compile(String.valueOf(stack.pop()));
                    cache = local;
                    stack.push(local);
                } else {
                    stack.pop();
                    stack.push(local);
                }
                continue;
            }
            if (op == V_RET) {
                return stack.pop();
            }
            if (op == V_DROP) {
                stack.pop();
                continue;
            }
            if (op == V_DUP) {
                stack.push(stack.peek());
                continue;
            }
            if (op == V_FAKE) {
                Object right = stack.pop();
                Object left = stack.pop();
                if (String.valueOf(left).length() < 0 && left.equals(right)) {
                    stack.push(left);
                }
                stack.push(right);
                continue;
            }
            if (op == V_EQ) {
                Object right = stack.pop();
                Object left = stack.pop();
                stack.push(Boolean.valueOf(String.valueOf(left).equals(String.valueOf(right))));
                continue;
            }
            if (op == V_BOOL) {
                stack.push(Boolean.valueOf(code[cursor++] != 0));
                continue;
            }
            if (op == V_JOIN) {
                Object third = stack.pop();
                Object second = stack.pop();
                Object first = stack.pop();
                stack.push(String.valueOf(first) + String.valueOf(second) + String.valueOf(third));
                continue;
            }
            throw new IllegalStateException("Invalid vm opcode: " + op);
        }
        throw new IllegalStateException("VM program terminated unexpectedly");
    }

    private static int[] select(int id) {
        if (id == 0) {
            return P0;
        }
        if (id == 1) {
            return P1;
        }
        if (id == 2) {
            return P2;
        }
        if (id == 3) {
            return P3;
        }
        if (id == 4) {
            return P4;
        }
        if (id == 5) {
            return P5;
        }
        if (id == 6) {
            return P6;
        }
        throw new IllegalArgumentException("Unknown vm program: " + id);
    }

    private static Object readArg(Object[] args, int index) {
        if (args == null || index < 0 || index >= args.length) {
            return "";
        }
        Object value = args[index];
        return value == null ? "" : value;
    }

    private static String c(int id) {
        if (id == 0) {
            return decodeLiteral(ShardRepository.prefixBlob(), ShardRepository.prefixSeed());
        }
        if (id == 1) {
            return decodeLiteral(ShardRepository.versionBlob(), ShardRepository.versionSeed());
        }
        if (id == 2) {
            return decodeLiteral(ShardRepository.patternBlob(), ShardRepository.patternSeed());
        }
        if (id == 3) {
            return decodeLiteral(ShardRepository.separatorBlob(), ShardRepository.separatorSeed());
        }
        if (id == 4) {
            return decodeLiteral(ShardRepository.joinBlob(), ShardRepository.joinSeed());
        }
        throw new IllegalArgumentException("Unknown const id: " + id);
    }

    private static String seed(int id) {
        if (id == 0) {
            return x(ShardRepository.passwordBlob(), ShardRepository.passwordSeed());
        }
        if (id == 1) {
            return x(ShardRepository.signalBlob(), ShardRepository.signalSeed());
        }
        throw new IllegalArgumentException("Unknown seed id: " + id);
    }

    private static String decodeLiteral(int[] blob, int seed) {
        int[] data = RuntimeDecoder.decodeTape(blob, seed);
        StringBuilder builder = new StringBuilder(data.length);
        for (int value : data) {
            builder.append((char) value);
        }
        return builder.toString();
    }

    private static String x(int[] blob, int seed) {
        return y(z(blob.length), RuntimeDecoder.decodeTape(blob, seed));
    }

    private static int[] z(int length) {
        int[] base = ProgramComposer.buildOuterProgram(length, O_SET, O_JUNK, O_COPY, O_BLOCK, O_STEP, O_NOP, O_END, O_HALT);
        int[] program = new int[base.length + (length * 2)];
        int read = 0;
        int write = 0;
        while (read < base.length) {
            int opcode = base[read++];
            program[write++] = opcode;
            if (opcode == O_SET || opcode == O_JUNK) {
                program[write++] = base[read++];
            }
            if (opcode == O_COPY) {
                program[write++] = O_FAKE;
                program[write++] = write ^ read;
            }
        }
        int[] trimmed = new int[write];
        System.arraycopy(program, 0, trimmed, 0, write);
        return trimmed;
    }

    private static String y(int[] program, int[] data) {
        IntTape tape = new IntTape(data.length);
        IntTape ops = new IntTape(data.length * 12 + 8);
        int cursor = 0;
        int checksum = 0;
        for (int pointer = 0; pointer < program.length; ) {
            int opcode = program[pointer++];
            if (opcode == O_HALT) {
                return w(ops.toArray(), tape.toArray());
            }
            if (opcode == O_SET) {
                cursor = program[pointer++];
                continue;
            }
            if (opcode == O_COPY) {
                tape.push(data[cursor]);
                continue;
            }
            if (opcode == O_BLOCK) {
                ops.pushAll(ProgramComposer.composeInnerDecodeBlock(I_PUSH, tape.size() - 1, I_DUP, I_NOP, I_SUB, I_XOR0, I_APPEND, I_POP));
                continue;
            }
            if (opcode == O_STEP) {
                cursor++;
                continue;
            }
            if (opcode == O_NOP) {
                continue;
            }
            if (opcode == O_JUNK) {
                int s = program[pointer++];
                checksum ^= (s + cursor);
                checksum = (checksum << 1) ^ (checksum >>> 1);
                checksum ^= checksum;
                continue;
            }
            if (opcode == O_FAKE) {
                int branch = program[pointer++];
                if (branch < 0 && branch == checksum) {
                    ops.push(I_NOP);
                }
                continue;
            }
            if (opcode == O_END) {
                ops.push(I_DONE);
                continue;
            }
            throw new IllegalStateException("Invalid outer opcode: " + opcode);
        }
        throw new IllegalStateException("Outer program terminated unexpectedly");
    }

    private static String w(int[] program, int[] data) {
        StringBuilder builder = new StringBuilder(data.length);
        IntStack stack = new IntStack(Math.max(8, data.length + 2));
        for (int pointer = 0; pointer < program.length; ) {
            int opcode = program[pointer++];
            if (opcode == I_DONE) {
                return builder.toString();
            }
            if (opcode == I_PUSH) {
                stack.push(data[program[pointer++]]);
                continue;
            }
            if (opcode == I_DUP) {
                stack.push(stack.peek());
                continue;
            }
            if (opcode == I_SUB) {
                stack.setTop(u(RuntimeDecoder.decodeTape(ShardRepository.subProgramBlob(), ShardRepository.programSeed()), stack.peek()));
                continue;
            }
            if (opcode == I_APPEND) {
                builder.append((char) stack.peek());
                continue;
            }
            if (opcode == I_POP) {
                stack.pop();
                continue;
            }
            if (opcode == I_XOR0) {
                stack.setTop(stack.peek() ^ 0);
                continue;
            }
            if (opcode == I_NOP) {
                continue;
            }
            throw new IllegalStateException("Invalid inner opcode: " + opcode);
        }
        throw new IllegalStateException("Inner program terminated unexpectedly");
    }

    private static int u(int[] program, int input) {
        int acc = 0;
        int reg = input;
        for (int pointer = 0; pointer < program.length; ) {
            int opcode = program[pointer++];
            if (opcode == S_DONE) {
                return reg;
            }
            if (opcode == S_INPUT) {
                reg = input;
                continue;
            }
            if (opcode == S_MASK) {
                acc = MASK;
                continue;
            }
            if (opcode == S_XOR) {
                reg ^= acc;
                continue;
            }
            if (opcode == S_ROL) {
                reg = ((reg << 1) | ((reg >>> 15) & 1)) & 0xFFFF;
                continue;
            }
            if (opcode == S_ROR) {
                reg = ((reg >>> 1) | ((reg & 1) << 15)) & 0xFFFF;
                continue;
            }
            if (opcode == S_ADD0) {
                reg += 0;
                continue;
            }
            if (opcode == S_FAKE) {
                if (reg == acc && reg < 0) {
                    reg ^= acc;
                }
                continue;
            }
            throw new IllegalStateException("Invalid sub opcode: " + opcode);
        }
        throw new IllegalStateException("Sub program terminated unexpectedly");
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(encoded.length * 2);
            for (byte current : encoded) {
                builder.append(String.format(Locale.ROOT, "%02x", current & 0xFF));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("缺少 SHA-256 算法", exception);
        }
    }

    private static final class ObjStack {
        private final Object[] values;
        private int size;

        private ObjStack(int capacity) {
            this.values = new Object[Math.max(8, capacity)];
        }

        private void push(Object value) {
            this.values[this.size++] = value;
        }

        private Object pop() {
            Object value = this.values[--this.size];
            this.values[this.size] = null;
            return value;
        }

        private Object peek() {
            return this.values[this.size - 1];
        }
    }

    private static final class IntTape {
        private final int[] values;
        private int size;

        private IntTape(int capacity) {
            this.values = new int[Math.max(8, capacity)];
        }

        private void push(int value) {
            this.values[this.size++] = value;
        }

        private void pushAll(int[] chunk) {
            System.arraycopy(chunk, 0, this.values, this.size, chunk.length);
            this.size += chunk.length;
        }

        private int size() {
            return this.size;
        }

        private int[] toArray() {
            int[] copy = new int[this.size];
            System.arraycopy(this.values, 0, copy, 0, this.size);
            return copy;
        }
    }

    private static final class IntStack {
        private final int[] values;
        private int size;

        private IntStack(int capacity) {
            this.values = new int[Math.max(8, capacity)];
        }

        private void push(int value) {
            this.values[this.size++] = value;
        }

        private int pop() {
            return this.values[--this.size];
        }

        private int peek() {
            return this.values[this.size - 1];
        }

        private void setTop(int value) {
            this.values[this.size - 1] = value;
        }
    }
}
