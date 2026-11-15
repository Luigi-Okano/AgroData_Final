package com.agrodata.util;

public final class Cpf {
    private Cpf() {}

    public static String somenteDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /** Valida os dígitos verificadores. */
    public static boolean valido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) return false;
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        int r = (soma * 10) % 11;
        return r == 10 ? 0 : r;
    }

    public static String formatar(String cpf) {
        String d = somenteDigitos(cpf);
        return d.length() == 11
                ? d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9)
                : cpf;
    }
}
