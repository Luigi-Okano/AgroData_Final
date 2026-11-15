package com.agrodata.service;

import com.agrodata.model.Fazenda;
import com.agrodata.model.Talhao;
import com.agrodata.model.Usuario;
import com.agrodata.repository.FazendaRepository;
import com.agrodata.repository.ProdutorRepository;
import com.agrodata.repository.TalhaoRepository;
import com.agrodata.repository.UsuarioRepository;
import com.agrodata.util.Cpf;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProdutorAccessService {

    private final UsuarioRepository usuarios;
    private final ProdutorRepository produtores;
    private final FazendaRepository fazendas;
    private final TalhaoRepository talhoes;

    public ProdutorAccessService(UsuarioRepository usuarios, ProdutorRepository produtores,
                                 FazendaRepository fazendas, TalhaoRepository talhoes) {
        this.usuarios = usuarios;
        this.produtores = produtores;
        this.fazendas = fazendas;
        this.talhoes = talhoes;
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public List<Fazenda> fazendasAcessiveis(String username, boolean admin) {
        if (admin) return fazendas.findAll();
        Long idProdutor = idProdutor(username);
        return idProdutor == null ? List.of() : fazendas.findByIdProdutor(idProdutor);
    }

    public List<Long> idsFazendasAcessiveis(String username, boolean admin) {
        return fazendasAcessiveis(username, admin).stream().map(Fazenda::getIdFazenda).toList();
    }

    public List<Talhao> talhoesAcessiveis(String username, boolean admin) {
        if (admin) return talhoes.findAll();
        List<Long> idsFazendas = idsFazendasAcessiveis(username, false);
        return idsFazendas.isEmpty() ? List.of() : talhoes.findByIdFazendaIn(idsFazendas);
    }

    public List<Long> idsTalhoesAcessiveis(String username, boolean admin) {
        return talhoesAcessiveis(username, admin).stream().map(Talhao::getIdTalhao).toList();
    }

    public void exigirAcessoFazenda(Long idFazenda, String username, boolean admin) {
        if (admin) return;
        boolean acessivel = fazendasAcessiveis(username, false).stream()
                .anyMatch(fazenda -> fazenda.getIdFazenda().equals(idFazenda));
        if (!acessivel) throw naoEncontrado();
    }

    public void exigirAcessoTalhao(Long idTalhao, String username, boolean admin) {
        if (admin) return;
        boolean acessivel = talhoesAcessiveis(username, false).stream()
                .anyMatch(talhao -> talhao.getIdTalhao().equals(idTalhao));
        if (!acessivel) throw naoEncontrado();
    }

    public Long idProdutor(String username) {
        Usuario usuario = usuarios.findByUsername(username).orElse(null);
        if (usuario == null) return null;
        String cpf = Cpf.somenteDigitos(usuario.getCpf());
        if (cpf.length() != 11) return null;
        return produtores.findByCpf(cpf).map(produtor -> produtor.getIdProdutor()).orElse(null);
    }

    private static ResponseStatusException naoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado.");
    }
}
