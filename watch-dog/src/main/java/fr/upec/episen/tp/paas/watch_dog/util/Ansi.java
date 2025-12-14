package fr.upec.episen.tp.paas.watch_dog.util;

public final class Ansi {

    private Ansi() {}

    public static final String RESET = "\u001B[0m";

    // Normal
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED    = "\u001B[31m";
    public static final String BLUE   = "\u001B[34m";

    // Critique / Infra
    public static final String DARK_RED = "\u001B[1;31m";

    // Helpers simples
    public static String green(String s)     { return GREEN + s + RESET; }
    public static String yellow(String s)    { return YELLOW + s + RESET; }
    public static String red(String s)       { return RED + s + RESET; }
    public static String blue(String s)      { return BLUE + s + RESET; }
    public static String darkred(String s)   { return DARK_RED + s + RESET; }
}