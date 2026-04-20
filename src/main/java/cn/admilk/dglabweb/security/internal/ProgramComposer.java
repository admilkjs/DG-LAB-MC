package cn.admilk.dglabweb.security.internal;

public final class ProgramComposer {
    private ProgramComposer() {
    }

    public static int[] buildOuterProgram(int length, int setCursor, int junkCheck, int copyData, int emitDecodeBlock, int advanceCursor, int noop, int emitDone, int done) {
        int[] program = new int[length * 8 + 3];
        int offset = 0;
        for (int index = 0; index < length; index++) {
            program[offset++] = setCursor;
            program[offset++] = index;
            program[offset++] = junkCheck;
            program[offset++] = index ^ 0x15;
            program[offset++] = copyData;
            program[offset++] = emitDecodeBlock;
            program[offset++] = advanceCursor;
            program[offset++] = noop;
        }
        program[offset++] = emitDone;
        program[offset++] = noop;
        program[offset] = done;
        return program;
    }

    public static int[] composeInnerDecodeBlock(int pushData, int slot, int dup, int noop, int runSubVm, int xorZero, int appendTop, int pop) {
        return new int[] {
            pushData,
            slot,
            dup,
            noop,
            runSubVm,
            xorZero,
            appendTop,
            pop
        };
    }
}
