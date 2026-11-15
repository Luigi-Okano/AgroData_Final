package com.agrodata.util;

/** Mascaramento de dados pessoais para exibição ao próprio usuário (LGPD: minimização). */
public final class Mascara {
    private Mascara() {}

    /** 12345678901 -> ***.456.789-** */
    public static String cpf(String cpf) {
        String d = Cpf.somenteDigitos(cpf);
        if (d.length() != 11) return cpf == null || cpf.isBlank() ? null : "***";
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }

    /** joao@dominio.com -> j***@dominio.com */
    public static String email(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** (66) 99123-4567 -> (**) *****-4567 */
    public static String telefone(String tel) {
        String d = Cpf.somenteDigitos(tel);
        if (d.length() < 8) return tel == null || tel.isBlank() ? null : "***";
        return "(**) *****-" + d.substring(d.length() - 4);
    }

    /** Carlos Eduardo Lima -> Carlos E*** L*** */
    public static String nome(String nome) {
        if (nome == null || nome.isBlank()) return null;
        String[] partes = nome.trim().split("\\s+");
        StringBuilder sb = new StringBuilder(partes[0]);
        for (int i = 1; i < partes.length; i++) sb.append(' ').append(partes[i].charAt(0)).append("***");
        return sb.toString();
    }
}
