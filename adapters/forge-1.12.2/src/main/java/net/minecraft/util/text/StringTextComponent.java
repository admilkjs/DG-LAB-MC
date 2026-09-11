package net.minecraft.util.text;

public class StringTextComponent extends TextComponentString {
    public StringTextComponent(String text) {
        super(text == null ? "" : text);
    }

    public String getString() {
        return this.getUnformattedText();
    }
}
