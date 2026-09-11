package com.mojang.blaze3d.matrix;

import net.minecraft.client.renderer.Matrix4f;

public class MatrixStack {
    private final Entry entry = new Entry();

    public Entry getLast() {
        return this.entry;
    }

    public static class Entry {
        private final Matrix4f matrix = new Matrix4f();

        public Matrix4f getMatrix() {
            return this.matrix;
        }
    }
}
