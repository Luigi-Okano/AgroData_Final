package com.agrodata.model;

import com.agrodata.security.CriptografiaConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "USUARIO", uniqueConstraints =
        @UniqueConstraint(name = "uk_usuario_cpf_fingerprint", columnNames = "cpf_fingerprint"))
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    /** true = senha provisória: o usuário só consegue trocar a senha até definir uma nova. */
    @Column(name = "senha_temporaria", nullable = false)
    private boolean senhaTemporaria = false;

    // Dados pessoais: gravados criptografados (AES-GCM)
    @Convert(converter = CriptografiaConverter.class)
    @Column(name = "nome_completo", length = 255)
    private String nomeCompleto;

    @Convert(converter = CriptografiaConverter.class)
    @Column(name = "cpf", length = 255)
    private String cpf;

    @Column(name = "cpf_fingerprint", length = 64)
    private String cpfFingerprint;

    @Convert(converter = CriptografiaConverter.class)
    @Column(name = "email", length = 255)
    private String email;

    @Convert(converter = CriptografiaConverter.class)
    @Column(name = "telefone", length = 255)
    private String telefone;

    public Usuario() {}

    public Usuario(String username, String senhaHash, Role role) {
        this.username = username;
        this.senhaHash = senhaHash;
        this.role = role;
    }

    public Long getIdUsuario() { return idUsuario; }
    public String getUsername() { return username; }
    public String getSenhaHash() { return senhaHash; }
    public Role getRole() { return role; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public void setRole(Role role) { this.role = role; }

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getCpfFingerprint() { return cpfFingerprint; }
    public void setCpfFingerprint(String cpfFingerprint) { this.cpfFingerprint = cpfFingerprint; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public boolean isSenhaTemporaria() { return senhaTemporaria; }
    public void setSenhaTemporaria(boolean senhaTemporaria) { this.senhaTemporaria = senhaTemporaria; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
